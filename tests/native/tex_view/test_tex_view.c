#include "hw/xbox/hle/pb/tex_view.h"
#include <assert.h>
#include <stdio.h>

int main(void)
{
    const HleTexView movie = {2560, 640, 480, 32};
    const HleTexView half = {1280, 320, 240, 32};
    /* Warrior Within binds a half-resolution postprocess target after its
     * full-size buffer at the same address. A movie samples full-size rows;
     * target bind recency must not change their pitch or clamp at row 240. */
    assert(hle_tex_view_prefer(movie, movie, 1, half, 2));
    assert(!hle_tex_view_prefer(movie, half, 2, movie, 1));
    assert(hle_tex_view_prefer(half, half, 1, movie, 2));
    assert(hle_tex_view_prefer(movie, movie, 3, movie, 2));
    assert(!hle_tex_view_prefer(movie, movie, 1, movie, 2));
    assert(!hle_tex_view_fits(movie, (HleTexView){2560, 640, 240, 32}));
    assert(!hle_tex_view_fits(movie, (HleTexView){2560, 640, 480, 16}));
    assert(!hle_tex_view_fits(movie, (HleTexView){0, 640, 480, 32}));
    /* A subrectangle can use a larger target with the same row layout. */
    assert(hle_tex_view_fits((HleTexView){2560, 160, 120, 32}, movie));
    /* Sands of Time's small bloom views exclude pitch-padding columns.
     * Both width and height constrain border filtering independently. */
    assert(hle_tex_view_needs_crop((HleTexView){192, 40, 30, 32}, (HleTexView){192, 48, 30, 32}));
    assert(hle_tex_view_needs_crop((HleTexView){128, 20, 15, 32}, (HleTexView){128, 32, 15, 32}));
    assert(hle_tex_view_needs_crop((HleTexView){64, 10, 7, 32}, (HleTexView){64, 16, 7, 32}));
    assert(hle_tex_view_needs_crop((HleTexView){2560, 640, 240, 32}, movie));
    assert(!hle_tex_view_needs_crop(movie, movie));
    assert(!hle_tex_view_needs_crop(movie, half));
    assert(!hle_tex_view_needs_crop((HleTexView){1280, 40, 30, 32}, movie));
    assert(!hle_tex_view_needs_crop((HleTexView){2560, 40, 30, 16}, movie));
    assert(!hle_tex_view_needs_crop((HleTexView){2560, 0, 30, 32}, movie));
    /* Swizzled views (no linear pitch) and unmatched layouts retain the
     * existing most-recently-bound selection. */
    const HleTexView swizzled = {0, 256, 256, 32};
    assert(hle_tex_view_prefer(swizzled, movie, 3, half, 2));
    assert(!hle_tex_view_prefer(swizzled, movie, 1, half, 2));
    puts("Texture view selection: full/half aliases, pitch, size, bpp, recency, subrect and swizzle passed");
    return 0;
}
