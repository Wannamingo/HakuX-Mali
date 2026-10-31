#ifndef HLE_SCALE_COPY_H
#define HLE_SCALE_COPY_H
/* Per-transition scratch. Destroy only before submission or after its fence. */
typedef struct HleScaleCopy {
    VkDescriptorSetLayout dsl;
    VkPipelineLayout pl;
    VkShaderModule shader;
    VkPipeline pipe;
    VkDescriptorPool pool;
    VkDescriptorSet set;
    VkBuffer buf[2];
    VkDeviceMemory mem[2];
} HleScaleCopy;
bool hle_scale_copy_init(HleScaleCopy *s, VkDeviceSize bytes);
void hle_scale_copy_free(HleScaleCopy *s);
void hle_scale_copy_record(HleScaleCopy *s, VkCommandBuffer cb, VkImage src, VkImage dst,
    VkImageAspectFlagBits aspect, uint32_t level, uint32_t face,
    uint32_t sw, uint32_t sh, uint32_t dw, uint32_t dh, uint32_t bytes);
#endif
