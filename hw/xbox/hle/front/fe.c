/*
 * hakuX HLE GPU — front-end command producer.
 */
#include "hw/xbox/hle/base.h"
#include "hw/xbox/hle/hitch.h"
#include "hw/xbox/hle/front/fe.h"
#include "hw/xbox/hle/vk/back.h"

#define HLE_RING_BYTES (16u << 20)

static HleRing g_ring;
static uint8_t *g_ring_mem;
static uint64_t g_value;

int hle_fe_init(void)
{
    if (g_ring_mem) {
        return 0;
    }
    g_ring_mem = hle_memalign(64, HLE_RING_BYTES);
    return hle_ring_init(&g_ring, g_ring_mem, HLE_RING_BYTES);
}

HleRing *hle_fe_ring(void)
{
    return &g_ring;
}

void *hle_fe_begin_raw(uint16_t op, uint32_t len)
{
    /* Pace the producer before publishing another frame, rather than let
     * the guest queue frames ahead of a throttled GPU/presenter (input lag).
     * All native presentation paths emit SWAP here, including IMMEDIATE. */
    if (op == HLE_OP_SWAP) {
        static bool checked;
        static int64_t interval, last;
        if (!checked) {
            const char *e = getenv("HAKUX_HLE_FPS_LIMIT");
            const int fps = e ? atoi(e) : 0;
            interval = fps > 0 && fps <= 240 ? 1000000000ll / fps : 0;
            checked = true;
        }
        if (interval) {
            int64_t now = hle_now_ns();
            const int64_t due = last + interval;
            if (last && now < due) {
                hle_fe_commit(); /* existing draws can run while their producer waits */
                do {
                    g_usleep((gulong)((due - now + 999) / 1000));
                    now = hle_now_ns();
                } while (now < due);
            }
            last = now; /* no burst of catch-up frames after a stall or pause */
        }
    }
    void *p = hle_ring_reserve(&g_ring, op, len);
    if (likely(p)) {
        return p;
    }
    uint32_t pa = hle_hitch_enter(HLE_ACT_RING);     /* full: the render thread is behind */
    while (!(p = hle_ring_reserve(&g_ring, op, len))) {
        hle_ring_publish(&g_ring);
        hle_back_kick();
        g_usleep(100);
    }
    hle_hitch_leave(pa);
    return p;
}

void *hle_fe_begin(uint16_t op, uint32_t len)
{
    void *p = hle_fe_begin_raw(op, len);
    memset(p, 0, len);
    return p;
}

void hle_fe_commit(void)
{
    hle_ring_publish(&g_ring);
    hle_back_kick();
}

unsigned hle_fe_state(const HleCmdState *st, HleCmdState *last, bool *sent)
{
    if (!*sent) {
        HleCmdState *c = hle_fe_begin_raw(HLE_OP_STATE, sizeof(*c));
        *c = *st;
        *last = *st;
        *sent = true;
        return 0;
    }
    enum { CH = 64, NCH = (sizeof(*st) + CH - 1) / CH };
    _Static_assert(sizeof(*st) % 8 == 0 && NCH <= 32, "HleCmdState: 8-byte words, 32 chunks");
    const uint8_t *a = (const uint8_t *)st;
    uint8_t *b = (uint8_t *)last;
    /* the changed chunks, a bit each, from the 8-byte words: a memcmp call
     * per chunk was ~4 % of Blinx's decoder (a state built on ~40 % of its
     * draws) */
    uint32_t diff = 0;
    for (unsigned i = 0; i < NCH; i++) {
        uint32_t o = i * CH, n = MIN((uint32_t)CH, (uint32_t)sizeof(*st) - o);
        uint64_t d = 0;
        for (uint32_t k = o; k < o + n; k += 8) {
            uint64_t x, y;
            memcpy(&x, a + k, 8);
            memcpy(&y, b + k, 8);
            d |= x ^ y;
        }
        diff |= (uint32_t)(d != 0) << i;
    }
    unsigned runs = 0;
    while (diff) {
        unsigned i = hle_ctz32(diff), j = i + 1;    /* the run of changed chunks */
        while (j < NCH && (diff >> j & 1)) {
            j++;
        }
        diff &= j < 32 ? ~0u << j : 0;
        uint32_t o = i * CH, len = MIN(j * CH, (uint32_t)sizeof(*st)) - o;
        HleCmdStatePart *c = hle_fe_begin_raw(HLE_OP_STATE_PART, sizeof(*c) + len);
        c->off = o;
        c->len = len;
        memcpy(c + 1, a + o, len);
        memcpy(b + o, a + o, len);
        runs++;
    }
    return runs;
}

uint64_t hle_fe_next_value(void)
{
    return ++g_value;
}

uint64_t hle_fe_last_value(void)
{
    return g_value;
}
