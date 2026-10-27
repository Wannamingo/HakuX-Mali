/* ROP state that must remain uniform within a pass on affected Mali drivers. */
#ifndef HLE_VK_ROP_PASS_H
#define HLE_VK_ROP_PASS_H
#include <stdbool.h>
#include <stdint.h>

typedef struct HleRopPass {
    bool valid;
    uint32_t blend, mask;
} HleRopPass;

static inline bool hle_rop_pass_split(const HleRopPass *p, uint32_t blend, uint32_t mask)
{
    return p->valid && (p->blend != blend || p->mask != mask);
}

static inline void hle_rop_pass_record(HleRopPass *p, uint32_t blend, uint32_t mask)
{
    *p = (HleRopPass){ true, blend, mask };
}

static inline void hle_rop_pass_reset(HleRopPass *p)
{
    p->valid = false;
}
#endif
