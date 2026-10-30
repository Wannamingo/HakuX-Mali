#!/usr/bin/env python3
"""Compile the actual C-embedded present shaders for both supported Vulkan targets.

Usage: python tools/native/check-present-shaders.py --glslc /path/to/ndk/glslc
No Android device, native build, external Python packages or game data required.
"""
import argparse
import ast
from pathlib import Path
import re
import shutil
import subprocess
import tempfile


def extract_strings(source):
    strings = {}
    token = re.compile(r'"(?:\\.|[^"\\])*"|/\*.*?\*/|//[^\n]*|;', re.S)
    for definition in re.finditer(r'static const char (k_\w+)\[\]\s*=\s*', source):
        pieces = []
        for match in token.finditer(source, definition.end()):
            if match.group() == ';':
                break
            if match.group().startswith('"'):
                pieces.append(ast.literal_eval(match.group()))
        strings[definition.group(1)] = ''.join(pieces)
    return strings


def check(glslc, output):
    repo = Path(__file__).resolve().parents[2]
    strings = extract_strings((repo / 'hw/xbox/hle/vk/swapchain.c').read_text(encoding='utf-8'))
    shaders = {'present.vert': strings['k_vs']}
    for name in ['bilinear', 'area', 'sgsr', 'fxaa']:
        shaders[name + '.frag'] = strings['k_fs_head'] + strings['k_fs_' + name]
    output.mkdir(parents=True, exist_ok=True)
    for name, shader in shaders.items():
        path = output / name
        path.write_text(shader, encoding='utf-8', newline='\n')
        for version in ['1.1', '1.3']:
            assembly = output / (name + '.vk' + version + '.spvasm')
            subprocess.run([str(glslc), '--target-env=vulkan' + version, '-Werror', '-S', str(path),
                            '-o', str(assembly)], check=True)
            layout = assembly.read_text(encoding='utf-8')
            for index, offset in [(3, 48), (4, 64), (5, 80)]:
                if not re.search(r'OpMemberDecorate %\w+ ' + str(index) + r' Offset ' + str(offset) + r'\b', layout):
                    raise RuntimeError(f'{name}: incorrect push-constant offset for member {index}')
        print(name + ': Vulkan 1.1 / 1.3 and picture push-constant layout OK')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--glslc', type=Path, default=shutil.which('glslc'))
    parser.add_argument('--output', type=Path, help='Keep generated shaders and SPIR-V assembly here')
    args = parser.parse_args()
    if args.glslc is None:
        parser.error('--glslc is required when the NDK compiler is not on PATH')
    if args.output:
        check(args.glslc, args.output)
    else:
        with tempfile.TemporaryDirectory(prefix='hakux-present-') as temporary:
            check(args.glslc, Path(temporary))
