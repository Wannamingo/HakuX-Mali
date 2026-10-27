#include "hw/xbox/hle/vk/rop_pass.h"
#include <assert.h>
#include <stdio.h>

int main(void)
{
    const uint32_t rgb = 0x00010101u, rgba = 0x01010101u;
    HleRopPass pass = {0};
    assert(!hle_rop_pass_split(&pass, 0, rgb));
    hle_rop_pass_record(&pass, 0, rgb); /* opaque world */
    for (unsigned i = 0; i < 100; ++i) {
        assert(!hle_rop_pass_split(&pass, 0, rgb)); /* no split per mesh */
        hle_rop_pass_record(&pass, 0, rgb);
    }
    assert(hle_rop_pass_split(&pass, 1, rgb)); /* opaque -> sky/dust blending */
    hle_rop_pass_record(&pass, 1, rgb);
    assert(!hle_rop_pass_split(&pass, 1, rgb)); /* more blended layers */
    assert(hle_rop_pass_split(&pass, 1, 0)); /* alpha-tested depth-only pass */
    hle_rop_pass_record(&pass, 1, 0);
    assert(!hle_rop_pass_split(&pass, 1, 0));
    assert(hle_rop_pass_split(&pass, 1, rgb)); /* colour restored over prepass */
    hle_rop_pass_record(&pass, 1, rgb);
    assert(hle_rop_pass_split(&pass, 1, rgba)); /* alpha-only mask change too */
    assert(hle_rop_pass_split(&pass, 0, rgb)); /* blending disabled */
    hle_rop_pass_reset(&pass); /* attachment change, copy, clear or submission */
    assert(!hle_rop_pass_split(&pass, 1, rgba));
    hle_rop_pass_record(&pass, 1, rgba);
    assert(!hle_rop_pass_split(&pass, 1, rgba));
    puts("ROP pass policy: opaque runs, transparent layers, masked prepass, alpha mask and pass reset passed");
    return 0;
}
