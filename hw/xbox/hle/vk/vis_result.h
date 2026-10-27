/* An unavailable occlusion result cannot prove that a surface is hidden. */
#ifndef HLE_VIS_RESULT_H
#define HLE_VIS_RESULT_H
#include <stdbool.h>
#include <stdint.h>

#define HLE_VIS_FALLBACK_PIXELS (640u * 480u)

static inline uint32_t hle_vis_result(uint64_t samples, uint32_t divisor, bool complete)
{
    if (!complete) {
        return HLE_VIS_FALLBACK_PIXELS;
    }
    const uint64_t pixels = samples / (divisor ? divisor : 1);
    return pixels > UINT32_MAX ? UINT32_MAX : (uint32_t)pixels;
}
#endif
