#include "hw/xbox/hle/base.h"
#include "hw/xbox/hle/shader/glsl_compile.h"
#include "hw/xbox/hle/vk/back_int.h"
#include "hw/xbox/hle/vk/scale_copy.h"
#include "hw/xbox/hle/vk/scale_copy_shader.h"
#define T (g_hvk.t)

void hle_scale_copy_free(HleScaleCopy *s)
{
    if (s->pool) T.vkDestroyDescriptorPool(g_hvk.dev,s->pool,NULL);
    if (s->pipe) T.vkDestroyPipeline(g_hvk.dev,s->pipe,NULL);
    if (s->shader) T.vkDestroyShaderModule(g_hvk.dev,s->shader,NULL);
    if (s->pl) T.vkDestroyPipelineLayout(g_hvk.dev,s->pl,NULL);
    if (s->dsl) T.vkDestroyDescriptorSetLayout(g_hvk.dev,s->dsl,NULL);
    for (unsigned i=0;i<2;i++) {
        if(s->buf[i])T.vkDestroyBuffer(g_hvk.dev,s->buf[i],NULL);
        if(s->mem[i])T.vkFreeMemory(g_hvk.dev,s->mem[i],NULL);
    }
    memset(s,0,sizeof(*s));
}

bool hle_scale_copy_init(HleScaleCopy *s,VkDeviceSize bytes)
{
    if(!bytes || bytes>g_hvk.props.limits.maxStorageBufferRange) return false;
    uint32_t nq=0;vkGetPhysicalDeviceQueueFamilyProperties(g_hvk.phys,&nq,NULL);
    VkQueueFamilyProperties *queues=g_new(VkQueueFamilyProperties,nq);
    vkGetPhysicalDeviceQueueFamilyProperties(g_hvk.phys,&nq,queues);
    bool compute=g_hvk.qfam<nq&&(queues[g_hvk.qfam].queueFlags&VK_QUEUE_COMPUTE_BIT);
    g_free(queues);if(!compute)return false;
    VkDescriptorSetLayoutBinding b[2]={{0,VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,1,VK_SHADER_STAGE_COMPUTE_BIT},
                                     {1,VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,1,VK_SHADER_STAGE_COMPUTE_BIT}};
    VkDescriptorSetLayoutCreateInfo di={.sType=VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO,.bindingCount=2,.pBindings=b};
    VkPushConstantRange range={VK_SHADER_STAGE_COMPUTE_BIT,0,20};
    VkPipelineLayoutCreateInfo li={.sType=VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,.setLayoutCount=1,
        .pSetLayouts=&s->dsl,.pushConstantRangeCount=1,.pPushConstantRanges=&range};
    if(T.vkCreateDescriptorSetLayout(g_hvk.dev,&di,NULL,&s->dsl)!=VK_SUCCESS ||
       T.vkCreatePipelineLayout(g_hvk.dev,&li,NULL,&s->pl)!=VK_SUCCESS)goto fail;
    size_t words=0;uint32_t *spv=hle_glsl_compile(HLE_SHADER_COMPUTE,hle_scale_copy_shader,&words);
    if(!spv)goto fail;
    VkShaderModuleCreateInfo mi={.sType=VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO,.codeSize=words*4,.pCode=spv};
    VkResult result=T.vkCreateShaderModule(g_hvk.dev,&mi,NULL,&s->shader);g_free(spv);
    if(result!=VK_SUCCESS)goto fail;
    VkComputePipelineCreateInfo ci={.sType=VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO,.layout=s->pl,
        .stage={.sType=VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO,.stage=VK_SHADER_STAGE_COMPUTE_BIT,
                .module=s->shader,.pName="main"}};
    if(!T.vkCreateComputePipelines||!T.vkCmdDispatch||
       T.vkCreateComputePipelines(g_hvk.dev,VK_NULL_HANDLE,1,&ci,NULL,&s->pipe)!=VK_SUCCESS)goto fail;
    for(unsigned i=0;i<2;i++) {
        VkBufferCreateInfo bi={.sType=VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO,.size=bytes,
            .usage=VK_BUFFER_USAGE_STORAGE_BUFFER_BIT|VK_BUFFER_USAGE_TRANSFER_SRC_BIT|VK_BUFFER_USAGE_TRANSFER_DST_BIT};
        if(T.vkCreateBuffer(g_hvk.dev,&bi,NULL,&s->buf[i])!=VK_SUCCESS)goto fail;
        VkMemoryRequirements mr;T.vkGetBufferMemoryRequirements(g_hvk.dev,s->buf[i],&mr);
        int type=hle_vk_memtype(mr.memoryTypeBits,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,0);
        VkMemoryAllocateInfo ai={.sType=VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,.allocationSize=mr.size,.memoryTypeIndex=(uint32_t)type};
        if(type<0||T.vkAllocateMemory(g_hvk.dev,&ai,NULL,&s->mem[i])!=VK_SUCCESS||
           T.vkBindBufferMemory(g_hvk.dev,s->buf[i],s->mem[i],0)!=VK_SUCCESS)goto fail;
    }
    VkDescriptorPoolSize ps={VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,2};
    VkDescriptorPoolCreateInfo pi={.sType=VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,.maxSets=1,.poolSizeCount=1,.pPoolSizes=&ps};
    if(T.vkCreateDescriptorPool(g_hvk.dev,&pi,NULL,&s->pool)!=VK_SUCCESS)goto fail;
    VkDescriptorSetAllocateInfo da={.sType=VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO,.descriptorPool=s->pool,
        .descriptorSetCount=1,.pSetLayouts=&s->dsl};
    if(T.vkAllocateDescriptorSets(g_hvk.dev,&da,&s->set)!=VK_SUCCESS)goto fail;
    for(unsigned i=0;i<2;i++) {
        VkDescriptorBufferInfo bi={s->buf[i],0,bytes};
        VkWriteDescriptorSet w={.sType=VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET,.dstSet=s->set,.dstBinding=i,
            .descriptorCount=1,.descriptorType=VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,.pBufferInfo=&bi};
        T.vkUpdateDescriptorSets(g_hvk.dev,1,&w,0,NULL);
    }
    return true;
fail:
    hle_scale_copy_free(s);return false;
}

static void sync_copy(VkCommandBuffer cb)
{
    VkMemoryBarrier2 b={.sType=VK_STRUCTURE_TYPE_MEMORY_BARRIER_2,
        .srcStageMask=VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,.srcAccessMask=VK_ACCESS_2_MEMORY_WRITE_BIT|VK_ACCESS_2_MEMORY_READ_BIT,
        .dstStageMask=VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,.dstAccessMask=VK_ACCESS_2_MEMORY_WRITE_BIT|VK_ACCESS_2_MEMORY_READ_BIT};
    VkDependencyInfo d={.sType=VK_STRUCTURE_TYPE_DEPENDENCY_INFO,.memoryBarrierCount=1,.pMemoryBarriers=&b};
    T.vkCmdPipelineBarrier2(cb,&d);
}

void hle_scale_copy_record(HleScaleCopy *s,VkCommandBuffer cb,VkImage src,VkImage dst,
    VkImageAspectFlagBits aspect,uint32_t level,uint32_t face,
    uint32_t sw,uint32_t sh,uint32_t dw,uint32_t dh,uint32_t bytes)
{
    sync_copy(cb); /* previous aspect's transfers no longer use scratch */
    VkBufferImageCopy copy={.imageSubresource={aspect,level,face,1},.imageExtent={sw,sh,1}};
    T.vkCmdCopyImageToBuffer(cb,src,VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,s->buf[0],1,&copy);
    sync_copy(cb);
    T.vkCmdBindPipeline(cb,VK_PIPELINE_BIND_POINT_COMPUTE,s->pipe);
    T.vkCmdBindDescriptorSets(cb,VK_PIPELINE_BIND_POINT_COMPUTE,s->pl,0,1,&s->set,0,NULL);
    uint32_t params[5]={sw,sh,dw,dh,bytes};
    T.vkCmdPushConstants(cb,s->pl,VK_SHADER_STAGE_COMPUTE_BIT,0,sizeof(params),params);
    uint32_t groups=((dw*dh*bytes+3)/4+63)/64;
    uint32_t gx=MIN(groups,g_hvk.props.limits.maxComputeWorkGroupCount[0]);
    T.vkCmdDispatch(cb,gx,(groups+gx-1)/gx,1);
    sync_copy(cb);
    copy.imageExtent=(VkExtent3D){dw,dh,1};
    T.vkCmdCopyBufferToImage(cb,s->buf[1],dst,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,1,&copy);
}
