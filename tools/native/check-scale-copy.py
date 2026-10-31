#!/usr/bin/env python3
"""Build the independent Android GPU resampling probe and its production shader.

Run the resulting probe with: probe /path/to/selected/ICD.so scale_copy.spv
No game, save or application data is modified. Device execution is separate.
"""
from pathlib import Path
import argparse,ast,re,subprocess
p=argparse.ArgumentParser()
p.add_argument('--ndk',type=Path,required=True)
p.add_argument('--output',type=Path,required=True)
args=p.parse_args();repo=Path(__file__).resolve().parents[2]
args.output.mkdir(parents=True,exist_ok=True)
header=repo/'hw/xbox/hle/vk/scale_copy_shader.h'
shader=args.output/'scale_copy.comp'
shader.write_text(''.join(ast.literal_eval(s) for s in re.findall(r'"(?:[^"\\]|\\.)*"',header.read_text())),encoding='utf-8')
subprocess.run([str(args.ndk/'shader-tools/windows-x86_64/glslc.exe'),'--target-env=vulkan1.1','-Werror',str(shader),'-o',str(args.output/'scale_copy.spv')],check=True)
subprocess.run([str(args.ndk/'toolchains/llvm/prebuilt/windows-x86_64/bin/clang++.exe'),'--target=aarch64-linux-android29','-std=c++17','-O2','-static-libstdc++',
 str(repo/'tools/native/tests/scale_copy.cpp'),'-ldl','-o',str(args.output/'scale_copy_probe')],check=True)
print('Built probe and production shader in',args.output)
