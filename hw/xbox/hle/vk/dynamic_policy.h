#ifndef HLE_DYNAMIC_POLICY_H
#define HLE_DYNAMIC_POLICY_H
#include <stdbool.h>
#include <stdint.h>
#include <string.h>
/* Queue occupancy is a host-side proxy, not GPU time. A downscale is a
 * probe: keep it only if observed FPS responds. Time is monotonic seconds. */
typedef struct HleDynamicPolicy {
    double ready,blocked,baseline,verify;
    unsigned low,high,restore;
} HleDynamicPolicy;
static inline void hle_dynamic_reset(HleDynamicPolicy *p,double now)
{ memset(p,0,sizeof(*p));p->ready=now+10; }
static inline unsigned hle_dynamic_window(HleDynamicPolicy *p,double now,double fps,double queue,
    bool eligible,unsigned current,unsigned min,unsigned max,unsigned goal)
{
    if(!eligible||fps<5) {p->low=p->high=0;p->ready=now+3;return current;}
    if(now<p->ready)return current;
    if(p->restore) {
        if(now<p->verify)return current;
        unsigned old=p->restore;p->restore=0;p->ready=now+8;
        if(fps<p->baseline*1.05) {p->blocked=now+30;return old;}
        return current;
    }
    if(now<p->blocked)return current;
    /* Fence queue occupancy includes gaps caused by CPU/GPU dependencies.
     * A moderate pending fraction can still be GPU-sensitive (Syberia on
     * G615: ~55%). Treat it only as permission to probe; FPS response below
     * decides whether the smaller scale is useful. */
    if(current>min && fps<goal*.92 && queue>.25) {
        p->high=0;
        if(++p->low>=3) {
            p->low=0;p->baseline=fps;p->restore=current;p->verify=now+4;p->ready=now+1;
            return current-1;
        }
    }else p->low=0;
    double ratio=(double)(current+1)/current;
    double projected=queue*goal/fps*ratio*ratio;
    if(current<max && fps>=goal*.98 && projected<.70) {
        if(++p->high>=5) {p->high=0;p->ready=now+10;return current+1;}
    }else p->high=0;
    return current;
}
#endif
