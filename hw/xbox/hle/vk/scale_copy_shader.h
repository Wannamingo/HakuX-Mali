#ifndef HLE_SCALE_COPY_SHADER_H
#define HLE_SCALE_COPY_SHADER_H
/* Bit-preserving nearest resampling through buffers. Depth/stencil blits
 * are not supported by MaliSX2 0.2.3 on G615. Separate aspects use 1, 2 or
 * 4 bytes per texel, including packed D16 and stencil, without 8/16-bit
 * storage extensions. Each invocation owns one destination uint. */
static const char hle_scale_copy_shader[] =
"#version 450\n"
"layout(local_size_x=64) in;\n"
"layout(set=0,binding=0,std430) readonly buffer Source { uint src[]; };\n"
"layout(set=0,binding=1,std430) writeonly buffer Destination { uint dst[]; };\n"
"layout(push_constant) uniform Params { uint sw,sh,dw,dh,bytes; } p;\n"
"void main() {\n"
" uint word=gl_GlobalInvocationID.x, first=word*4u;\n"
" uint total=p.dw*p.dh*p.bytes; if(first>=total) return;\n"
" uint result=0u;\n"
" for(uint j=0u;j<4u;j++) {\n"
"  uint byte=first+j; if(byte>=total) break;\n"
"  uint pixel=byte/p.bytes, channel=byte%p.bytes;\n"
"  uint x=pixel%p.dw, y=pixel/p.dw;\n"
"  uint sx=((2u*x+1u)*p.sw)/(2u*p.dw);\n"
"  uint sy=((2u*y+1u)*p.sh)/(2u*p.dh);\n"
"  uint offset=(sy*p.sw+sx)*p.bytes+channel;\n"
"  result|=((src[offset/4u]>>((offset%4u)*8u))&255u)<<(j*8u);\n"
" }\n"
" dst[word]=result;\n"
"}\n";
#endif
