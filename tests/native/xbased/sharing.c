/* Production share access and do_open: real temporary host file, no game resources. */
#include <assert.h>
#include "native/kernel/io.c"

static uint32_t obj_next=0x10000;
uint32_t nc_ob_create(NcKernel *k,enum NcObType type,uint32_t size) {
    (void)k;(void)type;uint32_t v=obj_next;obj_next+=size+0x20;return v;
}
uint32_t nc_ob_handle(NcKernel *k,uint32_t obj,enum NcObType type) {
    for(unsigned i=1;i<NC_MAX_HANDLES;i++)if(!k->handle_table[i].object){k->handle_table[i]=(NcHandle){obj,type};return i*4;}
    return 0;
}
void nc_kernel_trace_trigger(const char *p) { (void)p; }
bool nc_disc_find(const NcDisc *d,const char *path,NcDiscFile *out) { (void)d;(void)path;(void)out;return false; }
unsigned test_sharing(NcKernel *k) {
    char dir[]="/data/local/tmp/hakux-share-test-XXXXXX";assert(mkdtemp(dir));nc_io_set_hdd_root(dir);
    char part[512],path[512];snprintf(part,sizeof(part),"%s/Partition1",dir);assert(mkdir(part,0700)==0);
    snprintf(path,sizeof(path),"%s/cache.bin",part);int fd=open(path,O_CREAT|O_WRONLY,0600);assert(fd>=0);
    assert(write(fd,"original",8)==8);close(fd);
    const char *name="\\Device\\Harddisk0\\Partition1\\cache.bin";
    nc_vm_wr32(k->vm,0x800,0);nc_vm_wr32(k->vm,0x804,0x820);
    nc_wr16(nc_vm_host(k->vm,0x820),(uint16_t)strlen(name));nc_vm_wr32(k->vm,0x824,0x840);
    memcpy(nc_vm_host(k->vm,0x840),name,strlen(name));uint32_t h=0,info=0,h2=0;
    assert(do_open(k,0x40000000,0,0x800,FILE_OPEN,FILE_SYNCHRONOUS_IO,&h,&info)==0);
    assert(do_open(k,0x80000000,7,0x800,FILE_OPEN,FILE_SYNCHRONOUS_IO,&h2,&info)==STATUS_SHARING_VIOLATION);
    /* Conflict must be refused before an overwrite can truncate or rename. */
    assert(do_open(k,0x40000000,7,0x800,FILE_OVERWRITE,0,&h2,&info)==STATUS_SHARING_VIOLATION);
    struct stat st;assert(stat(path,&st)==0&&st.st_size==8);
    NcFile *nf=file_of(k,h);assert(nf);close(nf->fd);nf->used=false;
    assert(do_open(k,0x80000000,SHARE_READ,0x800,FILE_OPEN,0,&h,&info)==0);
    assert(do_open(k,0x80000000,SHARE_READ,0x800,FILE_OPEN,0,&h2,&info)==0);
    assert(do_open(k,0x40000000,7,0x800,FILE_OPEN,0,&h2,&info)==STATUS_SHARING_VIOLATION);
    /* Attribute-only access does not participate in data sharing. */
    assert(do_open(k,0x80,0,0x800,FILE_OPEN,0,&h2,&info)==0);
    assert(share_granted(0x10000000)==7);assert(share_granted(0x80)==0);
    for(unsigned i=0;i<MAX_FILES;i++)if(g_files[i].used){if(g_files[i].fd>=0)close(g_files[i].fd);g_files[i].used=false;}
    unlink(path);rmdir(part);rmdir(dir);return 12;
}
