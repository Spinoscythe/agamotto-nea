"""Generate Agamotto NEA design/algorithm diagram PNGs (print-friendly)."""
from pathlib import Path
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Circle, Polygon, Rectangle
import numpy as np

OUT = Path(__file__).resolve().parent / "nea_generated"
OUT.mkdir(exist_ok=True)

# Flat, canvas-like palette (light paper, dark ink)
BG = "#FAFAFA"
INK = "#1A1A1A"
MUTED = "#555555"
FILL = "#F0F0F0"
STROKE = "#2A2A2A"
ACCENT = "#2E76AB"
ACCENT_FILL = "#E8F1F8"
GREEN = "#1F8A65"
GREEN_FILL = "#E8F5F0"
ORANGE = "#C06028"
ORANGE_FILL = "#F8EEE8"
PINK = "#B8448B"
PINK_FILL = "#F7EAF3"
BLUE = "#3685BF"
BLUE_FILL = "#EAF2F8"


def new_fig(w=10, h=7):
    fig, ax = plt.subplots(figsize=(w, h), dpi=160)
    fig.patch.set_facecolor(BG)
    ax.set_facecolor(BG)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis("off")
    return fig, ax


def rounded(ax, x, y, w, h, text, fc=FILL, ec=STROKE, fontsize=9, weight="normal", tc=INK):
    box = FancyBboxPatch(
        (x, y), w, h,
        boxstyle="round,pad=0.02,rounding_size=1.2",
        linewidth=1.2, edgecolor=ec, facecolor=fc,
    )
    ax.add_patch(box)
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center",
            fontsize=fontsize, color=tc, weight=weight, wrap=True)
    return box


def arrow(ax, x1, y1, x2, y2, color=STROKE, style="-|>"):
    ax.annotate(
        "", xy=(x2, y2), xytext=(x1, y1),
        arrowprops=dict(arrowstyle=style, color=color, lw=1.3),
    )


def diamond(ax, cx, cy, w, h, text, fontsize=8):
    pts = np.array([[cx, cy + h / 2], [cx + w / 2, cy], [cx, cy - h / 2], [cx - w / 2, cy]])
    poly = Polygon(pts, closed=True, facecolor=ACCENT_FILL, edgecolor=STROKE, lw=1.2)
    ax.add_patch(poly)
    ax.text(cx, cy, text, ha="center", va="center", fontsize=fontsize, color=INK)


def save(fig, name):
    path = OUT / name
    fig.savefig(path, bbox_inches="tight", facecolor=fig.get_facecolor(), pad_inches=0.25)
    plt.close(fig)
    print("wrote", path.name, path.stat().st_size)


# --- image5: three-tier architecture ---
def diagram_architecture():
    fig, ax = new_fig(9, 8)
    ax.text(50, 96, "Agamotto — Three-tier architecture", ha="center", fontsize=13, weight="bold", color=INK)
    ax.text(50, 91, "React/TypeScript UI  ·  Java 21 / Spring Boot API  ·  H2 + Spring Data JPA",
            ha="center", fontsize=8, color=MUTED)

    # Actor
    ax.add_patch(Circle((50, 82), 2.2, facecolor=FILL, edgecolor=STROKE, lw=1.2))
    ax.plot([50, 50], [79.8, 76], color=STROKE, lw=1.2)
    ax.plot([47.5, 52.5], [78, 78], color=STROKE, lw=1.2)
    ax.plot([50, 48], [76, 73], color=STROKE, lw=1.2)
    ax.plot([50, 52], [76, 73], color=STROKE, lw=1.2)
    ax.text(50, 70.5, "Actor", ha="center", fontsize=9, color=INK)

    rounded(ax, 28, 58, 44, 9, "UI Layer\nReact + TypeScript (Vite SPA)", fc=ACCENT_FILL, fontsize=10, weight="semibold")
    arrow(ax, 50, 70, 50, 67.2)
    ax.text(62, 68.5, "HTTPS / JSON", fontsize=7, color=MUTED)

    rounded(ax, 28, 42, 44, 10, "API Layer\nJava 21 + Spring Boot\nREST Controllers · Services · JWT",
            fc=GREEN_FILL, fontsize=9, weight="semibold")
    arrow(ax, 50, 58, 50, 52.2)
    ax.text(66, 55, "Request / Response", fontsize=7, color=MUTED)

    rounded(ax, 28, 26, 44, 9, "Database Layer\nSpring Data JPA repositories",
            fc=ORANGE_FILL, fontsize=9, weight="semibold")
    arrow(ax, 50, 42, 50, 35.2)

    rounded(ax, 32, 10, 36, 9, "H2 Database\nfile-based relational store",
            fc=FILL, fontsize=9, weight="semibold")
    arrow(ax, 50, 26, 50, 19.2)
    ax.text(50, 4, "Everything above the database is built with Gradle as a Spring Boot application (Java 21).",
            ha="center", fontsize=7.5, color=MUTED, style="italic")
    save(fig, "image5.png")


# --- image6: hierarchy ---
def diagram_hierarchy():
    fig, ax = new_fig(11, 6.5)
    ax.text(50, 95, "Agamotto — Module hierarchy", ha="center", fontsize=13, weight="bold", color=INK)

    rounded(ax, 38, 78, 24, 8, "Agamotto", fc=ACCENT_FILL, fontsize=11, weight="bold")
    # branches
    for x in (18, 50, 82):
        ax.plot([50, x], [78, 68], color=STROKE, lw=1.1)
        ax.plot([x, x], [68, 64], color=STROKE, lw=1.1)

    heads = [
        (6, "Users", ORANGE_FILL),
        (38, "Schedule Planning", GREEN_FILL),
        (70, "Dashboards", BLUE_FILL),
    ]
    children = {
        0: ["Admin", "User\n(e.g. student)"],
        1: ["Project", "Tasks", "Schedule Plan", "Notifications"],
        2: ["Detailed review\n/ reports", "Summary review\n/ reports"],
    }
    for i, (x, title, fc) in enumerate(heads):
        rounded(ax, x, 54, 24, 8, title, fc=fc, fontsize=9, weight="semibold")
        kids = children[i]
        span = 24
        step = span / max(len(kids), 1)
        for j, kid in enumerate(kids):
            kx = x + step * j + step / 2 - 5
            ax.plot([x + 12, kx + 5], [54, 46], color=STROKE, lw=0.9)
            rounded(ax, kx, 28 if "\n" in kid else 32, 10, 12 if "\n" in kid else 8,
                    kid, fc=FILL, fontsize=7.5)
    save(fig, "image6.png")


# --- image7: system flowchart ---
def diagram_flowchart():
    fig, ax = new_fig(11, 8)
    ax.text(50, 96, "Agamotto — System flowchart", ha="center", fontsize=13, weight="bold", color=INK)

    rounded(ax, 42, 86, 16, 6, "User", fc=ACCENT_FILL, fontsize=9, weight="semibold")
    rounded(ax, 36, 72, 28, 7, "Agamotto UI\n(React SPA)", fc=ACCENT_FILL, fontsize=9, weight="semibold")
    arrow(ax, 50, 86, 50, 79.2)

    # middle processes
    procs = [
        (8, 54, "Login"),
        (30, 54, "Create / Update\nProject & Tasks"),
        (55, 54, "Request\nschedule plan"),
        (78, 54, "View reports\n/ Dashboards"),
    ]
    for x, y, t in procs:
        rounded(ax, x, y, 18, 9, t, fc=FILL, fontsize=8)
        arrow(ax, 50, 72, x + 9, 63.2)

    rounded(ax, 36, 34, 28, 8, "API Layer\nSpring Boot REST + Services", fc=GREEN_FILL, fontsize=9, weight="semibold")
    for x, _, _ in procs:
        arrow(ax, x + 9, 54, 50, 42.2)

    rounded(ax, 8, 16, 22, 8, "Login DB\n(users / profiles)", fc=ORANGE_FILL, fontsize=8)
    rounded(ax, 38, 16, 24, 8, "Schedule Planning DB\n(projects, tasks, plans)", fc=ORANGE_FILL, fontsize=8)
    rounded(ax, 72, 16, 20, 8, "Notify users\n(@Scheduled)", fc=PINK_FILL, fontsize=8)
    arrow(ax, 42, 34, 19, 24.2)
    arrow(ax, 50, 34, 50, 24.2)
    arrow(ax, 58, 34, 82, 24.2)
    arrow(ax, 82, 16, 58, 86)  # notify back toward user area - simplify
    ax.annotate("", xy=(50, 86), xytext=(82, 24),
                arrowprops=dict(arrowstyle="-|>", color=MUTED, lw=1.0,
                                connectionstyle="arc3,rad=-0.35"))
    ax.text(88, 50, "reminders", fontsize=7, color=MUTED, rotation=90)
    save(fig, "image7.png")


# --- image8: DFD ---
def diagram_dfd():
    fig, ax = new_fig(11, 8)
    ax.text(50, 96, "Agamotto — Level-0 / Level-1 dataflow (DFD)", ha="center", fontsize=12, weight="bold", color=INK)

    # External entity
    rounded(ax, 2, 78, 14, 8, "User", fc=ACCENT_FILL, fontsize=9, weight="bold")

    processes = [
        (22, 82, "1.0\nCreate/Update\nUser"),
        (22, 64, "2.0\nCreate/Update\nUser Profile"),
        (22, 46, "3.0\nCreate/Update\nProject"),
        (22, 28, "4.0\nCreate/Update\nTasks"),
        (55, 46, "5.0\nCreate\nSchedule Plan"),
        (78, 62, "6.0\nView reports\n/ Dashboard"),
        (78, 30, "7.0\nNotify User"),
    ]
    for x, y, t in processes:
        # DFD process = rounded rect with vertical bars feel
        rounded(ax, x, y, 18, 12, t, fc=FILL, fontsize=7.5)

    stores = [
        (42, 84, "D1 User"),
        (42, 66, "D2 User profile"),
        (42, 48, "D3 Project"),
        (42, 30, "D4 Tasks"),
        (55, 28, "D5 Schedule Plan"),
    ]
    for x, y, t in stores:
        rounded(ax, x, y, 16, 7, t, fc=ORANGE_FILL, fontsize=8, weight="semibold")

    # wires
    arrow(ax, 16, 82, 22, 88)
    arrow(ax, 40, 88, 42, 87.5)
    arrow(ax, 16, 82, 22, 70)
    arrow(ax, 40, 70, 42, 69.5)
    arrow(ax, 16, 82, 22, 52)
    arrow(ax, 40, 52, 42, 51.5)
    arrow(ax, 16, 82, 22, 34)
    arrow(ax, 40, 34, 42, 33.5)
    arrow(ax, 50, 33.5, 55, 46)
    arrow(ax, 64, 46, 55, 35)
    arrow(ax, 73, 46, 78, 68)
    arrow(ax, 73, 46, 78, 36)
    arrow(ax, 78, 30, 16, 78)
    ax.text(50, 8, "Open arrows = data flows · D# boxes = data stores · numbered boxes = processes",
            ha="center", fontsize=7.5, color=MUTED, style="italic")
    save(fig, "image8.png")


# --- image9: class diagram (simplified but accurate) ---
def class_box(ax, x, y, w, h, title, fields, methods, header_fc):
    # header
    ax.add_patch(Rectangle((x, y + h - 8), w, 8, facecolor=header_fc, edgecolor=STROKE, lw=1.0))
    ax.add_patch(Rectangle((x, y), w, h - 8, facecolor=BG, edgecolor=STROKE, lw=1.0))
    ax.text(x + w / 2, y + h - 4, title, ha="center", va="center", fontsize=7.5, weight="bold", color=INK)
    body = "\n".join(fields + ([""] if methods else []) + methods)
    ax.text(x + 1.2, y + h - 10, body, ha="left", va="top", fontsize=5.8, color=INK,
            family="monospace")


def diagram_class():
    fig, ax = new_fig(13, 9)
    ax.text(50, 97, "Agamotto — UML class diagram (Java / Spring Boot)", ha="center",
            fontsize=12, weight="bold", color=INK)

    class_box(ax, 2, 72, 22, 22, "User",
              ["+ id: String", "+ fullName: String", "+ email: String", "+ passwordHash: String"],
              ["+ register()", "+ login(): boolean"], ORANGE_FILL)
    class_box(ax, 28, 74, 24, 20, "UserProfile",
              ["+ preferredStart: LocalTime", "+ preferredEnd: LocalTime", "+ includeWeekends: boolean",
               "+ weightPriority/Urgency/Duration"],
              ["+ updatePreferences()"], ORANGE_FILL)
    class_box(ax, 56, 74, 20, 20, "Project",
              ["+ name: String", "+ startDate/endDate", "+ estimatedEffortHours"],
              ["+ createProject()"], GREEN_FILL)
    class_box(ax, 78, 72, 20, 22, "Task",
              ["+ title, priority", "+ complexity, deadline", "+ estDurationHours", "+ status: TaskStatus"],
              ["+ createTask()"], FILL)

    class_box(ax, 28, 40, 26, 24, "«service» SchedulerEngine",
              ["uses ScoringStrategy", "uses BestFitSelector", "uses GreedyPlacer"],
              ["+ selectMode()", "+ runSerenity()", "+ runCrunch()", "+ scoreTask()"], PINK_FILL)
    class_box(ax, 58, 42, 22, 22, "SchedulePlan",
              ["+ mode: ScheduleMode", "+ status: PlanStatus", "+ explanationSummary"],
              ["+ generate()"], PINK_FILL)
    class_box(ax, 82, 42, 16, 22, "ScheduleBlock",
              ["+ start/endTime", "+ decision", "+ reason"],
              ["+ calculateDuration()"], PINK_FILL)

    class_box(ax, 2, 12, 20, 18, "TaskHistory",
              ["+ changedAt", "+ changeType"], ["+ recordChange()"], BLUE_FILL)
    class_box(ax, 28, 12, 20, 18, "Notification",
              ["+ channel", "+ deadline"], ["+ sendReminder()"], GREEN_FILL)
    class_box(ax, 54, 12, 22, 18, "DashboardReport",
              ["+ scheduled/delayed/", "  excluded counts"], ["+ createSummary()"], GREEN_FILL)
    class_box(ax, 80, 12, 18, 18, "GreedyPlacer",
              ["+ place(...)", "PriorityQueue"], ["session split ≥4"], ACCENT_FILL)

    # simple association lines
    for a, b in [((13, 72), (40, 74)), ((50, 74), (66, 74)), ((76, 74), (88, 72)),
                 ((41, 40), (69, 42)), ((80, 42), (90, 42)), ((41, 40), (89, 30))]:
        ax.annotate("", xy=b, xytext=a, arrowprops=dict(arrowstyle="-|>", color=MUTED, lw=0.9))

    ax.text(50, 2, "Solid arrows = associations/dependencies · ids are String UUID (VARCHAR 36)",
            ha="center", fontsize=7.5, color=MUTED, style="italic")
    save(fig, "image9.png")


# --- image10: ERD ---
def er_table(ax, x, y, w, title, rows, header_fc=ACCENT_FILL):
    row_h = 3.2
    h = 5 + row_h * len(rows)
    ax.add_patch(Rectangle((x, y + h - 5), w, 5, facecolor=header_fc, edgecolor=STROKE, lw=1))
    ax.text(x + w / 2, y + h - 2.5, title, ha="center", va="center", fontsize=8, weight="bold")
    ax.add_patch(Rectangle((x, y), w, h - 5, facecolor=BG, edgecolor=STROKE, lw=1))
    for i, r in enumerate(rows):
        ax.text(x + 1, y + h - 5 - (i + 0.7) * row_h, r, ha="left", va="center",
                fontsize=5.6, family="monospace", color=INK)
    return h


def diagram_erd():
    fig, ax = new_fig(13, 9.5)
    ax.text(50, 97, "Agamotto — Entity relationship diagram (H2 / JPA)", ha="center",
            fontsize=12, weight="bold", color=INK)

    er_table(ax, 2, 70, 28, "users",
             ["PK user_id VARCHAR(36)", "full_name VARCHAR(120)", "email VARCHAR(150)", "password_hash VARCHAR(255)"])
    er_table(ax, 36, 72, 30, "user_profiles",
             ["PK profile_id", "FK user_id", "preferred_start/end TIME", "include_weekends BOOLEAN",
              "weight_priority/urgency/duration"], ORANGE_FILL)
    er_table(ax, 70, 70, 28, "projects",
             ["PK id", "FK user_id (owner)", "name VARCHAR(200)", "start_date / end_date",
              "estimated_effort_hours"], GREEN_FILL)

    er_table(ax, 2, 38, 30, "tasks",
             ["PK id", "FK project_id", "title, category", "priority, complexity",
              "est/corrected hours", "deadline, status"], FILL)
    er_table(ax, 36, 42, 30, "schedule_plans",
             ["PK id", "FK project_id", "mode VARCHAR(20)", "status, generated_at",
              "explanation_summary"], PINK_FILL)
    er_table(ax, 70, 40, 28, "schedule_blocks",
             ["PK id", "FK schedule_id", "FK task_id", "start_time / end_time",
              "decision, reason"], PINK_FILL)

    er_table(ax, 2, 8, 28, "task_history",
             ["PK id", "FK task_id", "FK changed_by_user_id", "changed_at, change_type"], BLUE_FILL)
    er_table(ax, 36, 10, 28, "notifications",
             ["PK id", "FK user_id", "FK task_id", "channel, deadline"], GREEN_FILL)
    er_table(ax, 70, 12, 28, "dashboard_reports",
             ["PK id", "FK user_id", "period", "scheduled/delayed/excluded"], GREEN_FILL)

    # relationship labels
    ax.text(33, 82, "1 — 1", fontsize=7, color=MUTED)
    ax.text(66, 82, "1 — *", fontsize=7, color=MUTED)
    ax.text(33, 55, "1 — *", fontsize=7, color=MUTED)
    ax.text(66, 55, "1 — *", fontsize=7, color=MUTED)
    ax.text(50, 2, "All PKs are application-generated UUID strings (VARCHAR 36) · schema targets 3NF",
            ha="center", fontsize=7.5, color=MUTED, style="italic")
    save(fig, "image10.png")


# --- image16: generate schedule ---
def diagram_generate_schedule():
    fig, ax = new_fig(9, 9)
    ax.text(50, 96, "Schedule generation — mode selection", ha="center", fontsize=12, weight="bold")
    rounded(ax, 30, 86, 40, 6, "Generating Schedule", fc=ACCENT_FILL, fontsize=10, weight="bold")
    rounded(ax, 28, 74, 44, 7, "Collect tasks + UserProfile preferences", fc=FILL, fontsize=9)
    arrow(ax, 50, 86, 50, 81.2)
    rounded(ax, 26, 62, 48, 7, "totalHours = Σ effectiveDurationHours(task)", fc=FILL, fontsize=9)
    arrow(ax, 50, 74, 50, 69.2)
    diamond(ax, 50, 48, 36, 14, "totalHours ≤\navailableHours + ε ?")
    arrow(ax, 50, 62, 50, 55.2)
    rounded(ax, 8, 28, 30, 8, "runSerenity(...)\nGreedyPlacer.place(all)", fc=GREEN_FILL, fontsize=8.5, weight="semibold")
    rounded(ax, 62, 28, 30, 8, "runCrunch(...)\nEDF → bestFit → place", fc=ORANGE_FILL, fontsize=8.5, weight="semibold")
    ax.text(28, 38, "Yes / Serenity", fontsize=8, color=GREEN)
    ax.text(72, 38, "No / Crunch", fontsize=8, color=ORANGE)
    arrow(ax, 38, 48, 23, 36.2)
    arrow(ax, 62, 48, 77, 36.2)
    rounded(ax, 28, 12, 44, 7, "Persist SchedulePlan + ScheduleBlocks", fc=FILL, fontsize=9)
    arrow(ax, 23, 28, 50, 19.2)
    arrow(ax, 77, 28, 50, 19.2)
    ax.text(50, 4, "Maps to SchedulerEngine.generate / selectMode / runSerenity / runCrunch",
            ha="center", fontsize=7.5, color=MUTED, style="italic")
    save(fig, "image16.png")


# --- image17: serenity ---
def diagram_serenity():
    fig, ax = new_fig(7, 10)
    ax.text(50, 97, "Serenity Mode — scoring & greedy placement", ha="center", fontsize=11, weight="bold")
    steps = [
        (86, "runSerenity(tasks, start, end, profile)"),
        (76, "score each task → PriorityQueue (score DESC)"),
        (66, "remainingHours map = effective durations"),
        (56, "for each working day in [start…end]"),
        (46, "cursor = day @ preferredStart\ndayHoursLeft = hoursPerDay"),
        (34, "while day has time: poll queue,\nplace min(day, left, sessionCap)"),
        (22, "create SCHEDULED block;\nif leftover → queue.offer(task)"),
        (12, "leftover remainingHours → DELAYED"),
    ]
    y_prev = None
    for y, text in steps:
        rounded(ax, 12, y, 76, 8 if "\n" not in text else 9.5, text, fc=GREEN_FILL if y > 80 else FILL, fontsize=8)
        if y_prev is not None:
            arrow(ax, 50, y_prev, 50, y + (9.5 if "\n" in text else 8))
        y_prev = y
    ax.text(50, 3, "Implementation: SchedulerEngine.runSerenity → GreedyPlacer.place",
            ha="center", fontsize=7, color=MUTED, style="italic")
    save(fig, "image17.png")


# --- image18: crunch ---
def diagram_crunch():
    fig, ax = new_fig(8, 10)
    ax.text(50, 97, "Crunch Mode — EDF, Best-Fit, place survivors", ha="center", fontsize=11, weight="bold")
    rounded(ax, 18, 86, 64, 7, "runCrunch(tasks, start, end, profile)", fc=ORANGE_FILL, fontsize=9, weight="bold")
    rounded(ax, 16, 74, 68, 7, "Sort tasks by deadline ASC (EDF)", fc=FILL, fontsize=9)
    arrow(ax, 50, 86, 50, 81.2)
    rounded(ax, 14, 62, 72, 7, "available = availableHours(start, end, profile)", fc=FILL, fontsize=9)
    arrow(ax, 50, 74, 50, 69.2)
    rounded(ax, 12, 48, 76, 9, "BestFitSelector.select(tasks, available)\noverflow → drop lowest priority / shortest first",
            fc=PINK_FILL, fontsize=8.5)
    arrow(ax, 50, 62, 50, 57.2)
    rounded(ax, 14, 34, 72, 8, "GreedyPlacer.place(fit.remaining(), …)", fc=GREEN_FILL, fontsize=9)
    arrow(ax, 50, 48, 50, 42.2)
    rounded(ax, 14, 20, 72, 8, "For each fit.excluded() → BlockDecision.EXCLUDED", fc=ORANGE_FILL, fontsize=9)
    arrow(ax, 50, 34, 50, 28.2)
    rounded(ax, 18, 8, 64, 7, "Return ScheduleResult(CRUNCH, blocks)", fc=FILL, fontsize=9)
    arrow(ax, 50, 20, 50, 15.2)
    ax.text(50, 2, "No separate “ask user” branch in current code — Best-Fit is the automatic trim",
            ha="center", fontsize=7, color=MUTED, style="italic")
    save(fig, "image18.png")


# --- iteration cycle for test plans ---
def diagram_iterations():
    fig, ax = new_fig(12, 5.2)
    ax.text(50, 92, "Iterative development (design → develop → test)", ha="center",
            fontsize=12, weight="bold", color=INK)
    colors = ["#1B4F72", "#148F77", "#1D8348", "#B7950B", "#CA6F1E", "#922B21"]
    xs = [10, 26, 42, 58, 74, 90]
    ys = [58, 32, 58, 32, 58, 32]
    for i, (x, y, c) in enumerate(zip(xs, ys, colors), start=1):
        circ = Circle((x, y), 9, facecolor=c, edgecolor=c, alpha=0.18, lw=0)
        ax.add_patch(circ)
        circ2 = Circle((x, y), 9, facecolor="none", edgecolor=c, lw=2.0)
        ax.add_patch(circ2)
        # three arc labels
        ax.text(x, y + 5.5, "DESIGN", ha="center", fontsize=6, color=c, weight="bold")
        ax.text(x + 5.2, y - 2, "TEST", ha="center", fontsize=6, color=c, weight="bold")
        ax.text(x - 5.2, y - 2, "DEV", ha="center", fontsize=6, color=c, weight="bold")
        ax.text(x, y + 0.5, f"#{i}", ha="center", fontsize=9, color=c, weight="bold")
        if i < 6:
            ax.annotate("", xy=(xs[i] - 9.5, ys[i]), xytext=(x + 9.5, y),
                        arrowprops=dict(arrowstyle="-|>", color=STROKE, lw=1.2))
    # timeline
    ax.annotate("", xy=(96, 12), xytext=(4, 12),
                arrowprops=dict(arrowstyle="-|>", color="#C0392B", lw=2.5))
    ax.text(50, 4, "Remember: projects are usually developed iteratively. For each iteration, a short test plan is written before development.",
            ha="center", fontsize=7.5, color="#C0392B", style="italic")
    save(fig, "iterations.png")


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
    print("done →", OUT)
