/* NV2A arrays may have two-byte-aligned floats; ordinary Vulkan vertex
 * input requires each component address to be naturally aligned. */
#ifndef HLE_PB_VERTEX_REPACK_H
#define HLE_PB_VERTEX_REPACK_H
#include "hw/xbox/hle/shader/vattr.h"
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <string.h>

typedef struct HleVertexPack {
    uint32_t mask, stride;
    uint16_t src[16], dst[16], bytes[16];
    uint8_t fmt[16];
} HleVertexPack;

static inline bool hle_vertex_pack_plan(const uint32_t attr[16], unsigned binding,
                                        uint32_t stride, uint32_t base, HleVertexPack *p)
{
    bool needed = false;
    memset(p, 0, sizeof(*p));
    for (unsigned a = 0; a < 16; ++a) {
        const uint8_t fmt = (uint8_t)attr[a];
        const unsigned n = hle_vat_count(fmt), type = hle_vat_type(fmt);
        if (!n || ((attr[a] >> 8) & 255) != binding) continue;
        if (n > 4) return false;
        unsigned component;
        switch (type) {
        case HLE_VAT_UB_D3D: case HLE_VAT_UB_OGL: component = 1; break;
        case HLE_VAT_S1: case HLE_VAT_S32K: case HLE_VAT_CMP: component = 2; break;
        case HLE_VAT_F: component = 4; break;
        default: return false;
        }
        p->mask |= 1u << a;
        p->fmt[a] = fmt;
        p->src[a] = (uint16_t)(attr[a] >> 16);
        p->dst[a] = (uint16_t)((p->stride + 3) & ~3u);
        p->bytes[a] = (uint16_t)(type == HLE_VAT_CMP ? 4 : n * component);
        /* Room for the four-component format fallback of byte/short inputs. */
        p->stride = p->dst[a] + (type != HLE_VAT_CMP && component < 4 ? 4 * component : p->bytes[a]);
        needed |= ((base + p->src[a]) % component) != 0 || (stride % component) != 0;
    }
    p->stride = (p->stride + 3) & ~3u;
    return needed;
}

static inline bool hle_vertex_pack_copy(const HleVertexPack *p, const uint8_t *src,
                                        size_t src_bytes, uint32_t src_stride,
                                        uint8_t *dst, size_t dst_bytes, uint32_t vertices)
{
    if (!vertices || !p->mask || (uint64_t)p->stride * vertices > dst_bytes) return false;
    for (unsigned a = 0; a < 16; ++a) {
        if ((p->mask >> a) & 1) {
            const uint64_t last = (uint64_t)(vertices - 1) * src_stride + p->src[a] + p->bytes[a];
            if (last > src_bytes) return false;
        }
    }
    memset(dst, 0, (size_t)p->stride * vertices);
    for (uint32_t v = 0; v < vertices; ++v) {
        for (unsigned a = 0; a < 16; ++a) {
            if (!((p->mask >> a) & 1)) continue;
            uint8_t *out = dst + (size_t)v * p->stride + p->dst[a];
            const unsigned type = hle_vat_type(p->fmt[a]);
            if (hle_vat_count(p->fmt[a]) < 4) {
                if (type == HLE_VAT_UB_D3D || type == HLE_VAT_UB_OGL) out[3] = 255;
                else if (type == HLE_VAT_S1 || type == HLE_VAT_S32K) {
                    const uint16_t one = type == HLE_VAT_S1 ? 32767 : 1;
                    memcpy(out + 6, &one, 2);
                }
            }
            memcpy(out, src + (size_t)v * src_stride + p->src[a], p->bytes[a]);
        }
    }
    return true;
}
#endif
