#include "native/cpu/rep_fault.h"
#include <assert.h>
#include <stdio.h>

static const uint64_t base = UINT64_C(0x7000000000);
static const uint8_t movsd[] = {0xf3, 0xa5};

static void check(unsigned unit, bool reverse, bool store, bool vector)
{
    const uint32_t initial_src = reverse ? 0x755ffc : 0x70002c;
    const uint32_t initial_dst = reverse ? 0x7c5fd0 : 0x770000;
    const uint32_t completed = 0x1ffc0 / unit;
    const int direction = reverse ? -1 : 1;
    const unsigned bytes = vector ? 32 : unit;
    const int adjustment = reverse && vector ? 32 - (int)unit : 0;
    const uint32_t current_src = initial_src + direction * completed * unit;
    const uint32_t current_dst = initial_dst + direction * completed * unit;
    uint64_t host[31] = {0};
    host[1] = base + current_dst - adjustment;
    host[2] = base + current_src - adjustment + (store ? direction * (int)bytes : 0);
    const unsigned rn = store ? 1 : 2;
    uint32_t insn;
    if (vector) {
        insn = 0xac800400u | (store ? 0 : 1u << 22) | (rn << 5);
        insn |= ((uint32_t)(direction * 2) & 127) << 15;
    } else {
        const unsigned size = unit == 1 ? 0 : unit == 2 ? 1 : 2;
        insn = 0x38000403u | (size << 30) | (store ? 0 : 1u << 22) | (rn << 5);
        insn |= ((uint32_t)(direction * (int)bytes) & 511) << 12;
    }
    const uint8_t code[] = {0xf3, 0x66, unit == 1 ? 0xa4 : 0xa5};
    uint32_t src = initial_src, dst = initial_dst, count = 0x57000 / unit;
    assert(nc_rep_movs_progress(unit == 4 ? movsd : code, unit == 4 ? 2 : 3,
                                insn, base, host, reverse ? 0x400 : 0, &src, &dst, &count));
    assert(src == current_src && dst == current_dst && count == 0x57000 / unit - completed);
}

int main(void)
{
    for (unsigned unit = 1; unit <= 4; unit *= 2)
        for (unsigned reverse = 0; reverse < 2; ++reverse)
            for (unsigned store = 0; store < 2; ++store)
                for (unsigned vector = 0; vector < 2; ++vector)
                    check(unit, reverse != 0, store != 0, vector != 0);

    /* A vector load straddles 0x720000: restart at the start of that chunk,
     * including its 20 bytes on the preceding page, rather than at si_addr. */
    uint64_t host[31] = {0};
    host[1] = base + 0x78ffc0;
    host[2] = base + 0x71ffec;
    uint32_t src = 0x70002c, dst = 0x770000, count = 0x15c00;
    assert(nc_rep_movs_progress(movsd, 2, 0xacc10440, base, host, 0, &src, &dst, &count));
    assert(src == 0x71ffec && dst == 0x78ffc0 && count == 0x15c00 - 0x1ffc0 / 4);

    const uint32_t saved_src = src, saved_dst = dst, saved_count = count;
    assert(!nc_rep_movs_progress(movsd, 2, 0xacc10440, base, host, 0, &src, &dst, &count)); /* no progress */
    assert(!nc_rep_movs_progress(movsd, 1, 0xacc10440, base, host, 0, &src, &dst, &count)); /* truncated code */
    assert(!nc_rep_movs_progress(movsd, 2, 0xacc10460, base, host, 0, &src, &dst, &count)); /* other base */
    const uint8_t segment[] = {0x64, 0xf3, 0xa5};
    assert(!nc_rep_movs_progress(segment, 3, 0xacc10440, base, host, 0, &src, &dst, &count));
    host[1] += 4;
    assert(!nc_rep_movs_progress(movsd, 2, 0xacc10440, base, host, 0, &src, &dst, &count)); /* inconsistent pointers */
    assert(src == saved_src && dst == saved_dst && count == saved_count);
    puts("REP MOVS restart: 24 scalar/vector, read/write, forward/reverse cases passed");
    return 0;
}
