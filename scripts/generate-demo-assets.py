#!/usr/bin/env python3
"""Generate bundled Paperstow sample-trip documents (valid image PDFs, images, text, markdown)."""

from __future__ import annotations

import io
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parents[1] / "app/src/main/assets/demo"
NAVY = (12, 32, 65)
GOLD = (201, 162, 39)
CREAM = (250, 247, 240)
TEAL = (0, 105, 120)
RED = (176, 42, 42)
GREEN = (36, 120, 72)
INK = (28, 28, 28)
MUTED = (90, 90, 90)


def font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for path in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    ):
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def write_jpeg_pdf(path: Path, jpeg: bytes, width: int, height: int) -> None:
    """Write a one-page PDF that embeds a JPEG. Pdfium / Android PdfRenderer accept this reliably."""
    content = f"q {width} 0 0 {height} 0 0 cm /Im0 Do Q\n".encode("ascii")

    def pack(n: int, payload: bytes) -> bytes:
        return f"{n} 0 obj\n".encode("ascii") + payload + b"\nendobj\n"

    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        (
            f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {width} {height}] "
            f"/Contents 4 0 R /Resources << /XObject << /Im0 5 0 R >> >> >>"
        ).encode("ascii"),
        b"<< /Length %d >>\nstream\n" % len(content) + content + b"endstream",
        (
            b"<< /Type /XObject /Subtype /Image /Width %d /Height %d "
            b"/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode "
            b"/Length %d >>\nstream\n" % (width, height, len(jpeg))
            + jpeg
            + b"\nendstream"
        ),
    ]
    body = b"%PDF-1.4\n"
    xref = [0]
    for i, obj in enumerate(objects, start=1):
        xref.append(len(body))
        body += pack(i, obj)
    startxref = len(body)
    table = f"xref\n0 {len(objects) + 1}\n0000000000 65535 f \n".encode("ascii")
    for offset in xref[1:]:
        table += f"{offset:010d} 00000 n \n".encode("ascii")
    trailer = (
        f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{startxref}\n%%EOF\n"
    ).encode("ascii")
    path.write_bytes(body + table + trailer)


def pdf(path: Path, title: str, lines: list[str], accent: tuple[int, int, int] = NAVY) -> None:
    width, height = 1240, 1754
    img = Image.new("RGB", (width, height), CREAM)
    draw = ImageDraw.Draw(img)
    draw.rectangle((36, 36, width - 37, height - 37), outline=accent, width=8)
    draw.rectangle((36, 36, width - 37, 150), fill=accent)
    draw.text((64, 70), title, fill=(255, 255, 255), font=font(36))
    y = 196
    for line in lines:
        color = RED if line.startswith("SAMPLE") or line.startswith("Fictional") else INK
        size = 20 if line.startswith("SAMPLE") or line.startswith("Fictional") else 28
        draw.text((72, y), line, fill=color, font=font(size))
        y += 40
    draw.text(
        (72, height - 90),
        "Paperstow sample  ·  fictional  ·  not an official document",
        fill=MUTED,
        font=font(20),
    )
    buf = io.BytesIO()
    img.save(buf, "JPEG", quality=86)
    write_jpeg_pdf(path, buf.getvalue(), width, height)


def card(size: tuple[int, int], bg: tuple[int, int, int]) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    img = Image.new("RGB", size, bg)
    draw = ImageDraw.Draw(img)
    draw.rectangle((16, 16, size[0] - 17, size[1] - 17), outline=GOLD, width=4)
    return img, draw


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.iterdir():
        if old.is_file():
            old.unlink()

    pdf(
        OUT / "john_doe_passport.pdf",
        "PASSPORT  —  SAMPLE",
        [
            "SAMPLE · not a real passport",
            "",
            "Type: P     Code: XX     Passport No: X0000001",
            "Surname: DOE",
            "Given names: JOHN",
            "Nationality: SAMPLE",
            "Date of birth: 01 JAN 1980",
            "Sex: M     Place of birth: SAMPLE CITY",
            "Date of issue: 01 JAN 2024     Date of expiry: 01 JAN 2034",
            "Authority: SAMPLE ISSUING OFFICE",
            "",
            "Machine readable zone (sample):",
            "P<XXXDOE<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<",
            "X00000011XXX8001011M3401015<<<<<<<<<<<00",
        ],
    )
    pdf(
        OUT / "visa_letter.pdf",
        "VISA LETTER  —  SAMPLE",
        [
            "SAMPLE · fictional correspondence",
            "",
            "Applicant: Jane Doe",
            "Passport: X0000002",
            "Visa type: Visitor  ·  tourism",
            "Valid: 01 JUN 2026 – 30 JUN 2026",
            "Duration of stay: 14 days     Entries: 1",
            "",
            "Collect the passport from the sample counter.",
            "Fictional letter for Paperstow demos only.",
        ],
    )
    pdf(
        OUT / "boarding_pass.pdf",
        "BOARDING PASS  —  SAMPLE",
        [
            "SAMPLE · not a real ticket",
            "",
            "Passenger: DOE / JOHN",
            "Flight: PS 101     Date: 01 JUN 2026",
            "From: Rivertown (RVT)     To: Lakeside (LKS)",
            "Depart: 09:15     Gate: A2     Seat: 12C     Zone: 2",
            "Booking ref: ABC123     Sequence: 018",
            "Ticket: 000 0000 0000001",
            "",
            "Baggage: 1 x 23kg. Arrive at the gate 40 minutes early.",
        ],
    )
    pdf(
        OUT / "hotel_confirmation.pdf",
        "HOTEL CONFIRMATION  —  SAMPLE",
        [
            "SAMPLE · not a real booking",
            "",
            "Lakeside Inn  (fictional property)",
            "Confirmation: LK-10021",
            "Guests: Jane Doe, John Doe",
            "Check-in: 01 JUN 2026 after 15:00",
            "Check-out: 08 JUN 2026 before 11:00",
            "Room: Twin, non-smoking, 2 adults",
            "Rate: 148 / night, breakfast included",
            "Address: 100 Harbor Street, Lakeside (sample)",
            "Phone: +1 555 0100",
            "",
            "Cancel free until 30 MAY 2026.",
        ],
    )
    pdf(
        OUT / "travel_insurance.pdf",
        "TRAVEL INSURANCE  —  SAMPLE",
        [
            "SAMPLE · not a real policy",
            "",
            "Policy: PS-TRAVEL-10001",
            "Insured: John Doe     DOB: 01 Jan 1980",
            "Also covered: Jane Doe     DOB: 15 Mar 1982",
            "Covered trip: 01 JUN 2026 – 08 JUN 2026",
            "Destinations: Rivertown, Lakeside",
            "",
            "Medical expenses: 250,000",
            "Emergency evacuation: 500,000",
            "Trip cancellation: 5,000",
            "Baggage: 2,000     Excess: 100",
            "",
            "Emergency: +1 555 0199 (sample number)",
        ],
        GREEN,
    )
    pdf(
        OUT / "vaccination_record.pdf",
        "VACCINATION RECORD  —  SAMPLE",
        [
            "SAMPLE · not a real medical record",
            "",
            "Patient: John Doe",
            "Date of birth: 01 Jan 1980     Sex: M",
            "Record ID: VAC-10001",
            "Clinic: Community Health Center (sample)",
            "",
            "Influenza       12 Oct 2025   left arm",
            "Tetanus-diph.   03 Mar 2024   right arm",
            "Hepatitis A     18 Jan 2024   left arm",
            "COVID-19 dose 1 02 Feb 2021   Pfizer",
            "COVID-19 dose 2 23 Feb 2021   Pfizer",
            "COVID-19 boost  10 Jan 2023   Pfizer",
            "",
            "Next due: influenza, autumn 2026",
            "Fictional values for Paperstow demos only.",
        ],
        TEAL,
    )
    pdf(
        OUT / "blood_test_report.pdf",
        "BLOOD TEST REPORT  —  SAMPLE",
        [
            "SAMPLE · not a real lab result",
            "",
            "Patient: Jane Doe",
            "Date of birth: 15 Mar 1982     Sex: F",
            "Collected: 20 Apr 2026     Reported: 21 Apr 2026",
            "Lab: Community Health Lab (sample)     Acc: LAB-20421",
            "Ordered by: Dr. A. Smith",
            "",
            "Test                 Result    Ref. range",
            "Hemoglobin           13.6 g/dL  12.0–15.5",
            "White blood cells    6.4 x10^9  4.0–11.0",
            "Platelets            248 x10^9  150–400",
            "Glucose (fasting)    92 mg/dL   70–99",
            "Creatinine           0.8 mg/dL  0.5–1.1",
            "TSH                  1.8 mIU/L  0.4–4.0",
            "",
            "Comment: All listed values are within the sample ranges.",
            "Fictional report for Paperstow demos only.",
        ],
        TEAL,
    )

    img, d = card((720, 960), (232, 214, 184))
    d.rectangle((80, 80, 640, 420), fill=(210, 190, 160))
    d.ellipse((220, 140, 500, 400), fill=(90, 70, 60))
    d.ellipse((250, 160, 470, 360), fill=(196, 154, 122))
    d.text((80, 460), "SAMPLE  ·  NOT A REAL ID", fill=RED, font=font(28))
    d.text((80, 520), "DOE, John", fill=NAVY, font=font(42))
    d.text((80, 590), "Passport photo", fill=NAVY, font=font(26))
    d.text((80, 660), "DOB 01 Jan 1980   Exp 01 Jan 2034", fill=(60, 60, 60), font=font(22))
    d.text((80, 820), "Paperstow demo image", fill=MUTED, font=font(20))
    img.save(OUT / "john_doe_photo.png", "PNG")

    img, d = card((720, 960), (236, 220, 200))
    d.rectangle((80, 80, 640, 420), fill=(214, 186, 164))
    d.ellipse((220, 140, 500, 400), fill=(80, 58, 48))
    d.ellipse((250, 160, 470, 360), fill=(210, 168, 136))
    d.text((80, 460), "SAMPLE  ·  NOT A REAL ID", fill=RED, font=font(28))
    d.text((80, 520), "DOE, Jane", fill=NAVY, font=font(42))
    d.text((80, 590), "Passport photo", fill=NAVY, font=font(26))
    d.text((80, 660), "DOB 15 Mar 1982   Exp 01 Jan 2034", fill=(60, 60, 60), font=font(22))
    d.text((80, 820), "Paperstow demo image", fill=MUTED, font=font(20))
    img.save(OUT / "jane_doe_photo.png", "PNG")

    img, d = card((900, 640), CREAM)
    d.ellipse((260, 80, 640, 460), outline=RED, width=10)
    d.text((310, 220), "VISA", fill=RED, font=font(72))
    d.text((300, 310), "ENTRY  01 JUN 26", fill=RED, font=font(28))
    d.text((80, 500), "LKS  ·  sample stamp  ·  not official", fill=MUTED, font=font(24))
    img.save(OUT / "visa_stamp.png", "PNG")

    img, d = card((1080, 480), TEAL)
    d.rectangle((40, 40, 1040, 440), fill=(245, 248, 250))
    d.text((70, 70), "PS 101   RVT → LKS", fill=NAVY, font=font(44))
    d.text((70, 150), "DOE / JANE     Seat 12A", fill=(40, 40, 40), font=font(32))
    d.text((70, 220), "01 Jun 2026   09:15   Gate A2", fill=(40, 40, 40), font=font(28))
    d.rectangle((820, 80, 1000, 260), fill=NAVY)
    for r in range(6):
        for c in range(6):
            if (r + c) % 2 == 0:
                d.rectangle((830 + c * 26, 90 + r * 26, 852 + c * 26, 112 + r * 26), fill=(240, 240, 240))
    d.text((70, 340), "Sample mobile pass  ·  Paperstow demo", fill=(80, 80, 80), font=font(22))
    img.save(OUT / "boarding_pass_mobile.jpg", "JPEG", quality=88)

    img, d = card((360, 220), (255, 255, 255))
    d.rectangle((6, 6, 353, 213), fill=(230, 245, 255), outline=NAVY, width=3)
    d.text((16, 14), "VACCINATION CARD", fill=NAVY, font=font(16))
    d.text((16, 40), "SAMPLE  ·  not a real record", fill=RED, font=font(12))
    d.text((16, 70), "Jane Doe", fill=INK, font=font(14))
    d.text((16, 96), "Flu 10 Oct 25   Td 04 Apr 24", fill=INK, font=font(12))
    d.text((16, 118), "Hep A 19 Jan 24   COVID boost", fill=INK, font=font(12))
    d.text((16, 170), "Paperstow demo BMP", fill=MUTED, font=font(11))
    img.save(OUT / "jane_vaccine_card.bmp", "BMP")

    frames = []
    for i, shade in enumerate(((20, 80, 90), (12, 32, 65))):
        frame, d = card((800, 500), shade)
        d.text((60, 80), "LAKESIDE INN", fill=GOLD, font=font(36))
        d.text((60, 160), "Room 214   Key packet", fill=(255, 255, 255), font=font(36))
        d.text((60, 240), "Wifi: LakesideGuest", fill=(230, 230, 230), font=font(26))
        d.text((60, 300), "Password: welcome2026", fill=(230, 230, 230), font=font(26))
        d.text((60, 400), f"Sample GIF frame {i + 1}  ·  Paperstow demo", fill=(200, 200, 200), font=font(20))
        frames.append(frame)
    frames[0].save(
        OUT / "hotel_key_packet.gif",
        save_all=True,
        append_images=frames[1:],
        duration=700,
        loop=0,
    )

    (OUT / "emergency_contacts.txt").write_text(
        "Emergency contacts (sample)\n"
        "============================\n\n"
        "John Doe  +1 555 0101\n"
        "Jane Doe  +1 555 0102\n"
        "Family — Alex Doe  +1 555 0103\n"
        "Travel insurer 24h  +1 555 0199\n"
        "Hotel front desk  +1 555 0100\n\n"
        "Blood type (John): O+\n"
        "Blood type (Jane): A+\n"
        "Allergies: none listed\n\n"
        "Fictional contacts for Paperstow demos. Replace with your own.\n",
        encoding="utf-8",
    )
    (OUT / "packing_list.txt").write_text(
        "Packing list — Lakeside week (sample)\n"
        "------------------------------------\n"
        "[ ] Passports and visa letter\n"
        "[ ] Insurance card\n"
        "[ ] Vaccination record and blood test copy\n"
        "[ ] Chargers and power bank\n"
        "[ ] Light jacket\n"
        "[ ] Medicines\n"
        "[ ] Copies of bookings (also in Paperstow)\n\n"
        "Sample note you can edit after import.\n",
        encoding="utf-8",
    )
    (OUT / "flight_itinerary.txt").write_text(
        "Itinerary — Doe family (sample)\n"
        "==============================\n\n"
        "01 Jun  PS101  RVT 09:15 → LKS 11:05\n"
        "01–08 Jun  Lakeside Inn, Room 214\n"
        "08 Jun  PS102  LKS 16:40 → RVT 18:30\n\n"
        "Seats: 12C / 12A\n"
        "Booking: ABC123\n\n"
        "Fictional itinerary for Paperstow demos.\n",
        encoding="utf-8",
    )
    (OUT / "sample_trail.gpx").write_text(
        """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="Paperstow" xmlns="http://www.topografix.com/GPX/1/1">
  <metadata>
    <name>Sample trail</name>
  </metadata>
  <trk>
    <name>Sample trail</name>
    <trkseg>
      <trkpt lat="41.50000" lon="-72.50000">
        <time>2026-06-01T13:10:00Z</time>
        <cmt>battery 84%</cmt>
      </trkpt>
      <trkpt lat="41.50140" lon="-72.49820">
        <time>2026-06-01T14:05:00Z</time>
        <cmt>battery 81%</cmt>
      </trkpt>
      <trkpt lat="41.50310" lon="-72.49640">
        <time>2026-06-01T15:40:00Z</time>
        <cmt>battery 76%</cmt>
      </trkpt>
    </trkseg>
  </trk>
</gpx>
""",
        encoding="utf-8",
    )
    (OUT / "trip_plans.md").write_text(
        "# Lakeside trip plans\n"
        "\n"
        "Simple sample checklist for John Doe and Jane Doe.\n"
        "\n"
        "- [x] Book flights and hotel\n"
        "- [x] Buy travel insurance\n"
        "- [ ] Pack medicines and copies of health records\n"
        "- [ ] Download boarding passes\n"
        "- [ ] Share itinerary with Alex Doe\n"
        "- [ ] Confirm Lakeside Inn late check-in\n"
        "- [ ] Review vaccination record before departure\n",
        encoding="utf-8",
    )

    print(f"Wrote {len(list(OUT.iterdir()))} files to {OUT}")


if __name__ == "__main__":
    main()
