#define VK_NO_PROTOTYPES
#include <vulkan/vulkan.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <vector>
#include <cstdint>
#define REQUIRE(x) do { if(!(x)) { std::printf("FAIL %d: %s\n",__LINE__,#x);std::fflush(stdout);std::exit(1); } } while(0)
#define V(x) REQUIRE((x)==VK_SUCCESS)
#define FUNCTIONS(X) \
 X(GetPhysicalDeviceProperties) X(GetPhysicalDeviceFormatProperties) X(GetPhysicalDeviceMemoryProperties) \
 X(GetPhysicalDeviceQueueFamilyProperties) X(CreateDevice) X(DestroyDevice) X(GetDeviceQueue) \
 X(CreateImage) X(DestroyImage) X(GetImageMemoryRequirements) X(AllocateMemory) X(FreeMemory) X(BindImageMemory) \
 X(CreateBuffer) X(DestroyBuffer) X(GetBufferMemoryRequirements) X(BindBufferMemory) X(MapMemory) X(UnmapMemory) \
 X(CreateCommandPool) X(DestroyCommandPool) X(AllocateCommandBuffers) X(BeginCommandBuffer) X(EndCommandBuffer) \
 X(CmdPipelineBarrier) X(CmdCopyBufferToImage) X(CmdBlitImage) X(CmdCopyImageToBuffer) \
 X(CreateFence) X(DestroyFence) X(QueueSubmit) X(WaitForFences)
#define COMPUTE(X) X(CreateDescriptorSetLayout) X(DestroyDescriptorSetLayout) X(CreatePipelineLayout) X(DestroyPipelineLayout) \
 X(CreateShaderModule) X(DestroyShaderModule) X(CreateComputePipelines) X(DestroyPipeline) X(CreateDescriptorPool) X(DestroyDescriptorPool) \
 X(AllocateDescriptorSets) X(UpdateDescriptorSets) X(CmdBindPipeline) X(CmdBindDescriptorSets) X(CmdPushConstants) X(CmdDispatch)
#define DECL(n) PFN_vk##n p##n;
FUNCTIONS(DECL)
COMPUTE(DECL)
VkPhysicalDevice phys;VkDevice dev;VkPhysicalDeviceMemoryProperties memory;
uint32_t memtype(uint32_t bits,VkMemoryPropertyFlags flags) {
 for(uint32_t i=0;i<memory.memoryTypeCount;i++)if((bits&(1u<<i))&&(memory.memoryTypes[i].propertyFlags&flags)==flags)return i;
 REQUIRE(false);return 0;
}
struct Image {VkImage img{};VkDeviceMemory mem{};};
Image image(VkFormat fmt,unsigned size,bool depth,unsigned mip) {
 Image a;VkImageCreateInfo ci{VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO};ci.imageType=VK_IMAGE_TYPE_2D;ci.format=fmt;
 ci.extent={size<<mip,size<<mip,1};ci.mipLevels=mip+1;ci.arrayLayers=2;ci.samples=VK_SAMPLE_COUNT_1_BIT;ci.tiling=VK_IMAGE_TILING_OPTIMAL;
 ci.usage=VK_IMAGE_USAGE_TRANSFER_SRC_BIT|VK_IMAGE_USAGE_TRANSFER_DST_BIT|VK_IMAGE_USAGE_SAMPLED_BIT|
   (depth?VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT:VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
 V(pCreateImage(dev,&ci,nullptr,&a.img));VkMemoryRequirements mr;pGetImageMemoryRequirements(dev,a.img,&mr);
 VkMemoryAllocateInfo ai{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO};ai.allocationSize=mr.size;ai.memoryTypeIndex=memtype(mr.memoryTypeBits,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
 V(pAllocateMemory(dev,&ai,nullptr,&a.mem));V(pBindImageMemory(dev,a.img,a.mem,0));return a;
}
void barrier(VkCommandBuffer cb,VkImage image,VkImageAspectFlags aspect,VkImageLayout old,VkImageLayout next) {
 VkImageMemoryBarrier b{VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER};b.srcAccessMask=old==VK_IMAGE_LAYOUT_UNDEFINED?0:VK_ACCESS_MEMORY_WRITE_BIT|VK_ACCESS_MEMORY_READ_BIT;
 b.dstAccessMask=VK_ACCESS_MEMORY_WRITE_BIT|VK_ACCESS_MEMORY_READ_BIT;b.oldLayout=old;b.newLayout=next;
 b.srcQueueFamilyIndex=b.dstQueueFamilyIndex=VK_QUEUE_FAMILY_IGNORED;b.image=image;b.subresourceRange={aspect,0,VK_REMAINING_MIP_LEVELS,0,VK_REMAINING_ARRAY_LAYERS};
 pCmdPipelineBarrier(cb,VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,0,0,nullptr,0,nullptr,1,&b);
}
int main(int argc,char**argv) {
 REQUIRE(argc==3);void* lib=dlopen(argv[1],RTLD_NOW|RTLD_LOCAL);if(!lib)puts(dlerror());REQUIRE(lib);
 auto get=(PFN_vkGetInstanceProcAddr)dlsym(lib,"vk_icdGetInstanceProcAddr");if(!get)get=(PFN_vkGetInstanceProcAddr)dlsym(lib,"vkGetInstanceProcAddr");REQUIRE(get);
 VkApplicationInfo app{VK_STRUCTURE_TYPE_APPLICATION_INFO};app.apiVersion=VK_API_VERSION_1_1;
 VkInstanceCreateInfo ic{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};ic.pApplicationInfo=&app;VkInstance inst;
 V(((PFN_vkCreateInstance)get(nullptr,"vkCreateInstance"))(&ic,nullptr,&inst));
 uint32_t count=1;V(((PFN_vkEnumeratePhysicalDevices)get(inst,"vkEnumeratePhysicalDevices"))(inst,&count,&phys));
#define LOAD(n) p##n=(PFN_vk##n)get(inst,"vk" #n);REQUIRE(p##n);
 FUNCTIONS(LOAD)
 COMPUTE(LOAD)
 VkPhysicalDeviceProperties prop;pGetPhysicalDeviceProperties(phys,&prop);printf("device=%s driver=%u\n",prop.deviceName,prop.driverVersion);
 pGetPhysicalDeviceMemoryProperties(phys,&memory);pGetPhysicalDeviceQueueFamilyProperties(phys,&count,nullptr);
 std::vector<VkQueueFamilyProperties> qp(count);pGetPhysicalDeviceQueueFamilyProperties(phys,&count,qp.data());unsigned family=0;
 while(family<count&&!(qp[family].queueFlags&VK_QUEUE_GRAPHICS_BIT))family++;REQUIRE(family<count);
 float priority=1;VkDeviceQueueCreateInfo qc{VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO};qc.queueFamilyIndex=family;qc.queueCount=1;qc.pQueuePriorities=&priority;
 VkDeviceCreateInfo dc{VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO};dc.queueCreateInfoCount=1;dc.pQueueCreateInfos=&qc;V(pCreateDevice(phys,&dc,nullptr,&dev));
 VkQueue queue;pGetDeviceQueue(dev,family,0,&queue);
 FILE* shader=fopen(argv[2],"rb");REQUIRE(shader);fseek(shader,0,SEEK_END);long length=ftell(shader);rewind(shader);
 std::vector<uint32_t> spv(length/4);REQUIRE(fread(spv.data(),1,length,shader)==(size_t)length);fclose(shader);
 VkShaderModule module;VkShaderModuleCreateInfo mc{VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO};mc.codeSize=length;mc.pCode=spv.data();V(pCreateShaderModule(dev,&mc,nullptr,&module));
 VkDescriptorSetLayoutBinding bindings[2]={{0,VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,1,VK_SHADER_STAGE_COMPUTE_BIT},{1,VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,1,VK_SHADER_STAGE_COMPUTE_BIT}};
 VkDescriptorSetLayout ds;VkDescriptorSetLayoutCreateInfo dl{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO};dl.bindingCount=2;dl.pBindings=bindings;V(pCreateDescriptorSetLayout(dev,&dl,nullptr,&ds));
 VkPushConstantRange range{VK_SHADER_STAGE_COMPUTE_BIT,0,20};VkPipelineLayout pl;VkPipelineLayoutCreateInfo lc{VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO};lc.setLayoutCount=1;lc.pSetLayouts=&ds;lc.pushConstantRangeCount=1;lc.pPushConstantRanges=&range;V(pCreatePipelineLayout(dev,&lc,nullptr,&pl));
 VkComputePipelineCreateInfo cc{VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO};cc.layout=pl;cc.stage={VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO};cc.stage.stage=VK_SHADER_STAGE_COMPUTE_BIT;cc.stage.module=module;cc.stage.pName="main";VkPipeline pipeline;V(pCreateComputePipelines(dev,VK_NULL_HANDLE,1,&cc,nullptr,&pipeline));
 const VkFormat formats[]={VK_FORMAT_B8G8R8A8_UNORM,VK_FORMAT_R5G6B5_UNORM_PACK16,VK_FORMAT_R8_UNORM,VK_FORMAT_R8G8_UNORM,
   VK_FORMAT_D16_UNORM,VK_FORMAT_D32_SFLOAT,VK_FORMAT_D24_UNORM_S8_UINT,VK_FORMAT_D32_SFLOAT_S8_UINT};
 for(unsigned base:{16u,25u})for(unsigned mip:{0u,1u})for(VkFormat fmt:formats) {
  VkFormatProperties fp;pGetPhysicalDeviceFormatProperties(phys,fmt,&fp);
  const auto need=VK_FORMAT_FEATURE_BLIT_SRC_BIT|VK_FORMAT_FEATURE_BLIT_DST_BIT;
  printf("size=%u mip=%u layer=1 format=%d optimal=%08x ",base,mip,fmt,fp.optimalTilingFeatures);fflush(stdout);
  bool blit=(fp.optimalTilingFeatures&need)==need;printf("%s ",blit?"BLIT":"COMPUTE");
  bool depth=fmt>=VK_FORMAT_D16_UNORM;bool stencil=fmt==VK_FORMAT_D24_UNORM_S8_UINT||fmt==VK_FORMAT_D32_SFLOAT_S8_UINT;
  unsigned bytes=fmt==VK_FORMAT_R8_UNORM?1:fmt==VK_FORMAT_R8G8_UNORM||fmt==VK_FORMAT_R5G6B5_UNORM_PACK16||fmt==VK_FORMAT_D16_UNORM?2:4;
  VkImageAspectFlags aspects=depth?VK_IMAGE_ASPECT_DEPTH_BIT:VK_IMAGE_ASPECT_COLOR_BIT;if(stencil)aspects|=VK_IMAGE_ASPECT_STENCIL_BIT;
  Image a=image(fmt,base,depth,mip),b=image(fmt,base*2,depth,mip),c=image(fmt,base,depth,mip);
  VkBuffer buffer;VkDeviceMemory bm;VkBufferCreateInfo bc{VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO};bc.size=65536;bc.usage=VK_BUFFER_USAGE_TRANSFER_SRC_BIT|VK_BUFFER_USAGE_TRANSFER_DST_BIT;
  V(pCreateBuffer(dev,&bc,nullptr,&buffer));VkMemoryRequirements mr;pGetBufferMemoryRequirements(dev,buffer,&mr);
  VkMemoryAllocateInfo ma{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO};ma.allocationSize=mr.size;ma.memoryTypeIndex=memtype(mr.memoryTypeBits,VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
  V(pAllocateMemory(dev,&ma,nullptr,&bm));V(pBindBufferMemory(dev,buffer,bm,0));uint8_t* map;V(pMapMemory(dev,bm,0,VK_WHOLE_SIZE,0,(void**)&map));memset(map,0,65536);
  VkBuffer scratch[2];VkDeviceMemory sm[2];VkDescriptorPool dp;VkDescriptorSet set;
  for(unsigned k=0;k<2;k++) {
   VkBufferCreateInfo sc{VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO};sc.size=65536;sc.usage=VK_BUFFER_USAGE_STORAGE_BUFFER_BIT|VK_BUFFER_USAGE_TRANSFER_SRC_BIT|VK_BUFFER_USAGE_TRANSFER_DST_BIT;
   V(pCreateBuffer(dev,&sc,nullptr,&scratch[k]));pGetBufferMemoryRequirements(dev,scratch[k],&mr);ma.allocationSize=mr.size;ma.memoryTypeIndex=memtype(mr.memoryTypeBits,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);V(pAllocateMemory(dev,&ma,nullptr,&sm[k]));V(pBindBufferMemory(dev,scratch[k],sm[k],0));
  }
  VkDescriptorPoolSize ps{VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,2};VkDescriptorPoolCreateInfo dpc{VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO};dpc.maxSets=1;dpc.poolSizeCount=1;dpc.pPoolSizes=&ps;V(pCreateDescriptorPool(dev,&dpc,nullptr,&dp));
  VkDescriptorSetAllocateInfo da{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO};da.descriptorPool=dp;da.descriptorSetCount=1;da.pSetLayouts=&ds;V(pAllocateDescriptorSets(dev,&da,&set));
  for(unsigned k=0;k<2;k++){VkDescriptorBufferInfo bi{scratch[k],0,65536};VkWriteDescriptorSet wr{VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET};wr.dstSet=set;wr.dstBinding=k;wr.descriptorCount=1;wr.descriptorType=VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;wr.pBufferInfo=&bi;pUpdateDescriptorSets(dev,1,&wr,0,nullptr);}
  for(unsigned i=0;i<base*base;i++) {
   if(depth&&fmt!=VK_FORMAT_D16_UNORM&&fmt!=VK_FORMAT_D24_UNORM_S8_UINT){float f=(i%7+1)/8.f;memcpy(map+i*4,&f,4);}
   else if(fmt==VK_FORMAT_D24_UNORM_S8_UINT){uint32_t d=(i%7+1)*0x100000;memcpy(map+i*4,&d,4);}
   else for(unsigned j=0;j<bytes;j++)map[i*bytes+j]=(i*37+j*51)&255;
   if(stencil)map[4096+i]=(i*13)&255;
  }
  VkCommandPool pool;VkCommandPoolCreateInfo pc{VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO};pc.queueFamilyIndex=family;V(pCreateCommandPool(dev,&pc,nullptr,&pool));
  VkCommandBuffer cb;VkCommandBufferAllocateInfo ca{VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO};ca.commandPool=pool;ca.commandBufferCount=1;ca.level=VK_COMMAND_BUFFER_LEVEL_PRIMARY;
  V(pAllocateCommandBuffers(dev,&ca,&cb));VkCommandBufferBeginInfo begin{VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO};V(pBeginCommandBuffer(cb,&begin));
  barrier(cb,a.img,aspects,VK_IMAGE_LAYOUT_UNDEFINED,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
  for(unsigned k=0;k<(stencil?2u:1u);k++) {
   VkBufferImageCopy copy{};copy.bufferOffset=k?4096:0;copy.imageSubresource={k?VK_IMAGE_ASPECT_STENCIL_BIT:depth?VK_IMAGE_ASPECT_DEPTH_BIT:VK_IMAGE_ASPECT_COLOR_BIT,mip,1,1};copy.imageExtent={base,base,1};
   pCmdCopyBufferToImage(cb,buffer,a.img,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,1,&copy);
  }
  barrier(cb,a.img,aspects,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
  for(unsigned k=0;k<2;k++) {
   auto src=k?b.img:a.img;auto dst=k?c.img:b.img;int ss=k?base*2:base,ds=k?base:base*2;
   barrier(cb,dst,aspects,VK_IMAGE_LAYOUT_UNDEFINED,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
   for(unsigned z=0;z<(stencil?2u:1u);z++) {
    VkImageBlit bl{};bl.srcSubresource={z?VK_IMAGE_ASPECT_STENCIL_BIT:depth?VK_IMAGE_ASPECT_DEPTH_BIT:VK_IMAGE_ASPECT_COLOR_BIT,mip,1,1};bl.dstSubresource=bl.srcSubresource;
    bl.srcOffsets[1]={ss,ss,1};bl.dstOffsets[1]={ds,ds,1};
    if(blit)pCmdBlitImage(cb,src,VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,dst,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,1,&bl,VK_FILTER_NEAREST);
    else {
     VkMemoryBarrier mb{VK_STRUCTURE_TYPE_MEMORY_BARRIER};mb.srcAccessMask=VK_ACCESS_MEMORY_WRITE_BIT|VK_ACCESS_MEMORY_READ_BIT;mb.dstAccessMask=mb.srcAccessMask;
     auto sync=[&](){pCmdPipelineBarrier(cb,VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,0,1,&mb,0,nullptr,0,nullptr);};sync();
     VkBufferImageCopy copy{};copy.imageSubresource=bl.srcSubresource;copy.imageExtent={(unsigned)ss,(unsigned)ss,1};pCmdCopyImageToBuffer(cb,src,VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,scratch[0],1,&copy);sync();
     pCmdBindPipeline(cb,VK_PIPELINE_BIND_POINT_COMPUTE,pipeline);pCmdBindDescriptorSets(cb,VK_PIPELINE_BIND_POINT_COMPUTE,pl,0,1,&set,0,nullptr);
     uint32_t params[5]={(unsigned)ss,(unsigned)ss,(unsigned)ds,(unsigned)ds,z?1:bytes};pCmdPushConstants(cb,pl,VK_SHADER_STAGE_COMPUTE_BIT,0,20,params);pCmdDispatch(cb,((ds*ds*params[4]+3)/4+63)/64,1,1);sync();
     copy.imageExtent={(unsigned)ds,(unsigned)ds,1};pCmdCopyBufferToImage(cb,scratch[1],dst,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,1,&copy);
    }
   }
   barrier(cb,dst,aspects,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
   if(k==0)for(unsigned z=0;z<(stencil?2u:1u);z++) {
    VkBufferImageCopy mid{};mid.bufferOffset=z?28672:16384;
    mid.imageSubresource={z?VK_IMAGE_ASPECT_STENCIL_BIT:depth?VK_IMAGE_ASPECT_DEPTH_BIT:VK_IMAGE_ASPECT_COLOR_BIT,mip,1,1};
    mid.imageExtent={base*2,base*2,1};pCmdCopyImageToBuffer(cb,b.img,VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,buffer,1,&mid);
   }

  }
  for(unsigned k=0;k<(stencil?2u:1u);k++) {
   VkBufferImageCopy copy{};copy.bufferOffset=40960+(k?4096:0);copy.imageSubresource={k?VK_IMAGE_ASPECT_STENCIL_BIT:depth?VK_IMAGE_ASPECT_DEPTH_BIT:VK_IMAGE_ASPECT_COLOR_BIT,mip,1,1};copy.imageExtent={base,base,1};
   pCmdCopyImageToBuffer(cb,c.img,VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,buffer,1,&copy);
  }
  V(pEndCommandBuffer(cb));VkFence fence;VkFenceCreateInfo fc{VK_STRUCTURE_TYPE_FENCE_CREATE_INFO};V(pCreateFence(dev,&fc,nullptr,&fence));
  VkSubmitInfo si{VK_STRUCTURE_TYPE_SUBMIT_INFO};si.commandBufferCount=1;si.pCommandBuffers=&cb;V(pQueueSubmit(queue,1,&si,fence));V(pWaitForFences(dev,1,&fence,VK_TRUE,10000000000ull));
  unsigned bad=0;for(unsigned i=0;i<base*base;i++)for(unsigned j=0;j<bytes;j++)if(!(fmt==VK_FORMAT_D24_UNORM_S8_UINT&&j==3)&&map[i*bytes+j]!=map[40960+i*bytes+j])bad++;
  if(stencil)for(unsigned i=0;i<base*base;i++)if(map[4096+i]!=map[45056+i])bad++;
  printf("roundtrip mismatch=%u ",bad);unsigned intermediate=0;
  for(unsigned y=0;y<base*2;y++)for(unsigned x=0;x<base*2;x++) {
   unsigned src=(y/2)*base+x/2,dst=y*(base*2)+x;
   for(unsigned j=0;j<bytes;j++)if(!(fmt==VK_FORMAT_D24_UNORM_S8_UINT&&j==3)&&map[src*bytes+j]!=map[16384+dst*bytes+j])intermediate++;
   if(stencil&&map[4096+src]!=map[28672+dst])intermediate++;
  }
  printf("independent 2x oracle mismatch=%u\n",intermediate);fflush(stdout);REQUIRE(!bad&&!intermediate);
  pDestroyFence(dev,fence,nullptr);pDestroyCommandPool(dev,pool,nullptr);pUnmapMemory(dev,bm);pDestroyBuffer(dev,buffer,nullptr);pFreeMemory(dev,bm,nullptr);
  pDestroyDescriptorPool(dev,dp,nullptr);for(unsigned k=0;k<2;k++){pDestroyBuffer(dev,scratch[k],nullptr);pFreeMemory(dev,sm[k],nullptr);}
  for(auto im:{a,b,c}){pDestroyImage(dev,im.img,nullptr);pFreeMemory(dev,im.mem,nullptr);}
 }
 pDestroyPipeline(dev,pipeline,nullptr);pDestroyPipelineLayout(dev,pl,nullptr);pDestroyDescriptorSetLayout(dev,ds,nullptr);pDestroyShaderModule(dev,module,nullptr);
 pDestroyDevice(dev,nullptr);((PFN_vkDestroyInstance)get(inst,"vkDestroyInstance"))(inst,nullptr);puts("PASS");
}
