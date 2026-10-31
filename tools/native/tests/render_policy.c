#include "hw/xbox/hle/vk/filter_policy.h"
#include "native/core/movie.h"
#include "native/core/movie_policy.h"
#include "hw/xbox/hle/vk/dynamic_policy.h"
#include <assert.h>
#include <math.h>
#include <stdio.h>

int main(void)
{
    const HleFilterFacts normal={.xfmt=HLE_XFMT_DXT1,.levels=8,.faces=1,
        .min_filter=2,.mag_filter=2,.mip_filter=2,.depth_test=true};
    assert(hle_filter_override(8,16,true,&normal)==8);
    assert(hle_filter_override(16,16,true,&normal)==16);
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
    assert(nc_movie_path("\\Device\\CdRom0\\video\\30fps\\CINE\\CINE0100.BIK"));
    assert(!nc_movie_path("\\Device\\CdRom0\\video\\30fps\\LoadIn0.bik"));
    assert(!nc_movie_path("\\Device\\CdRom0\\interface\\menu.bik"));
    assert(!nc_movie_path("\\Device\\CdRom0\\video\\mainmenu.bik"));
    assert(!nc_movie_path("\\Device\\CdRom0\\textures\\video.dds"));
    uint8_t header[36]={ 'B','I','K','i' };uint32_t w=0,h=0;
    uint32_t words[9]={0,0,300,0,0,640,480,30,1};memcpy(header+4,(uint8_t*)words+4,32);
    assert(nc_movie_header(header,&w,&h)&&w==640&&h==480);
    header[3]=0;assert(!nc_movie_header(header,&w,&h));header[3]='i';
    header[32]=0;assert(!nc_movie_header(header,&w,&h));header[32]=1;
    NcMovieVote vote={0};
    assert(!nc_movie_vote(&vote,640,480,2));assert(!nc_movie_vote(&vote,640,480,2));
    assert(nc_movie_vote(&vote,640,480,2));assert(nc_movie_vote(&vote,0,0,0));
    assert(nc_movie_vote(&vote,640,480,2));
    assert(nc_movie_vote(&vote,0,0,0));assert(nc_movie_vote(&vote,0,0,0));assert(!nc_movie_vote(&vote,0,0,0));
    assert(!nc_movie_vote(&vote,UINT32_MAX,480,2));
    const char *path="\\Device\\CdRom0\\video\\intro.bik";
    assert(!nc_movie_begin((void*)1,path));nc_movie_enable(true);
    uint64_t token=nc_movie_begin((void*)1,path);assert(token);
    nc_movie_read(token,0,header,4);assert(!nc_movie_source(&w,&h));
    nc_movie_read(token,4,header+4,32);assert(nc_movie_source(&w,&h)&&w==640&&h==480);
    nc_movie_close((void*)1);assert(!nc_movie_source(&w,&h));
    nc_movie_read(token,0,header,36);assert(!nc_movie_source(&w,&h));
    assert(!nc_movie_begin((void*)2,"\\video\\menu.bik"));
    token=nc_movie_begin((void*)3,path);nc_movie_read(token,0,header,36);
    nc_movie_enable(false);assert(!nc_movie_source(&w,&h));
    HleDynamicPolicy drs;hle_dynamic_reset(&drs,0);
    assert(hle_dynamic_window(&drs,5,40,.99,true,2,1,2,60)==2); /* warmup */
    assert(hle_dynamic_window(&drs,10,40,.99,true,2,1,2,60)==2);
    assert(hle_dynamic_window(&drs,11,40,.99,true,2,1,2,60)==2);
    assert(hle_dynamic_window(&drs,12,40,.99,true,2,1,2,60)==1);
    assert(hle_dynamic_window(&drs,16,59,.6,true,1,1,2,60)==1); /* keep useful reduction */
    assert(!drs.restore);
    hle_dynamic_reset(&drs,0);
    for(unsigned i=10;i<12;i++)assert(hle_dynamic_window(&drs,i,30,.99,true,2,1,2,60)==2);
    assert(hle_dynamic_window(&drs,12,30,.99,true,2,1,2,60)==1);
    assert(hle_dynamic_window(&drs,16,30,.4,true,1,1,2,60)==2); /* CPU-bound/fixed game cap */
    assert(hle_dynamic_window(&drs,25,30,.99,true,2,1,2,60)==2); /* retry blocked */
    hle_dynamic_reset(&drs,0);
    for(unsigned i=10;i<20;i++)assert(hle_dynamic_window(&drs,i,30,.3,true,2,1,2,60)==2);
    for(unsigned i=20;i<30;i++)assert(hle_dynamic_window(&drs,i,30,.99,false,2,1,2,60)==2); /* 2D/video */
    hle_dynamic_reset(&drs,0);
    for(unsigned i=10;i<14;i++)assert(hle_dynamic_window(&drs,i,60,.2,true,1,1,2,30)==1);
    assert(hle_dynamic_window(&drs,14,60,.2,true,1,1,2,30)==2);
    hle_dynamic_reset(&drs,0);
    for(unsigned i=10;i<30;i++)assert(hle_dynamic_window(&drs,i,60,.6,true,1,1,2,60)==1); /* no projected headroom */
    puts("PASS: filter, movie metadata/lifetime, DRS warmup, CPU-bound rollback, hysteresis and headroom policies");
    return 0;
}
