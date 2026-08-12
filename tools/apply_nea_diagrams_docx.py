"""Replace NEA diagram media and fix Word drawing extents to match PNG aspect ratios."""
from __future__ import annotations

import re
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

GEN = Path(__file__).resolve().parent / "nea_generated"
DESKTOP = Path(r"c:\Users\Nekretaur\Desktop")
DOCS = Path(__file__).resolve().parents[1] / "docs"

REPLACE = {
    "word/media/image5.png": GEN / "image5.png",
    "word/media/image6.png": GEN / "image6.png",
    "word/media/image7.png": GEN / "image7.png",
    "word/media/image8.png": GEN / "image8.png",
    "word/media/image9.png": GEN / "image9.png",
    "word/media/image10.png": GEN / "image10.png",
    "word/media/image16.png": GEN / "image16.png",
    "word/media/image17.png": GEN / "image17.png",
    "word/media/image18.png": GEN / "image18.png",
    "word/media/image19.png": GEN / "iterations.png",
}

EMU_PER_INCH = 914400
MAX_HEIGHT_IN = 5.8


def parse_rid_map(rels_xml: str) -> dict[str, str]:
    rid_map: dict[str, str] = {}
    for m in re.finditer(r"<Relationship\b[^>]*>", rels_xml):
        tag = m.group(0)
        mid = re.search(r'\bId="(rId\d+)"', tag)
        tgt = re.search(r'\bTarget="media/([^"]+)"', tag)
        if mid and tgt:
            rid_map[mid.group(1)] = tgt.group(1)
    return rid_map


def fix_extents(document_xml: str, rid_map: dict[str, str], media_bytes: dict[str, bytes]) -> str:
    """Keep each image width; set height from PNG aspect. Avoid XML pretty-print."""

    def aspect_for(filename: str) -> float | None:
        key = f"word/media/{filename}"
        data = media_bytes.get(key)
        if not data:
            return None
        im = Image.open(BytesIO(data))
        return im.size[0] / im.size[1]

    # Walk each drawing block that contains a blip embed
    pattern = re.compile(
        r"(<w:drawing\b[\s\S]*?</w:drawing>)",
        re.MULTILINE,
    )

    def rewrite_drawing(match: re.Match[str]) -> str:
        block = match.group(1)
        embed = re.search(r'r:embed="(rId\d+)"', block)
        if not embed:
            return block
        target = rid_map.get(embed.group(1))
        if not target or f"word/media/{target}" not in REPLACE:
            return block
        aspect = aspect_for(target)
        if not aspect:
            return block

        # Prefer a readable page width; old slots were often too narrow/short.
        preferred_w = {
            "image5.png": 4.2,
            "image6.png": 5.8,
            "image7.png": 5.4,
            "image8.png": 5.4,
            "image9.png": 5.8,
            "image10.png": 5.8,
            "image16.png": 5.2,
            "image17.png": 4.3,
            "image18.png": 4.3,
            "image19.png": 6.0,
        }.get(target, 5.0)
        cx = int(preferred_w * EMU_PER_INCH)
        cy = int(round(cx / aspect))
        max_cy = int(MAX_HEIGHT_IN * EMU_PER_INCH)
        if cy > max_cy:
            cy = max_cy
            cx = int(round(cy * aspect))

        def fix_ext(em: re.Match[str]) -> str:
            tag = em.group(0)
            cx_m = re.search(r'\bcx="(\d+)"', tag)
            cy_m = re.search(r'\bcy="(\d+)"', tag)
            if not cx_m or not cy_m:
                return tag
            new = re.sub(r'\bcx="\d+"', f'cx="{cx}"', tag, count=1)
            new = re.sub(r'\bcy="\d+"', f'cy="{cy}"', new, count=1)
            print(
                f"  {target}: {int(cx_m.group(1))/EMU_PER_INCH:.2f}x{int(cy_m.group(1))/EMU_PER_INCH:.2f}"
                f" -> {cx/EMU_PER_INCH:.2f}x{cy/EMU_PER_INCH:.2f}in"
            )
            return new

        # wp:extent and a:ext both carry cx/cy
        block = re.sub(r"<wp:extent\b[^/]*/>", fix_ext, block)
        block = re.sub(r"<a:ext\b[^/]*/>", fix_ext, block)
        return block

    return pattern.sub(rewrite_drawing, document_xml)


def rebuild(src: Path, dest: Path) -> None:
    with zipfile.ZipFile(src, "r") as zin:
        items = zin.infolist()
        raw = {i.filename: zin.read(i.filename) for i in items}

    for path, png in REPLACE.items():
        if path in raw and png.exists():
            raw[path] = png.read_bytes()
            print("replaced", path)

    rid_map = parse_rid_map(raw["word/_rels/document.xml.rels"].decode("utf-8"))
    xml = raw["word/document.xml"].decode("utf-8")
    xml = fix_extents(xml, rid_map, raw)
    raw["word/document.xml"] = xml.encode("utf-8")

    dest.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(dest, "w", compression=zipfile.ZIP_DEFLATED) as zout:
        for info in items:
            zout.writestr(info, raw[info.filename])
    print("wrote", dest)


def main():
    sources = [
        DESKTOP / "Agamotto_OCR_NEA_Java_Spring_canvas_v2.docx",
        DESKTOP / "Agamotto_OCR_NEA_Java_Spring_design_testing.docx",
        DESKTOP / "Agamotto_OCR_NEA_Java_Spring.docx",
        DESKTOP / "Agamotto_OCR_NEA_Java_Spring_canvas.docx",
    ]
    src = next(p for p in sources if p.exists())
    print("source", src.name)

    tmp = DESKTOP / "_nea_canvas_build.docx"
    rebuild(src, tmp)

    for out in [
        DESKTOP / "Agamotto_OCR_NEA_Java_Spring_canvas_v2.docx",
        DOCS / "Agamotto_OCR_NEA_Java_Spring_canvas.docx",
    ]:
        try:
            out.write_bytes(tmp.read_bytes())
            print("updated", out)
        except PermissionError:
            alt = out.with_name(out.stem + "_readable.docx")
            alt.write_bytes(tmp.read_bytes())
            print("locked, wrote", alt)

    try:
        tmp.unlink()
    except OSError:
        pass


if __name__ == "__main__":
    main()
