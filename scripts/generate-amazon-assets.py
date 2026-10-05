#!/usr/bin/env python3
"""
Generate Amazon Appstore Assets for Paperstow
- 114 x 114px PNG small icon (with transparency)
- 512 x 512px PNG large icon (with transparency)
- Screenshots across all Amazon Appstore approved resolutions:
    800 x 480px, 1024 x 600px, 1280 x 720px, 1280 x 800px,
    1920 x 1080px, 1920 x 1200px, 2560 x 1600px
  Both portrait and landscape presentations in PNG and JPG formats.
"""

import os
from PIL import Image, ImageDraw, ImageFilter

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS_DIR = os.path.join(BASE_DIR, "docs")
AMAZON_DIR = os.path.join(DOCS_DIR, "amazon")
ICON_OUT_DIR = os.path.join(AMAZON_DIR, "icon")
SCREENSHOTS_OUT_DIR = os.path.join(AMAZON_DIR, "screenshots")

SOURCE_ICON = os.path.join(DOCS_DIR, "brand", "paperstow-icon-1024.png")

SCREENS = [
    ("2.jpg", "01_home_vault", "Family Travel Papers Vault"),
    ("3.jpg", "02_tag_folder", "Organized Folders & Tags"),
    ("4.jpg", "03_document_preview", "Secure Document Viewer"),
    ("6.jpg", "04_import_scan", "Scan & Import Documents"),
    ("7.jpg", "05_ocr_search", "On-Device OCR & Search"),
]

# Resolution pairs: (Landscape width, height) & (Portrait width, height)
RESOLUTIONS = [
    {"name": "1920x1080", "landscape": (1920, 1080), "portrait": (1080, 1920)},
    {"name": "1280x800",  "landscape": (1280, 800),  "portrait": (800, 1280)},
    {"name": "1280x720",  "landscape": (1280, 720),  "portrait": (720, 1280)},
    {"name": "1920x1200", "landscape": (1920, 1200), "portrait": (1200, 1920)},
    {"name": "2560x1600", "landscape": (2560, 1600), "portrait": (1600, 2560)},
    {"name": "1024x600",  "landscape": (1024, 600),  "portrait": (600, 1024)},
    {"name": "800x480",   "landscape": (800, 480),    "portrait": (480, 800)},
]

def make_icons():
    os.makedirs(ICON_OUT_DIR, exist_ok=True)
    src = Image.open(SOURCE_ICON).convert("RGBA")
    
    # 1. 114x114 Squircle (Standard Amazon Small Icon with transparency)
    size = 114
    scale = 4
    mask = Image.new("L", (size * scale, size * scale), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), (size * scale - 1, size * scale - 1)], radius=24 * scale, fill=255)
    mask = mask.resize((size, size), Image.Resampling.LANCZOS)
    
    icon_114 = src.resize((size, size), Image.Resampling.LANCZOS)
    icon_114.putalpha(mask)
    
    out_114_path = os.path.join(ICON_OUT_DIR, "paperstow-icon-114x114.png")
    icon_114.save(out_114_path, format="PNG")
    print(f"✓ Generated {out_114_path} ({icon_114.size}, mode={icon_114.mode})")

    # Copy to root of amazon dir for easy discovery
    icon_114.save(os.path.join(AMAZON_DIR, "icon-114x114.png"), format="PNG")

    # 2. 114x114 Circular with transparency
    mask_circle = Image.new("L", (size * scale, size * scale), 0)
    draw_circle = ImageDraw.Draw(mask_circle)
    draw_circle.ellipse([(0, 0), (size * scale - 1, size * scale - 1)], fill=255)
    mask_circle = mask_circle.resize((size, size), Image.Resampling.LANCZOS)
    
    icon_114_circle = src.resize((size, size), Image.Resampling.LANCZOS)
    icon_114_circle.putalpha(mask_circle)
    out_114_circle = os.path.join(ICON_OUT_DIR, "paperstow-icon-114x114-circle.png")
    icon_114_circle.save(out_114_circle, format="PNG")
    print(f"✓ Generated {out_114_circle} ({icon_114_circle.size}, mode={icon_114_circle.mode})")

    # 3. 512x512 Large Icon with transparency
    size_512 = 512
    mask_512 = Image.new("L", (size_512 * 2, size_512 * 2), 0)
    draw_512 = ImageDraw.Draw(mask_512)
    draw_512.rounded_rectangle([(0, 0), (size_512 * 2 - 1, size_512 * 2 - 1)], radius=108 * 2, fill=255)
    mask_512 = mask_512.resize((size_512, size_512), Image.Resampling.LANCZOS)

    icon_512 = src.resize((size_512, size_512), Image.Resampling.LANCZOS)
    icon_512.putalpha(mask_512)
    out_512_path = os.path.join(ICON_OUT_DIR, "paperstow-icon-512x512.png")
    icon_512.save(out_512_path, format="PNG")
    icon_512.save(os.path.join(AMAZON_DIR, "icon-512x512.png"), format="PNG")
    print(f"✓ Generated {out_512_path} ({icon_512.size}, mode={icon_512.mode})")

def make_portrait_screenshots():
    portrait_base = os.path.join(SCREENSHOTS_OUT_DIR, "portrait")
    
    for res in RESOLUTIONS:
        pw, ph = res["portrait"]
        res_dir = os.path.join(portrait_base, f"{pw}x{ph}")
        os.makedirs(res_dir, exist_ok=True)
        
        for src_name, slug, label in SCREENS:
            src_path = os.path.join(DOCS_DIR, "screenshots", src_name)
            im = Image.open(src_path)
            # Clean status bar and nav bar: y=85 to y=2320
            clean = im.crop((0, 85, 1080, 2320))
            
            # Scale to target portrait size
            resized = clean.resize((pw, ph), Image.Resampling.LANCZOS)
            
            # Save PNG and JPG
            png_path = os.path.join(res_dir, f"{slug}.png")
            jpg_path = os.path.join(res_dir, f"{slug}.jpg")
            resized.save(png_path, format="PNG")
            resized.save(jpg_path, format="JPEG", quality=95)
            
        print(f"✓ Generated portrait screenshots for {pw}x{ph}")

def make_landscape_screenshots():
    landscape_base = os.path.join(SCREENSHOTS_OUT_DIR, "landscape")
    
    for res in RESOLUTIONS:
        lw, lh = res["landscape"]
        res_dir = os.path.join(landscape_base, f"{lw}x{lh}")
        os.makedirs(res_dir, exist_ok=True)
        
        for src_name, slug, label in SCREENS:
            src_path = os.path.join(DOCS_DIR, "screenshots", src_name)
            im = Image.open(src_path)
            clean = im.crop((0, 85, 1080, 2320))
            
            # Background with Paperstow navy brand gradient
            bg = Image.new("RGB", (lw, lh), (12, 32, 65))
            draw = ImageDraw.Draw(bg)
            for y in range(lh):
                factor = 1.0 - 0.28 * (y / lh)
                r = int(14 * factor)
                g = int(36 * factor)
                b = int(72 * factor)
                draw.line([(0, y), (lw, y)], fill=(r, g, b))
                
            # Frame height is 88% of canvas height
            phone_h = int(lh * 0.88)
            phone_w = int(phone_h * (clean.width / clean.height))
            
            screen_resized = clean.resize((phone_w, phone_h), Image.Resampling.LANCZOS)
            corner_r = max(4, int(phone_h * 0.035))
            
            # Mask for screen
            mask = Image.new("L", (phone_w, phone_h), 0)
            mask_draw = ImageDraw.Draw(mask)
            mask_draw.rounded_rectangle([(0, 0), (phone_w - 1, phone_h - 1)], radius=corner_r, fill=255)
            
            screen_rgba = screen_resized.convert("RGBA")
            screen_rgba.putalpha(mask)
            
            # Subtle drop shadow
            shadow_margin = max(10, int(lh * 0.03))
            shadow = Image.new("RGBA", (phone_w + shadow_margin * 2, phone_h + shadow_margin * 2), (0, 0, 0, 0))
            s_draw = ImageDraw.Draw(shadow)
            s_draw.rounded_rectangle(
                [(shadow_margin - 4, shadow_margin + 6), (phone_w + shadow_margin + 4, phone_h + shadow_margin + 12)],
                radius=corner_r + 4,
                fill=(0, 0, 0, 160)
            )
            blur_radius = max(4, int(shadow_margin * 0.6))
            shadow = shadow.filter(ImageFilter.GaussianBlur(blur_radius))
            
            x = (lw - phone_w) // 2
            y = (lh - phone_h) // 2
            
            bg.paste(shadow, (x - shadow_margin, y - shadow_margin), shadow)
            bg.paste(screen_rgba, (x, y), screen_rgba)
            
            # Save PNG and JPG
            png_path = os.path.join(res_dir, f"{slug}.png")
            jpg_path = os.path.join(res_dir, f"{slug}.jpg")
            bg.save(png_path, format="PNG")
            bg.save(jpg_path, format="JPEG", quality=95)
            
        print(f"✓ Generated landscape showcase screenshots for {lw}x{lh}")

if __name__ == "__main__":
    print("=== Generating Amazon Appstore Assets ===")
    make_icons()
    make_portrait_screenshots()
    make_landscape_screenshots()
    print("=== All Amazon Appstore Assets Generated Successfully ===")
