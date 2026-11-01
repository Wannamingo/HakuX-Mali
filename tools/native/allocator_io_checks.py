from pathlib import Path
import os, re, subprocess, json

import argparse

parser = argparse.ArgumentParser(description='Build extracted allocator and I/O regression tests with VM fault injection.')
parser.add_argument('--cc', default='clang')
parser.add_argument('--target')
parser.add_argument('--sysroot')
parser.add_argument('--output', type=Path, required=True)
parser.add_argument('--original-mm', help='Read allocator source from this git ref, to demonstrate the regression detector.')
args = parser.parse_args()
R = Path(__file__).resolve().parents[2]
O = args.output.resolve()
O.mkdir(parents=True, exist_ok=True)

def function(text, name):
    m = re.search(r'^(?:static )?(?:bool|void|uint64_t|uint32_t|Region \*)\s*' + name + r'\([^;]*?\)\s*\{', text, re.M)
    assert m, name
    depth = 0
    for i in range(text.index('{', m.start()), len(text)):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if not depth:
                return text[m.start():i+1]
    raise ValueError(name)

mm = subprocess.check_output(['git', '-C', str(R), 'show', args.original_mm + ':native/kernel/mm.c']).decode() if args.original_mm else (R / 'native/kernel/mm.c').read_text(encoding='utf-8')
io = (R / 'native/kernel/io.c').read_text(encoding='utf-8')
code = r'''
#include <stdint.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#define NC_PAGE 4096u
#define MEM_COMMIT 0x1000u
#define MEM_RESERVE 0x2000u
#define MEM_TOP_DOWN 0x100000u
#define MEM_NOZERO 0x800000u
#define NC_STATUS_SUCCESS 0
#define NC_STATUS_NO_MEMORY 0xc0000017u
#define NC_STATUS_INVALID_PARAMETER 0xc000000du
#define USER_LO 0x10000u
#define USER_HI 0x7ffe0000u
#define VA_GRAIN 0x10000u
#define MAX_REGIONS 4096
typedef struct { uint32_t base,size,protect; bool image; } Region;
static Region g_regions[MAX_REGIONS];
static unsigned g_nregions;
typedef struct { uint32_t ram_size; uint8_t *ram; } NcVm;
typedef struct { NcVm *vm; uint64_t vm_commits,vm_decommits; } NcKernel;
typedef struct { int unused; } NcFrame;
typedef struct { bool async; uint64_t pos; } NcFile;
static uint32_t maps[0x80000], words[256];
static uint8_t ram[32*NC_PAGE], used[32];
static unsigned maps_called, fail_map, alloc_called, fail_alloc_after;
static uint32_t max_piece;
static unsigned checks;
#define CHECK(x) do { checks++; if(!(x)){fprintf(stderr,"FAIL line %d: %s\n",__LINE__,#x);return 42;} } while(0)
static uint32_t nc_align_up(uint32_t n,uint32_t a){return (n+a-1)&~(a-1);}
static uint32_t nc_vm_phys(NcVm *vm,uint32_t va){return maps[va/NC_PAGE];}
static uint32_t nc_vm_free_bytes(NcVm *vm){uint32_t n=0;for(unsigned i=0;i<32;i++)n+=!used[i];return n*NC_PAGE;}
static uint32_t nc_vm_alloc_phys(NcVm *vm,uint32_t n,uint32_t lo,uint32_t hi){
  alloc_called++;
  if(n>max_piece || (fail_alloc_after && alloc_called>fail_alloc_after))return UINT32_MAX;
  unsigned pages=n/NC_PAGE;
  for(unsigned i=0;i+pages<=32;i++){unsigned j=0;while(j<pages&&!used[i+j])j++;if(j==pages){memset(used+i,1,pages);return i*NC_PAGE;}}
  return UINT32_MAX;
}
static bool nc_vm_map(NcVm *vm,uint32_t va,uint32_t pa,uint32_t n){
  if(++maps_called==fail_map)return false;
  for(unsigned i=0;i<n;i+=NC_PAGE){assert(maps[(va+i)/NC_PAGE]==UINT32_MAX);maps[(va+i)/NC_PAGE]=pa+i;}return true;
}
static void nc_vm_unmap(NcVm *vm,uint32_t va,uint32_t n){for(unsigned i=0;i<n;i+=NC_PAGE)maps[(va+i)/NC_PAGE]=UINT32_MAX;}
static void nc_vm_free_phys(NcVm *vm,uint32_t pa,uint32_t n){for(unsigned i=0;i<n;i+=NC_PAGE){assert(used[(pa+i)/NC_PAGE]);used[(pa+i)/NC_PAGE]=0;}}
static uint32_t nc_vm_rd32(NcVm *vm,uint32_t va){return words[va/4];}
static void nc_vm_wr32(NcVm *vm,uint32_t va,uint32_t v){words[va/4]=v;}
static void nc_log(const char *tag,const char *fmt,...){ }
static void log_holders(NcKernel *k,const char *why){ }
static void nc_kernel_frame_chain(NcKernel *k,NcFrame *f,char *p,size_t n){p[0]=0;}
static void reset(NcKernel *k){
  memset(maps,255,sizeof(maps));memset(used,0,sizeof(used));memset(words,0,sizeof(words));memset(ram,0xa5,sizeof(ram));
  g_nregions=maps_called=fail_map=alloc_called=fail_alloc_after=0;max_piece=sizeof(ram);k->vm_commits=k->vm_decommits=0;
}
'''
names = ['region_at', 'range_free', 'add_region', 'find_free', 'commit', 'NtAllocateVirtualMemory']
code += '\n'.join(function(mm, n) for n in names)
code += '\n' + function(io, 'io_moved')
code += r'''
int main(int argc,char **argv){
  NcVm vm={sizeof(ram),ram};NcKernel k={.vm=&vm};NcFrame f={0};
  uint32_t a[]={0x40,0,0x44,MEM_COMMIT|MEM_RESERVE,4};
  reset(&k);words[0x44/4]=33*NC_PAGE;
  CHECK(NtAllocateVirtualMemory(&k,&f,a)==NC_STATUS_NO_MEMORY);
  CHECK(nc_vm_free_bytes(&vm)==sizeof(ram) && g_nregions==0 && k.vm_commits==0);
  CHECK(words[0x40/4]==0 && words[0x44/4]==33*NC_PAGE);
  words[0x44/4]=16*NC_PAGE;
  CHECK(NtAllocateVirtualMemory(&k,&f,a)==NC_STATUS_SUCCESS);
  CHECK(nc_vm_free_bytes(&vm)==16*NC_PAGE && g_nregions==1 && k.vm_commits==16);
  /* Existing pages survive both an allocation failure and a later map failure. */
  reset(&k);used[5]=1;maps[0x11000/NC_PAGE]=5*NC_PAGE;memset(ram+5*NC_PAGE,0x73,NC_PAGE);
  max_piece=NC_PAGE;fail_map=2;
  CHECK(!commit(&k,0x10000,4*NC_PAGE,true));
  CHECK(nc_vm_free_bytes(&vm)==31*NC_PAGE && k.vm_commits==0);
  CHECK(maps[0x11000/NC_PAGE]==5*NC_PAGE && maps[0x10000/NC_PAGE]==UINT32_MAX && maps[0x12000/NC_PAGE]==UINT32_MAX);
  for(unsigned i=0;i<NC_PAGE;i++)CHECK(ram[5*NC_PAGE+i]==0x73);
  reset(&k);max_piece=NC_PAGE;fail_alloc_after=4;
  CHECK(!commit(&k,0x10000,4*NC_PAGE,true));
  CHECK(nc_vm_free_bytes(&vm)==sizeof(ram) && k.vm_commits==0);
  for(unsigned i=0;i<4;i++)CHECK(maps[0x10000/NC_PAGE+i]==UINT32_MAX);
  /* Failure inside an existing reservation must retain that reservation. */
  reset(&k);add_region(0x10000,64*NC_PAGE,4,false);a[3]=MEM_COMMIT;words[0x40/4]=0x10000;words[0x44/4]=33*NC_PAGE;
  CHECK(NtAllocateVirtualMemory(&k,&f,a)==NC_STATUS_NO_MEMORY && g_nregions==1);
  CHECK(g_regions[0].base==0x10000 && g_regions[0].size==64*NC_PAGE);
  /* A commit over already committed pages leaves their content intact. */
  reset(&k);CHECK(commit(&k,0x11000,NC_PAGE,false));uint32_t pa=maps[0x11000/NC_PAGE];
  CHECK(ram[pa]==0xa5);CHECK(commit(&k,0x10000,3*NC_PAGE,true));
  CHECK(k.vm_commits==3 && ram[pa]==0xa5 && ram[maps[0x10000/NC_PAGE]]==0 && ram[maps[0x12000/NC_PAGE]]==0);
  /* Async explicit offsets, synchronous offsets, current-position sentinel,
   * and 128 KiB traffic blocks with the title advancing its position. */
  for(unsigned async=0;async<2;async++)for(unsigned kind=0;kind<4;kind++){
    NcFile file={async,0x100000000ull+0x20000};uint64_t old=file.pos;
    words[0x80/4]=kind==2?0xfffffffeu:0x40000;words[0x84/4]=kind==2?0xffffffffu:kind==3?1:0;
    io_moved(&k,&file,kind?0x80:0,0x123456789ull);
    CHECK(file.pos==((async && (kind==1||kind==3))?old:0x123456789ull));
  }
  NcFile traffic={true,0};words[0x84/4]=0;
  for(unsigned i=0;i<40;i++){uint64_t off=traffic.pos;words[0x80/4]=(uint32_t)off;io_moved(&k,&traffic,0x80,off+0x20000);traffic.pos+=0x20000;CHECK(traffic.pos==(uint64_t)(i+1)*0x20000);}
  printf("PASS %u checks: failed heap probes, reservation rollback, mapping/allocation fault injection, existing data, zero/nozero, sync/overlapped file positions\n",checks);
  return 0;
}
'''
(O / 'checks.c').write_text(code, encoding='utf-8')
cmd = [args.cc]
if args.target:
    cmd += ['--target=' + args.target]
if args.sysroot:
    cmd += ['--sysroot=' + args.sysroot]
cmd += ['-O2', '-Wall', '-Werror', '-Wno-unused-function', '-fPIE', '-pie', str(O / 'checks.c'), '-o', str(O / 'checks')]
subprocess.run(cmd, check=True, env=dict(os.environ, TEMP=str(O), TMP=str(O)))
(O / 'build.json').write_text(json.dumps(dict(production_functions=names + ['io_moved'], scope='Actual extracted allocator and file-position functions with VM fault injection; full game checks are separate.', original_mm=args.original_mm), indent=2))
print('Allocator and I/O checks compiled:', O / 'checks')
