#ifndef HW_XBOX_HLE_VK_FILTER_POLICY_H
#define HW_XBOX_HLE_VK_FILTER_POLICY_H
#include "hw/xbox/hle/tex/xdk.h"

typedef struct HleFilterFacts {
    uint32_t xfmt, levels, faces, min_filter, mag_filter, mip_filter;
    bool linear, volume, depth_test, pretransformed, point_sprite;
} HleFilterFacts;

static inline unsigned hle_filter_override(unsigned requested, float limit, bool supported,
                                           const HleFilterFacts *f)
{
    if ((requested != 2 && requested != 4 && requested != 8 && requested != 16) || !supported ||
        !(limit >= 2 && limit <= 1024) || !f->depth_test || f->pretransformed ||
        f->point_sprite || f->linear || f->volume || f->faces != 1 || f->levels <= 1 ||
        f->min_filter < 2 || f->mag_filter < 2 || !f->mip_filter) return 0;
    switch (f->xfmt) {
    case HLE_XFMT_A1R5G5B5: case HLE_XFMT_X1R5G5B5: case HLE_XFMT_A4R4G4B4:
    case HLE_XFMT_R5G6B5: case HLE_XFMT_A8R8G8B8: case HLE_XFMT_X8R8G8B8:
    case HLE_XFMT_DXT1: case HLE_XFMT_DXT3: case HLE_XFMT_DXT5:
    case HLE_XFMT_A8B8G8R8: case HLE_XFMT_B8G8R8A8: case HLE_XFMT_R8G8B8A8:
        break;
    default: return 0;
    }
    return requested < (unsigned)limit ? requested : (unsigned)limit;
}

static inline uint64_t hle_filter_key2(uint32_t bias, uint32_t lod_down, unsigned anisotropy)
{
    return bias | (uint64_t)(lod_down & 0xffffu) << 32 | (uint64_t)anisotropy << 48;
}
#endif
