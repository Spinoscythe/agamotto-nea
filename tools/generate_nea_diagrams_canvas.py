"""
Canvas-styled NEA diagrams: flat, print-friendly, plain English.
Strong contrast, readable type, boxes sized to content (not stretched).
"""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parent / "nea_generated"
OUT.mkdir(exist_ok=True)

BG = (255, 255, 255)
INK = (18, 18, 18)
MUTED = (40, 40, 40)
CAPTION = (50, 50, 50)
STROKE = (35, 35, 35)
STROKE_SOFT = (80, 80, 80)
FILL = (242, 242, 242)
FILL_SOFT = (248, 248, 248)
ACCENT = (40, 110, 165)
ACCENT_FILL = (228, 240, 249)


def font(size: int, bold: bool = False):
    candidates = [
        ("C:/Windows/Fonts/segoeui.ttf", "C:/Windows/Fonts/segoeuib.ttf"),
        ("C:/Windows/Fonts/calibri.ttf", "C:/Windows/Fonts/calibrib.ttf"),
        ("C:/Windows/Fonts/arial.ttf", "C:/Windows/Fonts/arialbd.ttf"),
    ]
    for regular, bold_path in candidates:
        path = bold_path if bold else regular
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def new_canvas(w: int, h: int):
    img = Image.new("RGB", (w, h), BG)
    return img, ImageDraw.Draw(img)


def rounded(draw, xy, fill=FILL, outline=STROKE, radius=10, width=2):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def text_center(draw, box, text, fnt, fill=INK, spacing=4):
    x0, y0, x1, y1 = box
    bbox = draw.multiline_textbbox((0, 0), text, font=fnt, align="center", spacing=spacing)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    cx = (x0 + x1 - tw) / 2
    cy = (y0 + y1 - th) / 2
    draw.multiline_text((cx, cy), text, font=fnt, fill=fill, align="center", spacing=spacing)


def text_left(draw, xy, text, fnt, fill=INK, spacing=4):
    draw.multiline_text(xy, text, font=fnt, fill=fill, spacing=spacing)


def line(draw, a, b, fill=STROKE, width=2):
    draw.line([a, b], fill=fill, width=width)


def arrow(draw, a, b, fill=STROKE, width=2, size=9):
    line(draw, a, b, fill=fill, width=width)
    ax, ay = a
    bx, by = b
    ang = math.atan2(by - ay, bx - ax)
    p1 = (bx - size * math.cos(ang - 0.45), by - size * math.sin(ang - 0.45))
    p2 = (bx - size * math.cos(ang + 0.45), by - size * math.sin(ang + 0.45))
    draw.polygon([b, p1, p2], fill=fill)


def save(img: Image.Image, name: str):
    path = OUT / name
    img.save(path, "PNG", optimize=True)
    print("wrote", path.name, f"{img.size[0]}x{img.size[1]}", path.stat().st_size)


def titled(draw, w, title, subtitle=None, y=24):
    text_center(draw, (40, y, w - 40, y + 40), title, font(30, True), INK)
    if subtitle:
        text_center(draw, (40, y + 42, w - 40, y + 74), subtitle, font(17), CAPTION)
        return y + 90
    return y + 56


def flow_box(draw, box, text, accent=False, size=18):
    rounded(
        draw,
        box,
        fill=ACCENT_FILL if accent else FILL,
        outline=ACCENT if accent else STROKE,
        radius=10,
        width=2,
    )
    text_center(draw, box, text, font(size), INK, spacing=5)


def diamond_box(draw, cx, cy, text):
    ww, hh = 300, 130
    pts = [(cx, cy - hh // 2), (cx + ww // 2, cy), (cx, cy + hh // 2), (cx - ww // 2, cy)]
    draw.polygon(pts, fill=FILL, outline=STROKE)
    draw.line(pts + [pts[0]], fill=STROKE, width=2)
    text_center(draw, (cx - 130, cy - 44, cx + 130, cy + 44), text, font(17), INK, spacing=4)


# -------- diagrams --------

def diagram_architecture():
    w, h = 860, 980
    img, d = new_canvas(w, h)
    y = titled(d, w, "Three-tier architecture", "Browser app, Spring Boot API, and MySQL database")

    user = (290, y, 570, y + 64)
    rounded(d, user, fill=ACCENT_FILL, outline=ACCENT, radius=8, width=2)
    text_center(d, user, "User", font(20, True), INK)
    prev = user[3]

    layers = [
        ("User interface", "React and TypeScript single-page app", ACCENT_FILL),
        ("API", "Spring Boot controllers, services, and JWT login", FILL),
        ("Data access", "Spring Data JPA repositories", FILL),
        ("Database", "MySQL relational database", FILL_SOFT),
    ]
    box_w, box_h = 460, 105
    x0 = (w - box_w) // 2
    for name, detail, fill in layers:
        yb = prev + 42
        box = (x0, yb, x0 + box_w, yb + box_h)
        arrow(d, (w // 2, prev + 8), (w // 2, yb - 2), STROKE, 2)
        rounded(d, box, fill=fill, outline=STROKE, radius=10, width=2)
        text_center(d, (x0, yb + 18, x0 + box_w, yb + 52), name, font(22, True), INK)
        text_center(d, (x0, yb + 56, x0 + box_w, yb + 94), detail, font(17), MUTED)
        prev = box[3]

    text_center(
        d, (40, prev + 36, w - 40, prev + 100),
        "The UI talks to the API over HTTPS with JSON.\nMaven builds the Spring Boot app.",
        font(17), CAPTION, spacing=6,
    )
    save(img, "image5.png")


def diagram_hierarchy():
    w, h = 1280, 740
    img, d = new_canvas(w, h)
    titled(d, w, "Module hierarchy")

    root = (500, 100, 780, 170)
    rounded(d, root, fill=ACCENT_FILL, outline=ACCENT, radius=10, width=2)
    text_center(d, root, "Agamotto", font(22, True), INK)

    # Users
    ux, ubw = 60, 300
    up = (ux, 250, ux + ubw, 320)
    line(d, (640, 170), ((ux + ux + ubw) // 2, 250), STROKE, 2)
    rounded(d, up, fill=FILL, outline=STROKE, radius=10, width=2)
    text_center(d, up, "Users", font(19, True), INK)
    for i, kid in enumerate(["Admin", "User\n(for example student)"]):
        kx = ux + 10 + i * 148
        child = (kx, 400, kx + 140, 540)
        line(d, ((ux + ux + ubw) // 2, 320), ((child[0] + child[2]) // 2, 400), STROKE_SOFT, 2)
        rounded(d, child, fill=FILL_SOFT, outline=STROKE_SOFT, radius=8, width=2)
        text_center(d, child, kid, font(16), INK, spacing=3)

    # Schedule planning — 2x2 so labels fit
    sx, sbw = 400, 480
    sp = (sx, 250, sx + sbw, 320)
    line(d, (640, 170), ((sx + sx + sbw) // 2, 250), STROKE, 2)
    rounded(d, sp, fill=FILL, outline=STROKE, radius=10, width=2)
    text_center(d, sp, "Schedule planning", font(19, True), INK)
    kids = ["Project", "Tasks", "Schedule plan", "Notifications"]
    child_boxes = []
    for i, kid in enumerate(kids):
        col, row = i % 2, i // 2
        kx = sx + 24 + col * 228
        ky = 380 + row * 140
        child = (kx, ky, kx + 210, ky + 110)
        child_boxes.append(child)
        rounded(d, child, fill=FILL_SOFT, outline=STROKE_SOFT, radius=8, width=2)
        text_center(d, child, kid, font(17), INK)
    # bus line into the four boxes (not a fake deeper tree)
    mid_x = (sx + sx + sbw) // 2
    bus_y = 350
    line(d, (mid_x, 320), (mid_x, bus_y), STROKE_SOFT, 2)
    line(d, (child_boxes[0][0] + 105, bus_y), (child_boxes[1][0] + 105, bus_y), STROKE_SOFT, 2)
    for child in child_boxes[:2]:
        cx = (child[0] + child[2]) // 2
        line(d, (cx, bus_y), (cx, child[1]), STROKE_SOFT, 2)
    for top, bottom in ((child_boxes[0], child_boxes[2]), (child_boxes[1], child_boxes[3])):
        cx = (top[0] + top[2]) // 2
        line(d, (cx, top[3]), (cx, bottom[1]), STROKE_SOFT, 2)

    # Dashboards
    dx, dbw = 940, 300
    dp = (dx, 250, dx + dbw, 320)
    line(d, (640, 170), ((dx + dx + dbw) // 2, 250), STROKE, 2)
    rounded(d, dp, fill=FILL, outline=STROKE, radius=10, width=2)
    text_center(d, dp, "Dashboards", font(19, True), INK)
    for i, kid in enumerate(["Detailed\nreports", "Summary\nreports"]):
        kx = dx + 8 + i * 150
        child = (kx, 400, kx + 140, 540)
        line(d, ((dx + dx + dbw) // 2, 320), ((child[0] + child[2]) // 2, 400), STROKE_SOFT, 2)
        rounded(d, child, fill=FILL_SOFT, outline=STROKE_SOFT, radius=8, width=2)
        text_center(d, child, kid, font(16), INK, spacing=3)
    save(img, "image6.png")


def diagram_flowchart():
    w, h = 1200, 860
    img, d = new_canvas(w, h)
    titled(d, w, "System flowchart")

    user = (500, 100, 700, 160)
    rounded(d, user, fill=ACCENT_FILL, outline=ACCENT, radius=10, width=2)
    text_center(d, user, "User", font(19, True), INK)

    ui = (380, 210, 820, 310)
    rounded(d, ui, fill=FILL, outline=STROKE, radius=10, width=2)
    text_center(d, ui, "Agamotto user interface\nReact single-page app", font(18, True), INK, spacing=4)
    arrow(d, (600, 160), (600, 210), STROKE, 2)

    procs = [
        (40, "Log in"),
        (310, "Create or update\nproject and tasks"),
        (600, "Request a\nschedule plan"),
        (890, "View reports\nand dashboards"),
    ]
    for x, label in procs:
        box = (x, 380, x + 250, 490)
        rounded(d, box, fill=FILL_SOFT, outline=STROKE_SOFT, radius=10, width=2)
        text_center(d, box, label, font(17), INK, spacing=4)
        arrow(d, (600, 310), ((x + x + 250) // 2, 380), STROKE_SOFT, 2)

    api = (320, 560, 880, 670)
    rounded(d, api, fill=FILL, outline=STROKE, radius=10, width=2)
    text_center(d, api, "API layer\nSpring Boot REST endpoints and services", font(18, True), INK, spacing=4)
    for x, _ in procs:
        arrow(d, ((x + x + 250) // 2, 490), (600, 560), STROKE_SOFT, 2)

    stores = [
        (60, "Users and profiles"),
        (440, "Projects, tasks, and plans"),
        (860, "Notifications"),
    ]
    for x, label in stores:
        box = (x, 740, x + 280, 820)
        rounded(d, box, fill=FILL_SOFT, outline=STROKE_SOFT, radius=10, width=2)
        text_center(d, box, label, font(16), INK)
        arrow(d, (600, 670), ((x + x + 280) // 2, 740), STROKE_SOFT, 2)
    save(img, "image7.png")


def diagram_dfd():
    w, h = 1240, 920
    img, d = new_canvas(w, h)
    titled(d, w, "Data flow diagram", "User, processes, and data stores")

    user = (40, 170, 190, 250)
    rounded(d, user, fill=ACCENT_FILL, outline=ACCENT, radius=10, width=2)
    text_center(d, user, "User", font(19, True), INK)

    processes = [
        (250, 110, "1. Manage users"),
        (250, 240, "2. Manage profile"),
        (250, 370, "3. Manage projects"),
        (250, 500, "4. Manage tasks"),
        (600, 370, "5. Build schedule\nplan"),
        (920, 250, "6. Build\ndashboard"),
        (920, 500, "7. Send\nnotifications"),
    ]
    for x, y, label in processes:
        box = (x, y, x + 230, y + 95)
        rounded(d, box, fill=FILL, outline=STROKE, radius=10, width=2)
        text_center(d, box, label, font(17), INK, spacing=3)

    stores = [
        (510, 120, "Users store"),
        (510, 250, "Profiles store"),
        (510, 380, "Projects store"),
        (510, 510, "Tasks store"),
        (600, 660, "Schedule plans store"),
    ]
    for x, y, label in stores:
        box = (x, y, x + 240, y + 75)
        rounded(d, box, fill=FILL_SOFT, outline=STROKE_SOFT, radius=8, width=2)
        text_center(d, box, label, font(17, True), INK)

    arrow(d, (190, 200), (250, 155), STROKE, 2)
    arrow(d, (480, 155), (510, 155), STROKE_SOFT, 2)
    arrow(d, (190, 210), (250, 285), STROKE_SOFT, 2)
    arrow(d, (480, 285), (510, 285), STROKE_SOFT, 2)
    arrow(d, (190, 220), (250, 415), STROKE_SOFT, 2)
    arrow(d, (480, 415), (510, 415), STROKE_SOFT, 2)
    arrow(d, (190, 230), (250, 545), STROKE_SOFT, 2)
    arrow(d, (480, 545), (510, 545), STROKE_SOFT, 2)
    arrow(d, (730, 415), (600, 415), STROKE, 2)
    arrow(d, (715, 465), (720, 660), STROKE_SOFT, 2)
    arrow(d, (830, 415), (920, 295), STROKE_SOFT, 2)
    arrow(d, (830, 415), (920, 545), STROKE_SOFT, 2)
    arrow(d, (920, 595), (190, 250), STROKE_SOFT, 2)

    text_center(
        d, (40, 850, w - 40, 900),
        "Arrows show data moving between the user, processes, and stores.",
        font(17), CAPTION,
    )
    save(img, "image8.png")


def class_card(draw, x, y, w, h, title, lines, accent=False):
    rounded(
        draw,
        (x, y, x + w, y + h),
        fill=ACCENT_FILL if accent else FILL,
        outline=ACCENT if accent else STROKE,
        radius=10,
        width=2,
    )
    draw.line((x + 14, y + 44, x + w - 14, y + 44), fill=STROKE_SOFT, width=1)
    text_left(draw, (x + 16, y + 12), title, font(18, True), INK)
    text_left(draw, (x + 16, y + 54), "\n".join(lines), font(16), MUTED, spacing=5)


def diagram_class():
    w, h = 1360, 1000
    img, d = new_canvas(w, h)
    titled(d, w, "Class diagram", "Main Java classes used by the Spring Boot backend")

    class_card(d, 30, 110, 280, 210, "User",
               ["id", "full name, email", "password hash", "", "register, login"], False)
    class_card(d, 340, 110, 300, 210, "User profile",
               ["preferred start and end", "include weekends", "priority weights", "", "update preferences"], False)
    class_card(d, 670, 110, 280, 210, "Project",
               ["name", "start and end date", "estimated effort hours", "", "create project"], False)
    class_card(d, 980, 110, 340, 210, "Task",
               ["title, priority", "complexity, deadline", "estimated hours", "status", "create task"], False)

    class_card(d, 140, 390, 400, 240, "Scheduler engine (service)",
               ["scoring strategy", "best-fit selector", "greedy placer", "",
                "select mode, score task", "run serenity, run crunch"], True)
    class_card(d, 580, 390, 320, 240, "Schedule plan",
               ["mode", "status", "explanation summary", "", "generate"], False)
    class_card(d, 940, 390, 340, 240, "Schedule block",
               ["start and end time", "decision", "reason", "", "calculate duration"], False)

    class_card(d, 60, 700, 280, 190, "Task history",
               ["changed at", "change type", "", "record change"], False)
    class_card(d, 380, 700, 280, 190, "Notification",
               ["channel", "deadline", "", "send reminder"], False)
    class_card(d, 700, 700, 300, 190, "Dashboard report",
               ["scheduled count", "delayed count", "excluded count", "", "create summary"], False)
    class_card(d, 1040, 700, 280, 190, "Greedy placer",
               ["place tasks on days", "priority queue", "max session 2 hours", ""], True)

    text_center(
        d, (40, 930, w - 40, 980),
        "Links between classes are left out to keep the diagram clear. IDs are UUID strings.",
        font(17), CAPTION,
    )
    save(img, "image9.png")


def er_card(draw, x, y, w, title, rows):
    h = 48 + 28 * len(rows)
    rounded(draw, (x, y, x + w, y + h), fill=FILL, outline=STROKE, radius=10, width=2)
    text_left(draw, (x + 14, y + 12), title, font(18, True), INK)
    draw.line((x + 12, y + 46, x + w - 12, y + 46), fill=STROKE_SOFT, width=1)
    text_left(draw, (x + 14, y + 56), "\n".join(rows), font(16), MUTED, spacing=5)
    return h


def diagram_erd():
    w, h = 1360, 1040
    img, d = new_canvas(w, h)
    titled(d, w, "Entity relationship diagram", "Tables stored in MySQL through Spring Data JPA")

    er_card(d, 30, 110, 360, "users",
            ["Primary key: user id", "full name", "email", "password hash"])
    er_card(d, 460, 110, 400, "user profiles",
            ["Primary key: profile id", "Foreign key: user id", "preferred start and end", "include weekends", "priority weights"])
    er_card(d, 930, 110, 390, "projects",
            ["Primary key: id", "Foreign key: user id", "name", "start and end date", "estimated effort hours"])

    er_card(d, 30, 390, 400, "tasks",
            ["Primary key: id", "Foreign key: project id", "title and category", "priority and complexity", "hours, deadline, status"])
    er_card(d, 460, 390, 400, "schedule plans",
            ["Primary key: id", "Foreign key: project id", "mode and status", "generated at", "explanation summary"])
    er_card(d, 930, 390, 390, "schedule blocks",
            ["Primary key: id", "Foreign key: schedule id", "Foreign key: task id", "start and end time", "decision and reason"])

    er_card(d, 30, 720, 380, "task history",
            ["Primary key: id", "Foreign key: task id", "Foreign key: changed by", "changed at and change type"])
    er_card(d, 460, 720, 380, "notifications",
            ["Primary key: id", "Foreign key: user id", "Foreign key: task id", "channel and deadline"])
    er_card(d, 930, 720, 390, "dashboard reports",
            ["Primary key: id", "Foreign key: user id", "period", "scheduled, delayed, excluded"])

    text_center(
        d, (40, 960, w - 40, 1010),
        "One user has one profile. Elsewhere the links are one to many. Primary keys are UUIDs.",
        font(17), CAPTION,
    )
    save(img, "image10.png")


def diagram_generate_schedule():
    w, h = 980, 1000
    img, d = new_canvas(w, h)
    titled(d, w, "Schedule generation", "How the scheduler chooses Serenity or Crunch")

    boxes = [
        ((190, 110, 790, 200), "Start with tasks, dates, and the user profile", True),
        ((170, 255, 810, 345), "Collect tasks and profile preferences", False),
        ((150, 400, 830, 490), "Add up the hours needed for every task", False),
    ]
    for box, text, acc in boxes:
        flow_box(d, box, text, acc, size=18)
    arrow(d, (490, 200), (490, 255), STROKE, 2)
    arrow(d, (490, 345), (490, 400), STROKE, 2)
    arrow(d, (490, 490), (490, 545), STROKE, 2)

    diamond_box(d, 490, 610, "Do the hours fit\nin the available time?")
    flow_box(d, (40, 740, 400, 850), "Yes: Serenity mode\nPlace every task", False, size=17)
    flow_box(d, (580, 740, 940, 850), "No: Crunch mode\nSort by deadline, trim, then place", False, size=17)
    arrow(d, (370, 640), (220, 740), STROKE, 2)
    arrow(d, (610, 640), (760, 740), STROKE, 2)
    text_left(d, (230, 700), "yes", font(16, True), MUTED)
    text_left(d, (780, 700), "no", font(16, True), MUTED)

    final = (170, 900, 810, 980)
    line(d, (220, 850), (220, 890), STROKE, 2)
    line(d, (760, 850), (760, 890), STROKE, 2)
    line(d, (220, 890), (760, 890), STROKE, 2)
    arrow(d, (490, 890), (490, 900), STROKE, 2)
    flow_box(d, final, "Save the schedule plan and its time blocks", False, size=18)
    save(img, "image16.png")


def diagram_serenity():
    # Single column — large text, no wide empty boxes
    w, h = 920, 1080
    img, d = new_canvas(w, h)
    titled(d, w, "Serenity mode", "Used when there is enough time for every task")

    steps = [
        "Start with the task list, dates, and profile",
        "Score each task and put higher scores first",
        "Track how many hours each task still needs",
        "Walk through each working day in the range",
        "Start each day at the preferred start time",
        "Place as many hours as will fit that day",
        "Mark placed hours as scheduled; keep leftovers",
        "Hours still left at the end are marked delayed",
    ]
    y = 120
    for i, step in enumerate(steps):
        box = (70, y, w - 70, y + 88)
        flow_box(d, box, f"{i + 1}.  {step}", accent=(i == 0), size=18)
        if i:
            arrow(d, (w // 2, y - 20), (w // 2, y), STROKE, 2)
        y += 108
    text_center(d, (40, y + 4, w - 40, y + 44), "This is what the greedy placer does day by day.", font(17), CAPTION)
    save(img, "image17.png")


def diagram_crunch():
    w, h = 920, 980
    img, d = new_canvas(w, h)
    titled(d, w, "Crunch mode", "Used when the work does not fit in the available hours")

    steps = [
        "Start with the task list, dates, and profile",
        "Sort tasks by deadline, earliest first",
        "Work out how many hours are available",
        "Drop lowest-priority or shortest tasks until the rest fit",
        "Place the remaining tasks with the greedy placer",
        "Mark trimmed tasks as excluded",
        "Return the Crunch schedule and its blocks",
    ]
    y = 120
    for i, step in enumerate(steps):
        box_h = 100 if len(step) > 48 else 88
        box = (70, y, w - 70, y + box_h)
        flow_box(d, box, f"{i + 1}.  {step}", accent=(i == 0), size=18)
        if i:
            arrow(d, (w // 2, y - 20), (w // 2, y), STROKE, 2)
        y += box_h + 22
    text_center(
        d, (40, y + 8, w - 40, y + 50),
        "Best-fit trim is the automatic cut-down in this build.",
        font(17), CAPTION,
    )
    save(img, "image18.png")


def diagram_iterations():
    w, h = 1280, 540
    img, d = new_canvas(w, h)
    titled(d, w, "Iterative development", "Each iteration goes through design, develop, then test")

    n = 6
    margin = 40
    gap = 18
    box_w = (w - 2 * margin - (n - 1) * gap) // n
    y = 150
    for i in range(n):
        x = margin + i * (box_w + gap)
        box = (x, y, x + box_w, y + 250)
        rounded(d, box, fill=FILL if i % 2 == 0 else FILL_SOFT, outline=STROKE, radius=10, width=2)
        text_center(d, (x, y + 24, x + box_w, y + 70), f"Iteration {i + 1}", font(18, True), INK)
        d.line((x + 16, y + 82, x + box_w - 16, y + 82), fill=STROKE_SOFT, width=1)
        text_center(d, (x, y + 100, x + box_w, y + 230), "Design\nDevelop\nTest", font(18), MUTED, spacing=10)
        if i < n - 1:
            arrow(d, (x + box_w + 2, y + 125), (x + box_w + gap - 2, y + 125), STROKE, 2, size=8)

    text_center(
        d, (50, 440, w - 50, 510),
        "Write a short test plan before development in each iteration.",
        font(18), CAPTION,
    )
    save(img, "iterations.png")


if __name__ == "__main__":
    diagram_architecture()
    diagram_hierarchy()
    diagram_flowchart()
    diagram_dfd()
    diagram_class()
    diagram_erd()
    diagram_generate_schedule()
    diagram_serenity()
    diagram_crunch()
    diagram_iterations()
    print("done")
