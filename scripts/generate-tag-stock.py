#!/usr/bin/env python3
"""Original folder covers for Manage Tags. Mid-tone so they read in light and dark themes."""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = Path(__file__).resolve().parents[1] / "app/src/main/assets/tag_stock"
SIZE = 512


def font(size: int):
    for path in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ):
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def canvas(top, bottom):
    img = Image.new("RGB", (SIZE, SIZE), top)
    draw = ImageDraw.Draw(img)
    for y in range(SIZE):
        t = y / (SIZE - 1)
        r = int(top[0] + (bottom[0] - top[0]) * t)
        g = int(top[1] + (bottom[1] - top[1]) * t)
        b = int(top[2] + (bottom[2] - top[2]) * t)
        draw.line([(0, y), (SIZE, y)], fill=(r, g, b))
    return img, draw


def save(img: Image.Image, name: str) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    img = img.filter(ImageFilter.SMOOTH)
    img.save(OUT / f"{name}.jpg", "JPEG", quality=88)


def passport():
    img, d = canvas((46, 92, 110), (28, 62, 78))
    d.rounded_rectangle((118, 70, 394, 452), 22, fill=(132, 42, 52), outline=(201, 162, 39), width=6)
    d.ellipse((196, 130, 316, 250), outline=(201, 162, 39), width=8)
    d.polygon([(256, 148), (276, 210), (214, 174), (298, 174), (236, 210)], fill=(201, 162, 39))
    d.text((168, 290), "PASSPORT", font=font(28), fill=(236, 220, 170))
    d.rounded_rectangle((160, 350, 352, 400), 8, fill=(92, 28, 36))
    save(img, "passport")


def visa():
    img, d = canvas((92, 78, 118), (58, 48, 82))
    d.rounded_rectangle((80, 90, 432, 430), 16, fill=(236, 228, 210), outline=(90, 70, 50), width=4)
    d.ellipse((150, 140, 362, 352), outline=(176, 48, 48), width=14)
    d.text((188, 220), "ENTRY", font=font(36), fill=(176, 48, 48))
    d.line([(120, 380), (390, 380)], fill=(90, 70, 50), width=4)
    save(img, "visa")


def tickets():
    img, d = canvas((36, 78, 128), (24, 48, 88))
    d.rounded_rectangle((70, 150, 442, 360), 18, fill=(240, 236, 220), outline=(36, 78, 128), width=4)
    d.ellipse((70, 220, 110, 290), fill=(36, 78, 128))
    d.ellipse((402, 220, 442, 290), fill=(36, 78, 128))
    d.polygon([(140, 230), (300, 200), (360, 230), (300, 250), (140, 250)], fill=(36, 78, 128))
    d.polygon([(300, 200), (390, 170), (400, 190), (330, 220)], fill=(201, 162, 39))
    d.text((150, 280), "BOARDING", font=font(26), fill=(36, 78, 128))
    save(img, "tickets")


def hotel():
    img, d = canvas((48, 88, 108), (32, 56, 74))
    d.rectangle((150, 140, 362, 430), fill=(232, 220, 196), outline=(90, 70, 48), width=4)
    d.polygon([(128, 150), (256, 70), (384, 150)], fill=(176, 72, 62))
    for x in (180, 230, 280, 320):
        for y in (180, 240, 300, 360):
            d.rounded_rectangle((x, y, x + 28, y + 36), 4, fill=(90, 150, 176))
    d.rectangle((232, 370, 280, 430), fill=(90, 70, 48))
    save(img, "hotel")


def health():
    img, d = canvas((36, 102, 98), (24, 70, 72))
    d.ellipse((96, 96, 416, 416), fill=(236, 244, 238), outline=(36, 120, 88), width=8)
    d.rectangle((226, 150, 286, 362), fill=(196, 52, 58))
    d.rectangle((150, 226, 362, 286), fill=(196, 52, 58))
    save(img, "health")


def insurance():
    img, d = canvas((58, 72, 118), (38, 48, 86))
    d.polygon([(256, 80), (420, 150), (390, 340), (256, 430), (122, 340), (92, 150)], fill=(232, 228, 214), outline=(58, 72, 118), width=6)
    d.ellipse((186, 170, 326, 310), outline=(36, 120, 88), width=12)
    d.line([(220, 250), (250, 286), (304, 210)], fill=(36, 120, 88), width=12)
    save(img, "insurance")


def notes():
    img, d = canvas((118, 92, 58), (86, 64, 40))
    d.rounded_rectangle((110, 70, 402, 450), 10, fill=(246, 238, 210), outline=(90, 70, 40), width=4)
    d.rectangle((110, 70, 402, 120), fill=(201, 162, 39))
    for y in (170, 220, 270, 320, 370):
        d.line([(150, y), (360, y)], fill=(140, 120, 80), width=4)
    save(img, "notes")


def plans():
    img, d = canvas((46, 96, 86), (30, 68, 62))
    d.rounded_rectangle((100, 80, 412, 440), 16, fill=(236, 242, 228), outline=(46, 96, 86), width=5)
    boxes = [(140, 140), (140, 220), (140, 300)]
    for i, (x, y) in enumerate(boxes):
        d.rounded_rectangle((x, y, x + 48, y + 48), 8, outline=(46, 96, 86), width=5)
        if i < 2:
            d.line([(x + 10, y + 26), (x + 20, y + 36), (x + 38, y + 12)], fill=(36, 120, 72), width=6)
        d.line([(x + 70, y + 24), (x + 230, y + 24)], fill=(90, 110, 90), width=5)
    save(img, "plans")


def trail():
    img, d = canvas((48, 92, 72), (32, 58, 48))
    d.polygon([(0, 360), (120, 220), (220, 300), (340, 160), (512, 280), (512, 512), (0, 512)], fill=(92, 140, 78))
    d.polygon([(80, 512), (180, 280), (240, 300), (200, 512)], fill=(168, 132, 72))
    d.ellipse((236, 90, 300, 154), fill=(196, 52, 58), outline=(236, 220, 210), width=6)
    d.polygon([(256, 148), (300, 250), (212, 250)], fill=(196, 52, 58))
    save(img, "trail")


def family():
    img, d = canvas((86, 70, 118), (56, 46, 86))
    d.ellipse((150, 120, 230, 200), fill=(236, 210, 176))
    d.ellipse((282, 110, 372, 200), fill=(236, 210, 176))
    d.rounded_rectangle((140, 200, 242, 380), 40, fill=(72, 120, 168))
    d.rounded_rectangle((274, 196, 382, 380), 40, fill=(168, 80, 98))
    d.ellipse((230, 250, 282, 302), fill=(236, 210, 176))
    save(img, "family")


def car():
    img, d = canvas((58, 88, 108), (40, 60, 78))
    d.rounded_rectangle((80, 230, 432, 330), 28, fill=(201, 162, 39))
    d.polygon([(150, 230), (210, 160), (330, 160), (390, 230)], fill=(72, 120, 150))
    d.ellipse((130, 300, 210, 380), fill=(40, 40, 48))
    d.ellipse((310, 300, 390, 380), fill=(40, 40, 48))
    d.ellipse((150, 320, 190, 360), fill=(180, 190, 200))
    d.ellipse((330, 320, 370, 360), fill=(180, 190, 200))
    save(img, "car")


def trip():
    img, d = canvas((36, 82, 118), (24, 52, 86))
    d.ellipse((116, 90, 396, 370), fill=(72, 150, 150), outline=(236, 220, 180), width=8)
    d.arc((160, 110, 352, 350), 200, 340, fill=(236, 220, 180), width=8)
    d.line((256, 90, 256, 370), fill=(236, 220, 180), width=6)
    d.rounded_rectangle((180, 340, 332, 450), 16, fill=(168, 92, 52))
    d.rectangle((232, 318, 280, 350), fill=(201, 162, 39))
    save(img, "trip")


def folder():
    img, d = canvas((78, 96, 118), (52, 68, 88))
    d.rounded_rectangle((90, 160, 250, 210), 10, fill=(201, 162, 39))
    d.rounded_rectangle((90, 190, 422, 430), 18, fill=(232, 220, 188), outline=(90, 78, 52), width=4)
    d.rounded_rectangle((130, 150, 380, 390), 12, fill=(246, 238, 214))
    save(img, "folder")


def main() -> None:
    passport()
    visa()
    tickets()
    hotel()
    health()
    insurance()
    notes()
    plans()
    trail()
    family()
    car()
    trip()
    folder()
    print(f"Wrote {len(list(OUT.glob('*.jpg')))} covers to {OUT}")


if __name__ == "__main__":
    main()
