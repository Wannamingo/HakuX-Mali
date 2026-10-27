#include "hw/xbox/hle/vk/vis_result.h"
#include <assert.h>
#include <stdio.h>

int main(void)
{
    /* Unsupported queries on MaliSX2 used to look identical to a completed
     * zero-sample query, hiding geometry. Partial/missing results must not
     * establish occlusion even when the available parts all counted zero. */
    assert(hle_vis_result(0, 4, false) == HLE_VIS_FALLBACK_PIXELS);
    assert(hle_vis_result(64, 4, false) == HLE_VIS_FALLBACK_PIXELS);
    assert(hle_vis_result(0, 4, true) == 0);
    assert(hle_vis_result(1024, 4, true) == 256);
    assert(hle_vis_result(1024, 16, true) == 64);
    assert(hle_vis_result(37, 0, true) == 37);
    assert(hle_vis_result(UINT64_MAX, 4, true) == UINT32_MAX);
    puts("Visibility: unavailable/partial results stay visible; valid counts and zero preserved");
    return 0;
}
