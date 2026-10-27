/* Compile production seh.c with a controllable guest stack and CPU frame. */
#include <assert.h>
#include <stdarg.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#define NATIVE_KERNEL_KERNEL_H
#define NC_PAGE 4096u
#define NC_MAX_SEH 8
#define NC_KORDINAL_SEH_RETURN 0x3F9u
#define NC_KORDINAL_SEH_COLLIDED 0x3F7u
enum { NC_EAX, NC_ECX, NC_EDX, NC_EBX, NC_ESP, NC_EBP, NC_ESI, NC_EDI };
typedef struct NcSeh {
    bool unwind;
    uint32_t base, rec, ctx, reg, nested, target;
} NcSeh;
typedef struct NcThread { unsigned id, nseh; NcSeh seh[NC_MAX_SEH]; } NcThread;
typedef struct NcFrame { uint32_t gpr[8], eip, flags; } NcFrame;
typedef struct NcVm { uint8_t ram[65536]; uint32_t absent; } NcVm;
typedef struct NcKernel {
    NcThread *current;
    NcVm *vm;
    uint32_t kpcr_va, seh_return_va, seh_nested_va, seh_collided_va;
    bool redirected, stopped;
} NcKernel;
static uint32_t nc_vm_phys(NcVm *vm, uint32_t va) {
    return va >= sizeof(vm->ram) || va / NC_PAGE == vm->absent ? UINT32_MAX : va;
}
static void *nc_vm_host(NcVm *vm, uint32_t va) { assert(va < sizeof(vm->ram)); return vm->ram + va; }
static uint32_t nc_rd32(const void *p) { uint32_t v; memcpy(&v, p, 4); return v; }
static void nc_wr32(void *p, uint32_t v) { memcpy(p, &v, 4); }
static void nc_wr16(void *p, uint16_t v) { memcpy(p, &v, 2); }
static uint32_t nc_vm_rd32(NcVm *vm, uint32_t va) { return nc_rd32(nc_vm_host(vm, va)); }
static void nc_vm_wr32(NcVm *vm, uint32_t va, uint32_t v) { nc_wr32(nc_vm_host(vm, va), v); }
static uint32_t nc_frame_gpr(NcFrame *f, int reg) { return f->gpr[reg]; }
static void nc_frame_set_gpr(NcFrame *f, int reg, uint32_t v) { f->gpr[reg] = v; }
static uint32_t nc_frame_eip(NcFrame *f) { return f->eip; }
static void nc_frame_set_eip(NcFrame *f, uint32_t v) { f->eip = v; }
static uint32_t nc_frame_cs(NcFrame *f) { return 8; }
static uint32_t nc_frame_eflags(NcFrame *f) { return f->flags; }
static void nc_frame_set_eflags(NcFrame *f, uint32_t v) { f->flags = v; }
static void nc_log(const char *tag, const char *fmt, ...) {}
static void nc_kernel_request_stop(NcKernel *k, NcFrame *f, const char *fmt, ...) { k->stopped = true; }
static void nc_kernel_frame_chain(NcKernel *k, NcFrame *f, char *out, size_t len) { out[0] = 0; }
static void nc_kernel_impl(NcKernel *k, const char *name, uint64_t (*fn)(NcKernel *, NcFrame *, const uint32_t *)) {}

#include "native/kernel/seh.c"

static NcVm vm;
static NcThread thread;
static NcKernel kernel;
static NcFrame frame;

static void setup(void)
{
    memset(&vm, 0, sizeof(vm));
    memset(&thread, 0, sizeof(thread));
    memset(&kernel, 0, sizeof(kernel));
    memset(&frame, 0, sizeof(frame));
    vm.absent = UINT32_MAX;
    thread.id = 8;
    kernel.vm = &vm;
    kernel.current = &thread;
    kernel.kpcr_va = 0x100;
    kernel.seh_return_va = 0x3000;
    kernel.seh_nested_va = 0x3010;
    kernel.seh_collided_va = 0x3020;
    nc_vm_wr32(&vm, 0x100, 0xE100);
    nc_vm_wr32(&vm, 0x104, 0xF000);
    nc_vm_wr32(&vm, 0x108, 0x8000);
    nc_vm_wr32(&vm, 0xE100, CHAIN_END);
    nc_vm_wr32(&vm, 0xE104, 0x2000);
    for (unsigned i = 0; i < 8; i++) frame.gpr[i] = 0x11220000u + i;
    frame.gpr[NC_ESP] = 0xE000;
    frame.eip = 0x19C587;
    frame.flags = 0x202;
}

static void dispatch_and_continue(bool write)
{
    setup();
    const NcFrame original = frame;
    assert(nc_seh_access_violation(&kernel, &frame, 0x6E0000, write));
    assert(thread.nseh == 1 && !kernel.stopped && frame.eip == 0x2000);
    NcSeh *level = &thread.seh[0];
    assert(nc_vm_rd32(&vm, level->base) == kernel.seh_return_va);
    assert(nc_vm_rd32(&vm, level->base + 8) == 0xE100);
    assert(nc_vm_rd32(&vm, level->rec + ER_CODE) == 0xC0000005);
    assert(nc_vm_rd32(&vm, level->rec + ER_ADDRESS) == original.eip);
    assert(nc_vm_rd32(&vm, level->rec + ER_NPARAMS) == 2);
    assert(nc_vm_rd32(&vm, level->rec + ER_INFO) == (write ? 1u : 0u));
    assert(nc_vm_rd32(&vm, level->rec + ER_INFO + 4) == 0x6E0000);
    assert(nc_vm_rd32(&vm, level->ctx + CX_ESP) == original.gpr[NC_ESP]);
    /* A filter committed the page and returned CONTINUE_EXECUTION. The
     * faulting read must retry with its original stack and register state. */
    frame.gpr[NC_EAX] = CONTINUE_EXECUTION;
    frame.gpr[NC_ESP] = level->base + 4;
    nc_seh_stub(&kernel, &frame, NC_KORDINAL_SEH_RETURN);
    assert(thread.nseh == 0 && !kernel.stopped);
    assert(memcmp(frame.gpr, original.gpr, sizeof(frame.gpr)) == 0);
    assert(frame.eip == original.eip && frame.flags == original.flags);
    assert(chain_head(&kernel) == 0xE100);
}

int main(void)
{
    dispatch_and_continue(false);
    dispatch_and_continue(true);
    setup();
    nc_vm_wr32(&vm, 0x100, CHAIN_END);
    assert(!nc_seh_access_violation(&kernel, &frame, 0x4000, false));
    setup();
    frame.gpr[NC_ESP] = 0x8100; /* insufficient dispatch stack */
    assert(!nc_seh_access_violation(&kernel, &frame, 0x4000, false));
    setup();
    vm.absent = 0xD; /* unmapped dispatch stack */
    assert(!nc_seh_access_violation(&kernel, &frame, 0x4000, false));
    setup();
    nc_vm_wr32(&vm, 0x100, 0xF100); /* registration outside stack */
    assert(!nc_seh_access_violation(&kernel, &frame, 0x4000, false));
    setup();
    assert(nc_seh_access_violation(&kernel, &frame, 0x4000, false));
    frame.gpr[NC_EAX] = CONTINUE_SEARCH;
    frame.gpr[NC_ESP] = thread.seh[0].base + 4;
    nc_seh_stub(&kernel, &frame, NC_KORDINAL_SEH_RETURN);
    assert(kernel.stopped); /* an unhandled violation stops; it is never ignored */
    puts("PASS production SEH access violations, retry state, chain restoration and invalid stacks");
    return 0;
}
