#!/usr/bin/env python3
"""
Generate Amazon Fire TV Assets for Paperstow
- App Icon: 1280 x 720px PNG (no transparency / 24-bit RGB)
- Screenshots: 1920 x 1080px landscape JPG & PNG (no transparency / 24-bit RGB)
- Fire TV Background: 1920 x 1080px landscape (no transparency)
"""

import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS_DIR = os.path.join(BASE_DIR, "docs")
FIRETV_DIR = os.path.join(DOCS_DIR, "amazon", "firetv")
FIRETV_SCREENSHOTS_DIR = os.path.join(FIRETV_DIR, "screenshots")

SOURCE_ICON = os.path.join(DOCS_DIR, "brand", "paperstow-icon-1024.png")

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REGULAR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

SCREENS = [
    {
        "file": "2.jpg",
        "slug": "01_home_vault",
        "title": "Family Travel Vault",
        "tag": "OFFLINE VAULT",
        "desc": [
            "Keep passports, visas, boarding passes, and tickets",
            "encrypted on your device. Searchable at the gate",
            "even in airplane mode — no accounts, no cloud."
        ],
        "chips": ["• AES-256-GCM Encryption", "• 100% Offline & Private", "• No Cloud Required"]
    },
    {
        "file": "3.jpg",
        "slug": "02_tag_folder",
        "title": "Organized Trips & Tags",
        "tag": "ORGANIZATION",
        "desc": [
            "Categorize documents by trip, family member, or tag.",
            "Subfolder names automatically become tags during import.",
            "Write notes and manage interactive packing checklists."
        ],
        "chips": ["• Folder & Tag Organization", "• Travel Notes & Checklists", "• Auto-Categorization"]
    },
    {
        "file": "4.jpg",
        "slug": "03_document_preview",
        "title": "Secure Document Viewer",
        "tag": "PRIVACY & SECURITY",
        "desc": [
            "High-resolution PDF and photo viewer with instant rendering.",
            "Files are encrypted individually at rest using keys stored",
            "exclusively in Android's hardware KeyStore."
        ],
        "chips": ["• Multi-Page PDF Viewer", "• Hardware KeyStore Security", "• Instant Share Sheet"]
    },
    {
        "file": "6.jpg",
        "slug": "04_import_scan",
        "title": "Smart Ingest & Scanner",
        "tag": "EASY IMPORT",
        "desc": [
            "Import single PDFs or photos, scan physical papers with",
            "the on-device camera scanner, or pull in an entire folder.",
            "Other apps can share files directly into Paperstow."
        ],
        "chips": ["• On-Device Camera Scanner", "• Batch Folder Import", "• Share Sheet Ingest"]
    },
    {
        "file": "7.jpg",
        "slug": "05_ocr_search",
        "title": "On-Device OCR & Search",
        "tag": "INSTANT SEARCH",
        "desc": [
            "Bundled machine learning reads text from document pages",
            "directly on this device. Search across all files by filename,",
            "category tags, or words from the page itself."
        ],
        "chips": ["• Google ML Kit On-Device", "• Full-Text Keyword Search", "• Zero Data Upload"]
    }
]

def generate_firetv_icon():
    os.makedirs(FIRETV_DIR, exist_ok=True)
    W, H = 1280, 720
    
    # Base background: Paperstow deep navy gradient
    img = Image.new("RGB", (W, H), (12, 32, 65))
    draw = ImageDraw.Draw(img)
    
    # Subtle ambient lighting centered near the icon area
    for y in range(H):
        for x in range(0, W, 2):
            dist = math.sqrt((x - 450)**2 + (y - 320)**2)
            factor = max(0.0, min(1.0, dist / 800.0))
            r = int(18 * (1 - factor * 0.5) + 8 * (factor * 0.5))
            g = int(44 * (1 - factor * 0.5) + 20 * (factor * 0.5))
            b = int(88 * (1 - factor * 0.5) + 45 * (factor * 0.5))
            draw.point((x, y), fill=(r, g, b))
            draw.point((x + 1, y), fill=(r, g, b))
            
    # Load brand emblem
    src_icon = Image.open(SOURCE_ICON).convert("RGBA")
    icon_size = 350
    scale = 2
    
    # Rounded squircle mask for the icon mark
    mask = Image.new("L", (icon_size * scale, icon_size * scale), 0)
    m_draw = ImageDraw.Draw(mask)
    m_draw.rounded_rectangle([(0, 0), (icon_size * scale - 1, icon_size * scale - 1)], radius=70 * scale, fill=255)
    mask = mask.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    
    resized_icon = src_icon.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    resized_icon.putalpha(mask)
    
    # Icon Placement (comfortably within the 882x448 safe area: x in [199, 1081], y in [136, 584])
    icon_x = 210
    icon_y = (H - icon_size) // 2
    
    # Drop shadow
    shadow_margin = 30
    shadow = Image.new("RGBA", (icon_size + shadow_margin * 2, icon_size + shadow_margin * 2), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_draw.rounded_rectangle(
        [(shadow_margin - 4, shadow_margin + 8), (icon_size + shadow_margin + 4, icon_size + shadow_margin + 16)],
        radius=72,
        fill=(0, 0, 0, 160)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(16))
    
    img.paste(shadow, (icon_x - shadow_margin, icon_y - shadow_margin), shadow)
    img.paste(resized_icon, (icon_x, icon_y), resized_icon)
    
    # Typography
    font_title = ImageFont.truetype(FONT_BOLD, 70)
    font_badge = ImageFont.truetype(FONT_BOLD, 20)
    font_sub = ImageFont.truetype(FONT_REGULAR, 24)
    font_chips = ImageFont.truetype(FONT_BOLD, 16)
    
    text_x = icon_x + icon_size + 65
    text_y = icon_y + 35
    
    # Category Pill
    pill_w = 265
    pill_h = 32
    draw.rounded_rectangle([(text_x, text_y), (text_x + pill_w, text_y + pill_h)], radius=6, fill=(234, 140, 20))
    draw.text((text_x + 14, text_y + 6), "OFFLINE TRAVEL VAULT", fill=(255, 255, 255), font=font_badge)
    
    # App Title
    title_y = text_y + 44
    draw.text((text_x, title_y), "Paperstow", fill=(255, 255, 255), font=font_title)
    
    # Subtitle / Tagline
    tagline_y = title_y + 86
    draw.text((text_x, tagline_y), "Stow family passports, visas, & tickets.", fill=(236, 232, 245), font=font_sub)
    draw.text((text_x, tagline_y + 34), "100% on-device. Zero cloud.", fill=(200, 210, 230), font=font_sub)
    
    # Feature Badges
    chips_y = tagline_y + 86
    chips = ["AES-256-GCM", "On-Device OCR", "Biometric Lock"]
    chip_x = text_x
    for chip in chips:
        bbox = font_chips.getbbox(chip)
        chip_w = bbox[2] - bbox[0] + 24
        chip_h = 28
        draw.rounded_rectangle([(chip_x, chips_y), (chip_x + chip_w, chips_y + chip_h)], radius=5, fill=(24, 52, 96))
        draw.text((chip_x + 12, chips_y + 6), chip, fill=(220, 235, 255), font=font_chips)
        chip_x += chip_w + 14
        
    # MUST be strictly RGB PNG with no transparency
    final_img = img.convert("RGB")
    out_path = os.path.join(FIRETV_DIR, "icon-1280x720.png")
    final_img.save(out_path, format="PNG")
    
    # Also save with full named alias
    alias_path = os.path.join(FIRETV_DIR, "paperstow-firetv-icon-1280x720.png")
    final_img.save(alias_path, format="PNG")
    print(f"✓ Generated Fire TV App Icon: {out_path} ({final_img.size}, mode={final_img.mode})")

def generate_firetv_screenshots():
    os.makedirs(FIRETV_SCREENSHOTS_DIR, exist_ok=True)
    W, H = 1920, 1080
    
    font_brand = ImageFont.truetype(FONT_BOLD, 22)
    font_title = ImageFont.truetype(FONT_BOLD, 54)
    font_desc = ImageFont.truetype(FONT_REGULAR, 26)
    font_pill = ImageFont.truetype(FONT_BOLD, 20)
    
    for screen_info in SCREENS:
        img = Image.new("RGB", (W, H), (12, 32, 65))
        draw = ImageDraw.Draw(img)
        
        # Subtle horizontal gradient
        for y in range(H):
            factor = 1.0 - 0.28 * (y / H)
            r = int(14 * factor)
            g = int(36 * factor)
            b = int(72 * factor)
            draw.line([(0, y), (W, y)], fill=(r, g, b))
            
        # Ambient glow behind mobile screen
        glow = Image.new("RGBA", (800, 800), (0, 0, 0, 0))
        g_draw = ImageDraw.Draw(glow)
        g_draw.ellipse([(0, 0), (799, 799)], fill=(28, 70, 135, 110))
        glow = glow.filter(ImageFilter.GaussianBlur(90))
        img.paste(Image.new("RGB", (800, 800), (18, 48, 92)), (1050, 140), glow)
        
        # Load screenshot
        src_path = os.path.join(DOCS_DIR, "screenshots", screen_info["file"])
        im = Image.open(src_path)
        clean = im.crop((0, 85, 1080, 2320))
        
        phone_h = 920
        phone_w = int(phone_h * (clean.width / clean.height))  # ~444px
        
        screen_resized = clean.resize((phone_w, phone_h), Image.Resampling.LANCZOS)
        corner_r = 30
        
        mask = Image.new("L", (phone_w, phone_h), 0)
        m_draw = ImageDraw.Draw(mask)
        m_draw.rounded_rectangle([(0, 0), (phone_w - 1, phone_h - 1)], radius=corner_r, fill=255)
        
        screen_rgba = screen_resized.convert("RGBA")
        screen_rgba.putalpha(mask)
        
        # Drop shadow
        shadow_margin = 40
        shadow = Image.new("RGBA", (phone_w + shadow_margin * 2, phone_h + shadow_margin * 2), (0, 0, 0, 0))
        s_draw = ImageDraw.Draw(shadow)
        s_draw.rounded_rectangle(
            [(shadow_margin - 6, shadow_margin + 12), (phone_w + shadow_margin + 6, phone_h + shadow_margin + 20)],
            radius=corner_r + 4,
            fill=(0, 0, 0, 180)
        )
        shadow = shadow.filter(ImageFilter.GaussianBlur(22))
        
        phone_x = 1250
        phone_y = (H - phone_h) // 2
        
        img.paste(shadow, (phone_x - shadow_margin, phone_y - shadow_margin), shadow)
        img.paste(screen_rgba, (phone_x, phone_y), screen_rgba)
        
        # Left Text Layout
        text_x = 130
        text_y = 230
        
        # Brand pill / tag
        tag_text = screen_info["tag"]
        bbox_tag = font_brand.getbbox(tag_text)
        tag_w = bbox_tag[2] - bbox_tag[0] + 28
        draw.rounded_rectangle([(text_x, text_y), (text_x + tag_w, text_y + 36)], radius=6, fill=(234, 140, 20))
        draw.text((text_x + 14, text_y + 7), tag_text, fill=(255, 255, 255), font=font_brand)
        
        # Feature Headline
        title_y = text_y + 56
        draw.text((text_x, title_y), screen_info["title"], fill=(255, 255, 255), font=font_title)
        
        # Description Lines
        curr_y = title_y + 88
        for line in screen_info["desc"]:
            draw.text((text_x, curr_y), line, fill=(210, 220, 238), font=font_desc)
            curr_y += 40
            
        # Feature Highlights
        chip_y = curr_y + 44
        for chip in screen_info["chips"]:
            bbox = font_pill.getbbox(chip)
            bw = bbox[2] - bbox[0] + 32
            draw.rounded_rectangle([(text_x, chip_y), (text_x + bw, chip_y + 44)], radius=8, fill=(20, 48, 90))
            draw.text((text_x + 16, chip_y + 11), chip, fill=(240, 245, 255), font=font_pill)
            chip_y += 56
            
        # Ensure strictly RGB with no transparency
        final_img = img.convert("RGB")
        
        # Save both PNG and JPG
        png_path = os.path.join(FIRETV_SCREENSHOTS_DIR, f"{screen_info['slug']}.png")
        jpg_path = os.path.join(FIRETV_SCREENSHOTS_DIR, f"{screen_info['slug']}.jpg")
        final_img.save(png_path, format="PNG")
        final_img.save(jpg_path, format="JPEG", quality=95)
        
        print(f"✓ Generated Fire TV screenshot: {screen_info['slug']} (PNG & JPG, size={final_img.size}, mode={final_img.mode})")

def generate_firetv_background():
    W, H = 1920, 1080
    img = Image.new("RGB", (W, H), (12, 32, 65))
    draw = ImageDraw.Draw(img)
    for y in range(H):
        factor = 1.0 - 0.3 * (y / H)
        r = int(14 * factor)
        g = int(36 * factor)
        b = int(72 * factor)
        draw.line([(0, y), (W, y)], fill=(r, g, b))
        
    final_img = img.convert("RGB")
    bg_png = os.path.join(FIRETV_DIR, "background-1920x1080.png")
    bg_jpg = os.path.join(FIRETV_DIR, "background-1920x1080.jpg")
    final_img.save(bg_png, format="PNG")
    final_img.save(bg_jpg, format="JPEG", quality=95)
    print(f"✓ Generated Fire TV Background: {bg_png}")

if __name__ == "__main__":
    print("=== Generating Fire TV Assets ===")
    generate_firetv_icon()
    generate_firetv_screenshots()
    generate_firetv_background()
    print("=== Fire TV Assets Successfully Generated ===")
