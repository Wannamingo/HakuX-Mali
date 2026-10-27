/* Linear texture views must retain their row layout when several render
 * targets alias the same guest address. */
#ifndef HLE_PB_TEX_VIEW_H
#define HLE_PB_TEX_VIEW_H
#include <stdbool.h>
#include <stdint.h>

typedef struct HleTexView {
    uint32_t pitch, w, h, bpp;
} HleTexView;

static inline bool hle_tex_view_fits(HleTexView texture, HleTexView target)
{
    return texture.pitch && target.pitch == texture.pitch &&
           target.bpp == texture.bpp && target.w >= texture.w && target.h >= texture.h;
}

static inline bool hle_tex_view_prefer(HleTexView texture, HleTexView candidate, uint64_t bound,
                                      HleTexView best, uint64_t best_bound)
{
    const bool fits = hle_tex_view_fits(texture, candidate);
    const bool best_fits = hle_tex_view_fits(texture, best);
    return fits != best_fits ? fits : bound > best_bound;
}

static inline bool hle_tex_view_needs_crop(HleTexView texture, HleTexView target)
{
    return texture.w && texture.h && hle_tex_view_fits(texture, target) &&
           (texture.w != target.w || texture.h != target.h);
}
#endif
