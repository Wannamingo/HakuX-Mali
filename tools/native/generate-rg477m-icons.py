"""Compile the supplied launcher artwork into Android density resources (Pillow)."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw

repo = Path(__file__).resolve().parents[2]
source = repo / 'android/artwork/rg477m-launcher-source.png'
res = repo / 'android/app/src/main/res'
art = Image.open(source).convert('RGBA')
rgb = art.convert('RGB')
r, g, b = rgb.split()
silhouette = ImageChops.subtract(g, ImageChops.lighter(r, b)).point(lambda v: 255 if v > 10 else 0)
mono = Image.new('RGBA', art.size, 'white')
mono.putalpha(silhouette)

def centered(src, size, width, background):
    canvas = Image.new('RGBA', (size, size), background)
    height = round(width * src.height / src.width)
    fitted = src.resize((width, height), Image.Resampling.LANCZOS)
    canvas.alpha_composite(fitted, ((size - width) // 2, (size - height) // 2))
    return canvas

for density, scale in [('mdpi', 1), ('hdpi', 1.5), ('xhdpi', 2), ('xxhdpi', 3), ('xxxhdpi', 4)]:
    folder = res / ('mipmap-' + density)
    folder.mkdir(exist_ok=True)
    legacy = round(48 * scale)
    centered(art, legacy, round(44 * scale), 'black').convert('RGB').save(folder / 'ic_launcher.png', optimize=True)
    adaptive = round(108 * scale)
    centered(art, adaptive, round(54 * scale), (0, 0, 0, 0)).save(folder / 'ic_launcher_foreground.png', optimize=True)
    centered(mono, adaptive, round(54 * scale), (0, 0, 0, 0)).save(folder / 'ic_launcher_monochrome.png', optimize=True)
    Image.new('RGB', (adaptive, adaptive), 'black').save(folder / 'ic_launcher_background.png', optimize=True)

print('Compiled supplied artwork into legacy, adaptive and themed launcher resources for five densities')
