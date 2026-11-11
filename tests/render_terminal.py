from pathlib import Path
import re
from PIL import Image, ImageDraw, ImageFont

# snimky pochazeji ze skutecneho vykreslovace a tady se jen prevedou na obrazek
palette = [(18, 22, 30), (205, 70, 70), (90, 190, 120), (225, 190, 80),
           (100, 140, 220), (180, 100, 205), (90, 190, 200), (225, 230, 238),
           (120, 130, 145), (255, 110, 110), (140, 240, 175), (255, 225, 130),
           (140, 185, 250), (230, 150, 250), (140, 235, 245), (245, 248, 255)]
font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf', 14)
cell_w, cell_h = 9, 20


def render(path):
    width, height = map(int, path.with_suffix('.size').read_text().split())
    default_fg, default_bg = palette[7], palette[0]
    fg, bg, reverse = default_fg, default_bg, False
    grid = [[(' ', fg, bg) for _ in range(width)] for _ in range(height)]
    x = y = 0
    data = path.read_text()
    index = 0
    while index < len(data):
        if data[index] == '\x1b':
            match = re.match(r'\x1b\[([0-9;?]*)([A-Za-z])', data[index:])
            if not match:
                index += 1
                continue
            values, command = match.groups()
            index += len(match.group())
            if values.startswith('?'):
                continue
            params = [int(v) if v else 0 for v in values.split(';')]
            if command in ('H', 'f'):
                y = (params[0] or 1) - 1
                x = ((params[1] or 1) if len(params) > 1 else 1) - 1
            elif command == 'J' and params[0] == 2:
                grid = [[(' ', default_fg, default_bg) for _ in range(width)] for _ in range(height)]
            elif command == 'm':
                p = 0
                while p < len(params):
                    value = params[p]
                    if value == 0: fg, bg, reverse = default_fg, default_bg, False
                    elif value == 7: reverse = True
                    elif value == 27: reverse = False
                    elif 30 <= value <= 37: fg = palette[value - 30]
                    elif 90 <= value <= 97: fg = palette[value - 90 + 8]
                    elif 40 <= value <= 47: bg = palette[value - 40]
                    elif value in (38, 48) and p + 4 < len(params) and params[p + 1] == 2:
                        color = tuple(params[p + 2:p + 5])
                        if value == 38: fg = color
                        else: bg = color
                        p += 4
                    p += 1
            continue
        char = data[index]
        index += 1
        if char == '\n': y += 1; x = 0; continue
        if char == '\r': x = 0; continue
        if ord(char) < 32: continue
        if 0 <= y < height and 0 <= x < width:
            grid[y][x] = (char, bg if reverse else fg, fg if reverse else bg)
        x += 1
    image = Image.new('RGB', (width * cell_w, height * cell_h), default_bg)
    draw = ImageDraw.Draw(image)
    for row, cells in enumerate(grid):
        for column, (char, fore, back) in enumerate(cells):
            draw.rectangle((column * cell_w, row * cell_h, (column + 1) * cell_w - 1, (row + 1) * cell_h - 1), fill=back)
            if char != ' ': draw.text((column * cell_w, row * cell_h + 1), char, font=font, fill=fore)
    image.save(path.with_suffix('.png'))
    path.with_suffix('.txt').write_text('\n'.join(''.join(cell[0] for cell in row) for row in grid))


for path in Path('build/ui-snapshots').glob('*.ansi'):
    render(path)
print('Rendered terminal frames in build/ui-snapshots')
