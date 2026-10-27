#include "hw/xbox/hle/pb/vertex_repack.h"
#include <assert.h>
#include <stdio.h>

int main(void)
{
    /* The Two Thrones: position, normal, three short bone indices, three
     * float weights and UV, in a 50-byte interleaved stream. Both odd
     * vertices and the weights/UV offsets violate float alignment. */
    uint32_t attr[16] = {0};
    attr[0] = 0x32;
    attr[1] = (30u << 16) | 0x32;
    attr[2] = (12u << 16) | 0x32;
    attr[7] = (24u << 16) | 0x35;
    attr[9] = (42u << 16) | 0x22;
    HleVertexPack p;
    assert(hle_vertex_pack_plan(attr, 0, 50, 0, &p));
    assert(p.stride == 52 && p.mask == ((1u<<0)|(1u<<1)|(1u<<2)|(1u<<7)|(1u<<9)));
    uint8_t source[150], result[156];
    for (unsigned i = 0; i < sizeof(source); ++i) source[i] = (uint8_t)(i * 17 + 5);
    memset(result, 0xCC, sizeof(result));
    assert(hle_vertex_pack_copy(&p, source, sizeof(source), 50, result, sizeof(result), 3));
    for (unsigned v = 0; v < 3; ++v) {
        for (unsigned a = 0; a < 16; ++a) {
            if (!((p.mask >> a) & 1)) continue;
            assert((v * p.stride + p.dst[a]) % 4 == 0);
            assert(memcmp(source + v * 50 + p.src[a], result + v * p.stride + p.dst[a], p.bytes[a]) == 0);
        }
        uint16_t w;
        memcpy(&w, result + v * p.stride + p.dst[7] + 6, 2);
        assert(w == 1); /* default W if a 3-short format needs a 4-short fallback */
    }
    uint8_t untouched[156];
    memset(untouched, 0x5A, sizeof(untouched));
    assert(!hle_vertex_pack_copy(&p, source, 149, 50, untouched, sizeof(untouched), 3));
    assert(!hle_vertex_pack_copy(&p, source, sizeof(source), 50, untouched, 155, 3));
    for (unsigned i = 0; i < sizeof(untouched); ++i) assert(untouched[i] == 0x5A);
    assert(hle_vertex_pack_plan(attr, 0, 0, 0, &p));
    assert(hle_vertex_pack_copy(&p, source, 50, 0, result, 52, 1)); /* constant binding */
    uint32_t aligned[16] = {0x32, (12u<<16)|0x22, (20u<<16)|0x40};
    assert(!hle_vertex_pack_plan(aligned, 0, 24, 0, &p));
    assert(hle_vertex_pack_plan(aligned, 0, 24, 2, &p)); /* unaligned in-place base */
    aligned[1] = (12u<<16) | (1u<<8) | 0x22;
    assert(hle_vertex_pack_plan(aligned, 1, 22, 0, &p));
    assert(p.mask == 2); /* another binding is not included */
    puts("Vertex repack: 50-byte skinned mesh, exact data preservation, alignment, bounds and constant/other bindings passed");
    return 0;
}
