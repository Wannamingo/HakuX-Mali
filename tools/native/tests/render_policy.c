#include "hw/xbox/hle/vk/filter_policy.h"
#include <assert.h>
#include <math.h>
#include <stdio.h>

int main(void)
{
    const HleFilterFacts normal={.xfmt=HLE_XFMT_DXT1,.levels=8,.faces=1,
        .min_filter=2,.mag_filter=2,.mip_filter=2,.depth_test=true};
    assert(hle_filter_override(8,16,true,&normal)==8);
    assert(hle_filter_override(8,4,true,&normal)==4);
    assert(hle_filter_override(0,16,true,&normal)==0);
    assert(hle_filter_override(3,16,true,&normal)==0);
    assert(hle_filter_override(8,16,false,&normal)==0);
    assert(hle_filter_override(8,NAN,true,&normal)==0);
    assert(hle_filter_override(8,1,true,&normal)==0);
    HleFilterFacts f=normal;
    f.xfmt=HLE_XFMT_P8; assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.xfmt=HLE_XFMT_YUY2;assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.pretransformed=true;assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.depth_test=false;assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.linear=true;assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.volume=true;assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.faces=6;assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.levels=1;assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.min_filter=1;assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.mag_filter=1;assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.mip_filter=0;assert(hle_filter_override(8,16,true,&f)==0);
    f=normal;f.point_sprite=true;assert(hle_filter_override(8,16,true,&f)==0);
    const uint32_t bias=0xbf000000;
    uint64_t original=hle_filter_key2(bias,32,0), enhanced=hle_filter_key2(bias,32,8);
    assert(original!=enhanced && (uint32_t)enhanced==bias && (uint16_t)(enhanced>>32)==32);
    assert(original==(bias|((uint64_t)32<<32)));
    puts("PASS: 22 renderer filter policy cases");
    return 0;
}
