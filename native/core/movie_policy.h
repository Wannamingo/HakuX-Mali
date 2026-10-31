#ifndef NATIVE_CORE_MOVIE_POLICY_H
#define NATIVE_CORE_MOVIE_POLICY_H
#include <stdbool.h>
#include <stdint.h>
#include <string.h>

/* Only known movie locations. Menus and loading animations are explicitly
 * excluded even when they use Bink. Unknown names preserve the game layout. */
static inline bool nc_movie_path(const char *path)
{
    char p[512]; size_t n=0;
    for (; path[n] && n<sizeof(p)-1; n++) {
        unsigned char c=(unsigned char)path[n];
        p[n]=c=='/'?'\\':c>='A'&&c<='Z'?(char)(c+32):(char)c;
    }
    p[n]=0;
    if (n<4 || strcmp(p+n-4,".bik") || strstr(p,"menu") || strstr(p,"interface") ||
        strstr(p,"\\load") || strstr(p,"loading")) return false;
    return strstr(p,"\\video\\") || strstr(p,"\\movies\\") || strstr(p,"\\cine\\") ||
           strstr(p,"\\cinematics\\");
}

static inline uint32_t nc_movie_le32(const uint8_t *p)
{ return p[0]|(uint32_t)p[1]<<8|(uint32_t)p[2]<<16|(uint32_t)p[3]<<24; }

/* Bink1 container header: frame count +8, width/height +20/+24, rate +28/+32.
 * Layout reference: FFmpeg libavformat/bink.c (container metadata only). */
static inline bool nc_movie_header(const uint8_t data[36], uint32_t *w, uint32_t *h)
{
    const uint32_t frames=nc_movie_le32(data+8), width=nc_movie_le32(data+20),
        height=nc_movie_le32(data+24), num=nc_movie_le32(data+28), den=nc_movie_le32(data+32);
    if (data[0]!='B'||data[1]!='I'||data[2]!='K'||!data[3]||!strchr("bfghik",data[3]) ||
        frames<2||frames>1000000||width<160||width>4096||height<100||height>2160||
        !num||!den||(double)num/den<5||(double)num/den>120) return false;
    *w=width;*h=height;return true;
}

typedef struct NcMovieVote { unsigned yes,no;uint32_t w,h,kind;bool active; } NcMovieVote;
/* Frame hysteresis prevents a single upload/overlay from changing layout. */
static inline uint64_t nc_movie_vote(NcMovieVote *v,uint32_t w,uint32_t h,uint32_t kind)
{
    if(w>8192||h>8192)w=h=kind=0;
    if(w&&h&&kind) {
        v->no=0;
        if(v->w!=w||v->h!=h||v->kind!=kind) {v->yes=0;v->active=false;v->w=w;v->h=h;v->kind=kind;}
        if(v->yes<3)v->yes++;
        if(v->yes>=3)v->active=true;
    } else {
        v->yes=0;
        if(++v->no>=3) {v->w=v->h=v->kind=0;v->no=3;v->active=false;}
    }
    return v->active ? v->w|((uint64_t)v->h<<16)|((uint64_t)v->kind<<32):0;
}
#endif
