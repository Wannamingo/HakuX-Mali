#include "native/core/movie.h"
#include "native/core/movie_policy.h"
#include <pthread.h>

static bool g_enabled;
static pthread_mutex_t g_lock=PTHREAD_MUTEX_INITIALIZER;
static uint64_t g_next,g_read_serial;
static struct { const void *file; uint64_t token,mask,read_serial;uint8_t header[36];uint32_t w,h; } g_stream[16];

void nc_movie_reset(void) { pthread_mutex_lock(&g_lock);memset(g_stream,0,sizeof(g_stream));pthread_mutex_unlock(&g_lock); }
void nc_movie_enable(bool on)
{
    bool old=__atomic_exchange_n(&g_enabled,on,__ATOMIC_ACQ_REL);
    if(old&&!on)nc_movie_reset();
}
uint64_t nc_movie_begin(const void *file,const char *path)
{
    if(!__atomic_load_n(&g_enabled,__ATOMIC_ACQUIRE)||!nc_movie_path(path))return 0;
    uint64_t result=0;pthread_mutex_lock(&g_lock);
    for(unsigned i=0;i<16;i++)if(g_stream[i].file==file){result=g_stream[i].token;break;}
    if(!result)for(unsigned i=0;i<16;i++)if(!g_stream[i].file){
        g_stream[i].file=file;result=g_stream[i].token=++g_next;break;
    }
    pthread_mutex_unlock(&g_lock);return result;
}
void nc_movie_read(uint64_t token,uint64_t offset,const void *data,size_t size)
{
    if(!token||!size)return;
    pthread_mutex_lock(&g_lock);
    for(unsigned i=0;i<16;i++)if(g_stream[i].token==token) {
        g_stream[i].read_serial=++g_read_serial;
        for(size_t k=0;k<size&&offset+k<36;k++){
            g_stream[i].header[offset+k]=((const uint8_t*)data)[k];g_stream[i].mask|=1ull<<(offset+k);
        }
        if(offset<36&&g_stream[i].mask==((1ull<<36)-1)) {
            g_stream[i].w=g_stream[i].h=0;
            nc_movie_header(g_stream[i].header,&g_stream[i].w,&g_stream[i].h);
        }
        break;
    }
    pthread_mutex_unlock(&g_lock);
}
void nc_movie_close(const void *file)
{
    if(!__atomic_load_n(&g_enabled,__ATOMIC_ACQUIRE))return;
    pthread_mutex_lock(&g_lock);
    for(unsigned i=0;i<16;i++)if(g_stream[i].file==file)memset(&g_stream[i],0,sizeof(g_stream[i]));
    pthread_mutex_unlock(&g_lock);
}
bool nc_movie_source(uint32_t *w,uint32_t *h)
{
    if(!__atomic_load_n(&g_enabled,__ATOMIC_ACQUIRE))return false;
    uint64_t latest=0;pthread_mutex_lock(&g_lock);
    for(unsigned i=0;i<16;i++)if(g_stream[i].w&&g_stream[i].h&&g_stream[i].read_serial>latest){
        *w=g_stream[i].w;*h=g_stream[i].h;latest=g_stream[i].read_serial;
    }
    pthread_mutex_unlock(&g_lock);return latest!=0;
}
