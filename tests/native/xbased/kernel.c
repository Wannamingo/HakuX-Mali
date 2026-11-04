/* CPU-only regression harness: production dispatcher, ports and quantum/yield. */
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include "native/kernel/ke.c"

static unsigned checks;
NcSchedStats g_nc_sched;
#define CHECK(x) do { checks++; if (!(x)) { fprintf(stderr,"FAIL %s:%d: %s\n",__FILE__,__LINE__,#x); abort(); } } while(0)
void nc_log(const char *tag,const char *fmt,...) { (void)tag; (void)fmt; }
void nc_kernel_request_stop(NcKernel *k,NcFrame *f,const char *fmt,...) { (void)k;(void)f;(void)fmt; abort(); }
void nc_kernel_clock_arm(NcKernel *k) { (void)k; }
void nc_context_set_gpr(NcCpuContext *c,int reg,uint32_t v) { c->blob[reg]=v; }
void nc_buf_put(NcBuf *b,const void *v,size_t n) {
    if(b->len+n>b->cap) { b->cap=(b->len+n)*2; b->p=realloc(b->p,b->cap); assert(b->p); }
    memcpy(b->p+b->len,v,n);b->len+=n;
}
bool nc_rd_get(NcRd *r,void *p,size_t n) {
    if(n>r->len-r->pos) {r->fail=true;memset(p,0,n);return false;}
    memcpy(p,r->p+r->pos,n);r->pos+=n;return true;
}
bool nc_apc_pending(const NcThread *t) { (void)t;return false; }
bool nc_apc_deliver(NcKernel *k,NcFrame *f) { (void)k;(void)f;return false; }
uint32_t nc_ob_lookup(NcKernel *k,uint32_t h,enum NcObType *ty) {
    if((h&3)||h/4>=NC_MAX_HANDLES||!k->handle_table[h/4].object)return 0;
    if(ty)*ty=k->handle_table[h/4].type;return k->handle_table[h/4].object;
}
extern unsigned test_sharing(NcKernel *k);
int main(void) {
    NcKernel *k=calloc(1,sizeof(*k));NcVm vm={0};vm.base=calloc(1,1<<20);k->vm=&vm;
    NcThread *a=&k->threads[0],*b=&k->threads[1];k->current=a;k->threads_hi=2;
    a->priority=b->priority=8;a->state=NC_T_RUNNING;b->state=NC_T_READY;
    boost(b,4);CHECK(nc_thread_prio(b)==12);boost(b,1);CHECK(b->boost==4);
    boost(b,100);CHECK(nc_thread_prio(b)==15);
    b->boost=0;b->no_boost=true;boost(b,2);CHECK(b->boost==0);b->no_boost=false;
    b->priority=20;boost(b,4);CHECK(nc_thread_prio(b)==20);b->priority=8;
    a->boost=2;nc_ps_yield(k);CHECK(a->boost==1);CHECK(a->slice_expired);
    a->slice_expired=false;k->slice_thread=a;nc_ps_slice_end(k,nc_ke_now_ns());
    CHECK(a->boost==0);CHECK(a->slice_expired);
    const uint32_t event=0x100;init_header(k,event,OBJ_SYNCHRONIZATION_EVENT,4,0);
    b->state=NC_T_WAITING;b->wait=(NcWait){.objects={event},.count=1};k->resched=false;
    set_event(k,event,3);CHECK(b->state==NC_T_READY);CHECK(nc_thread_prio(b)==11);CHECK(k->resched);
    CHECK(signal_state(k,event)==0);
    /* More than the upstream fixed 512-packet limit; preserve FIFO across a state round-trip. */
    nc_ke_ports_reset();Port *p=calloc(1,sizeof(*p));p->obj=0x200;g_ports[g_nports++]=p;
    init_header(k,p->obj,OBJ_QUEUE,10,0);a->state=b->state=NC_T_READY;
    for(unsigned i=0;i<700;i++)nc_ke_port_queue(k,p->obj,i,1000+i,0,2000+i);
    CHECK(p->n==700);CHECK(signal_state(k,p->obj)==700);
    g_port_out[0][0]=0x300;g_port_out[0][1]=0x304;g_port_out[0][2]=0x308;
    NcBuf saved={0};CHECK(nc_ke_ports_save(&saved));
    NcRd r={.p=saved.p,.len=saved.len};CHECK(nc_ke_ports_load(&r));CHECK(r.pos==r.len);
    p=g_ports[0];CHECK(p->n==700);CHECK(g_port_out[0][0]==0x300);
    for(unsigned i=0;i<700;i++) {
        g_port_out[0][0]=0x300;g_port_out[0][1]=0x304;g_port_out[0][2]=0x308;
        port_take(k,a,p->obj);CHECK(nc_vm_rd32(&vm,0x300)==i);
        CHECK(nc_vm_rd32(&vm,0x304)==1000+i);CHECK(nc_vm_rd32(&vm,0x30c)==2000+i);
        CHECK(g_port_out[0][0]==0);
    }
    CHECK(signal_state(k,p->obj)==0);CHECK(p->n==0);
    /* A blocked remove is filled before the waiter becomes runnable. */
    a->state=NC_T_WAITING;a->wait=(NcWait){.objects={p->obj},.count=1};
    g_port_out[0][0]=0x300;g_port_out[0][1]=0x304;g_port_out[0][2]=0x308;
    nc_ke_port_queue(k,p->obj,42,43,44,45);CHECK(a->state==NC_T_READY);
    CHECK(nc_vm_rd32(&vm,0x300)==42);CHECK(nc_vm_rd32(&vm,0x308)==44);
    CHECK(nc_vm_rd32(&vm,0x30c)==45);CHECK(p->n==0);
    /* Truncation is rejected; no stale queue is retained by the next boot. */
    NcRd bad={.p=saved.p,.len=3};CHECK(!nc_ke_ports_load(&bad));nc_ke_ports_reset();CHECK(g_nports==0);
    checks+=test_sharing(k);free(saved.p);free(vm.base);free(k);
    printf("PASS xbased Android ARM64 CPU-only kernel/I-O regression: %u checks\n",checks);
    return 0;
}
