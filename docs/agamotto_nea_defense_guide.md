# Agamotto — OCR A-Level Computer Science NEA Master Defense Guide
### *Everything you need to confidently explain, demonstrate, and defend your project to your teacher.*

---

## 1. The 30-Second Elevator Pitch (Memorize This)

> *"Agamotto is an intelligent project management and automated scheduling platform built with a Java Spring Boot backend, MySQL database, and React frontend.*
> 
> *Most timetable apps are static: if you miss a study session, the whole plan breaks, or they cram a 6-hour task into a single exhausting night. Agamotto solves this computationally:*
> 1. *It evaluates whether your workload fits into your available hours (**Serenity Mode**) or if you're overbooked (**Crunch Mode**).*
> 2. *It uses a **Priority Queue** with a multi-factor heuristic scoring formula (priority, deadline urgency, duration) to order tasks.*
> 3. *It decomposes tasks across calendar days using **anti-burnout session caps** (max 2 hours for hard tasks) and **daily pacing**.*
> 4. *In group projects, it uses **load-balancing** based on members' relative busy percentages, and supports real-time WebSockets for live collaboration.*
> 5. *If you miss a session, its **dynamic rescheduling engine** automatically replans the remaining backlog without losing completed history."*

---

## 2. High-Level Architecture (The 10,000-Foot View)

```
┌────────────────────────────────────────────────────────┐
│                   FRONTEND (React + Vite)              │
│  - Pages: Dashboard, Generate Schedule, Calendar Grid  │
│  - Axios HTTP Client (Bearer JWT token in headers)    │
│  - STOMP WebSocket Client (/ws) for real-time updates  │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP REST / STOMP WS
                            ▼
┌────────────────────────────────────────────────────────┐
│              BACKEND (Spring Boot 3 + Java 17)         │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 1. Security & Auth Filter                        │  │
│  │    - JwtAuthenticationFilter validates Bearer    │  │
│  │    - SecurityConfig protects all routes          │  │
│  └────────────────────────┬─────────────────────────┘  │
│                           ▼                            │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 2. REST Controllers                              │  │
│  │    - ScheduleController, TaskController, etc.    │  │
│  └────────────────────────┬─────────────────────────┘  │
│                           ▼                            │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 3. Service Layer (Business & Algorithmic Logic)  │  │
│  │    - SchedulePlanServiceImpl                     │  │
│  │    - SchedulerEngine (Serenity vs Crunch)        │  │
│  │    - ScheduleBuilder (Day-by-day greedy placer)  │  │
│  │    - BestFitSelector (Overload exclusion)        │  │
│  │    - ScoringStrategy (Multi-factor heuristic)    │  │
│  │    - GroupScheduleBuilder (Team load balancer)   │  │
│  └────────────────────────┬─────────────────────────┘  │
│                           ▼                            │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 4. Spring Data JPA Repositories (Hibernate ORM)  │  │
│  │    - ProjectRepository, TaskRepository, etc.     │  │
│  └────────────────────────┬─────────────────────────┘  │
└───────────────────────────┼────────────────────────────┘
                            │ SQL Queries / Transactions
                            ▼
┌────────────────────────────────────────────────────────┐
│                  DATABASE (MySQL 8)                    │
│  - Tables: users, user_profiles, projects, tasks,     │
│    task_history, schedule_plans, schedule_blocks...    │
└────────────────────────────────────────────────────────┘
```

---

## 3. The Core Algorithms Deep-Dive (Your Group A Complex Techniques)

This is where you earn the top marks for OCR NEA Technical Solution.

### Algorithm 1: Serenity Mode vs Crunch Mode
* **File:** [`SchedulerEngine.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/SchedulerEngine.java)
* **Method:** `generate()`, `runSerenity()`, `runCrunch()`
* **The Logic:**
  1. Calculate total workload: $\sum \text{EffectiveHours}(\text{tasks})$.
  2. Calculate total capacity: Count working days $\times$ daily hours (checking if weekends are enabled in `UserProfile`).
  3. **Capacity Check:**
     - If $\text{Total Workload} \le \text{Available Hours}$: Choose **SERENITY MODE**. All tasks can fit!
     - If $\text{Total Workload} > \text{Available Hours}$: Choose **CRUNCH MODE**. You are overloaded!
  4. In Crunch Mode:
     - Sort tasks by deadline.
     - Call `BestFitSelector` to exclude the cheapest/lower-priority tasks until the remaining work fits into the window.
     - Schedule the survivors and mark dropped tasks as `EXCLUDED` with an explanatory reason.

---

### Algorithm 2: The Heuristic Scoring Formula
* **File:** [`ScoringStrategy.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/ScoringStrategy.java)
* **Method:** `scoreTask(Task, UserProfile, LocalDate)`
* **Mathematical Formula:**
  $$\text{Score} = (w_p \times \text{Priority}) + (w_u \times \text{Urgency}) + (w_{ed} \times \text{Duration})$$
  Where:
  - $w_p, w_u, w_{ed}$ are user weights configured in `UserProfile` (default $1.0$ each).
  - $\text{Priority}$ is an integer $1$ to $5$ ($5$ being most critical).
  - $\text{Urgency} = \frac{1}{\text{Days Until Deadline}}$ (or $2.0$ maximum urgency if due today or overdue).
  - $\text{Duration}$ uses `correctedDurationHours` (if user/system updated it) or falls back to `estimatedDurationHours`.

---

### Algorithm 3: The Day-by-Day Simulation & Anti-Burnout Pacing
* **File:** [`ScheduleBuilder.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/ScheduleBuilder.java)
* **Method:** `buildSchedule(...)`
* **The Logic:**
  1. **Calendar Loop:** Advances day-by-day from `startDate` to `endDate`.
  2. **Morning Queue:** Every morning, unfinished tasks are placed in a **Binary Heap `PriorityQueue`** ordered by their heuristic priority score.
  3. **Anti-Burnout Rule 1 (Sitting Caps):** High complexity tasks cannot be worked on indefinitely in one sitting:
     - Complexity $1\text{--}2$ (Easy): max $0.5$ hours (30 min).
     - Complexity $3$ (Medium): max $1.0$ hour.
     - Complexity $4\text{--}5$ (Hard): max $2.0$ hours.
  4. **Anti-Burnout Rule 2 (Daily Pacing):** 
     $$\text{Daily Pacing} = \frac{\text{Remaining Task Hours}}{\text{Working Days Left}}$$
     If a task needs 6 hours and is due in 3 days, today's quota is 2 hours. Once it reaches 2 hours today, it is deferred to tomorrow (`tasksForTomorrow`), allowing other tasks to be worked on today.
  5. **Panic Mode Override:** If the deadline is today or tomorrow ($\le 1$ working day left), the daily pacing cap is removed to ensure the task finishes on time!
  6. **Multi-Round Daily Placement:** If a task still has quota left today but just finished a sitting, it is put into `tasksForTodayNextRound`, letting other queue items take a turn before coming back to it.

---

### Algorithm 4: Team Load Balancing (Group Projects)
* **File:** [`GroupScheduleBuilder.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/group/GroupScheduleBuilder.java)
* **Method:** `buildSchedule(...)`
* **The Logic:**
  1. Each member has a `MemberSchedule` tracking:
     - Their start/end working hours.
     - Their total capacity over the project duration.
     - Their booked hours so far.
  2. **Relative Load Metric:** Instead of checking raw hours booked, it calculates:
     $$\text{Busy Percentage} = \frac{\text{Hours Booked}}{\text{Total Available Hours}}$$
     *Why this is clever:* A part-time member with 20 hours who has booked 10 hours is at 50% capacity. A full-time member with 40 hours who has booked 10 hours is only at 25% capacity. The algorithm assigns unassigned tasks to the full-time member!
  3. **Admin Routing:** If an admin designated an assignee on the task, the scheduler respects it. If that member is booked for the day, the task is deferred to tomorrow.

---

### Algorithm 5: Dynamic Rescheduling for Unattended Sessions
* **File:** [`ScheduleBuilder.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/ScheduleBuilder.java#L307-L456)
* **Method:** `rearrangeForUnattendedSession(...)`
* **The Logic:**
  - What happens when a user misses a study block?
  1. All blocks completed **before** the missed session are preserved as-is.
  2. The missed block is kept on the timetable as an audit record (marked with reason *"Unattended session"*), but its hours are **not** deducted from the task.
  3. The clock advances to the end of the unattended block, and all remaining tasks and missed hours are re-fed into the greedy placement engine to fill subsequent slots.
  4. **Deadline Clamping & Unified Delay Resolution:** If rearranging the schedule pushes remaining work into or past the day of the deadline, blocks are clamped at the deadline boundary. In a consolidated post-processing loop, any tasks with unplaced hours produce a `DELAYED` block. If approaching or passing the deadline prevented scheduling, it logs the event and records an explicit descriptive error: `"Delayed: Error: Rearranging schedule after unattended session approached the day of the deadline (<date>). Remaining <X>h cannot be accommodated before deadline — you can't do anything anymore."` (or stating the deadline has already arrived or passed).

---

## 4. "Why Did You Choose That Data Structure?" (Goldmine for NEA Marks)

Your teacher will almost certainly ask: *"Why did you use this collection here instead of an array or simple list?"*

| Data Structure | Where Used | Why It Was Chosen Over Alternatives |
| :--- | :--- | :--- |
| **`PriorityQueue`** (Binary Heap) | `ScheduleBuilder`, `GroupScheduleBuilder` | The pool of tasks is dynamic. Tasks are partially completed, split, and re-inserted. A heap gives $O(\log n)$ insertion and removal. Sorting a standard `ArrayList` after every single block placement would be wasteful ($O(n \log n)$ repeatedly). |
| **`IdentityHashMap`** | `ScheduleBuilder` (`remainingHoursPerTask`) | Standard `HashMap` uses `equals()` and `hashCode()`. For unsaved JPA entities, IDs can be `null`, causing key collisions! `IdentityHashMap` compares keys by **reference identity** (`==` memory address), guaranteeing each task object safely tracks its own backlog. |
| **`HashSet`** | `BestFitSelector` (`excludedSet`) | Provides $O(1)$ constant time lookup for `.contains()`. If an `ArrayList` were used to check which tasks were excluded, it would take $O(n)$ per check, degrading the filtering to $O(n^2)$. |
| **`EnumSet`** | `SchedulePlanServiceImpl` (`SCHEDULABLE`) | Backed by a single long bitmask under the hood. It offers extreme CPU efficiency ($O(1)$ bitwise operations) and type safety, preventing invalid status strings. |
| **`LinkedHashSet`** | `tasksForTomorrow` | Preserves deterministic insertion order while guaranteeing no duplicate task references are queued across days. |
| **`StringBuilder`** | Explanations and audit reasons | Pre-allocates buffer for appending audit fragments; avoids creating multiple immutable `String` objects in garbage collection memory. |

---

## 5. Instant Code Navigation Map (Where to Click in Your IDE)

Keep this table handy during your presentation so you never hesitate when asked to show code:

| Feature / Concept | File | Key Method / Lines |
| :--- | :--- | :--- |
| **Serenity vs Crunch Decision** | [`SchedulerEngine.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/SchedulerEngine.java) | `selectMode()`, `generate()` |
| **Best-Fit Exclusion** | [`BestFitSelector.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/BestFitSelector.java) | `select()` |
| **Heuristic Scoring & Urgency** | [`ScoringStrategy.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/ScoringStrategy.java) | `scoreTask()`, `deadlineUrgency()` |
| **Day Simulation & Pacing** | [`ScheduleBuilder.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/ScheduleBuilder.java) | `buildSchedule()` (around line 105) |
| **Anti-Burnout Caps** | [`ScheduleBuilder.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/ScheduleBuilder.java) | `getMaxSessionHours()`, constants at top |
| **Unattended Rescheduling** | [`ScheduleBuilder.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/ScheduleBuilder.java#L307-L456) | `rearrangeForUnattendedSession()` |
| **Group Load Balancing** | [`GroupScheduleBuilder.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/group/GroupScheduleBuilder.java) | `findLeastBusyAvailableMember()` |
| **Plan Persistence & Audit** | [`SchedulePlanServiceImpl.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/impl/SchedulePlanServiceImpl.java) | `generateAndPersist()` |
| **JWT Security Filter** | [`JwtAuthenticationFilter.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/config/JwtAuthenticationFilter.java) | `doFilterInternal()` |
| **STOMP WebSocket Setup** | [`WebSocketConfig.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/config/WebSocketConfig.java) | `registerStompEndpoints()` |

---

## 6. Top 10 Teacher Viva Questions & Exact Answers

### Q1: "In simple terms, what makes this more than just a basic calendar app?"
> **Answer:** *"A standard calendar app is passive—it just stores slots that the user manually drags and drops. Agamotto is an automated constraint-satisfaction and scheduling engine. It takes task complexity, deadlines, user availability, and historical estimation accuracy, and algorithmically constructs an optimal schedule that prevents burnout through daily decomposition and complexity session caps."*

### Q2: "How does your algorithm decide whether to run Serenity or Crunch mode?"
> **Answer:** *"In `SchedulerEngine`, it compares the sum of effective task durations with the total working capacity between the start and end dates. If total hours fit within capacity, it runs Serenity mode. If the user is overbooked, it switches to Crunch mode, which uses a Best-Fit heuristic to exclude lower-priority and shorter tasks so that the critical workload can still be accomplished before deadlines."*

### Q3: "Walk me through how you score tasks."
> **Answer:** *"In `ScoringStrategy`, the formula is a weighted sum: $(w_p \times \text{priority}) + (w_u \times \text{urgency}) + (w_{ed} \times \text{duration})$. The weights are user-customizable in their profile. Urgency is calculated as $1 / \text{daysUntilDeadline}$, with a cap of $2.0$ for tasks due today or overdue so they always outrank tomorrow's work."*

### Q4: "What is daily pacing and why did you implement it?"
> **Answer:** *"If a student has a 10-hour coursework due in 5 days, a naive greedy algorithm would schedule all 10 hours on Day 1, which causes cognitive exhaustion. Daily pacing divides remaining hours by remaining working days—e.g., $10 / 5 = 2$ hours per day. Once a task hits its 2-hour quota for today, it yields to other tasks. However, if the deadline is today or tomorrow, the pacing cap lifts to ensure the deadline is met."*

### Q5: "Why did you use a `PriorityQueue` in `ScheduleBuilder`?"
> **Answer:** *"Because the set of ready tasks is not static. When a task finishes a 1-hour sitting, it still has work left, so it gets re-inserted. A Priority Queue is a binary heap that provides $O(\log n)$ insertions and retrievals. Sorting an `ArrayList` repeatedly would be $O(n \log n)$ after every single block, which is computationally wasteful."*

### Q6: "Why did you use `IdentityHashMap` instead of a regular `HashMap`?"
> **Answer:** *"In `ScheduleBuilder`, we track remaining hours per task. JPA entities use ID-based `equals()` and `hashCode()`. For newly created tasks or unsaved entities where the ID is temporarily `null`, a standard `HashMap` would treat them as the exact same key and overwrite data! `IdentityHashMap` compares object memory addresses (`==`), guaranteeing every task instance has its own isolated remaining-hours tracker."*

### Q7: "How does group scheduling work when tasks don't have an assigned person?"
> **Answer:** *"In `GroupScheduleBuilder`, if an admin hasn't assigned a specific member, the engine performs heuristic load balancing. It checks all members who have free hours today and selects the one with the lowest `busyPercentage` ($\text{booked hours} / \text{total capacity}$). This ensures fair distribution relative to whether someone works part-time or full-time."*

### Q8: "What happens if a student misses a scheduled study session?"
> **Answer:** *"In `ScheduleBuilder.rearrangeForUnattendedSession`, past completed sessions are locked and untouched. The missed block is retained with an audit note explaining it was missed, but its hours are restored to the task backlog. The engine then replans all remaining work starting from the time the missed session ended. If rearranging the schedule causes the uncompleted work to approach the day of the deadline such that remaining hours cannot fit before the deadline, the scheduler clamps blocks at the deadline and creates a `DELAYED` block with an explicit error explaining that the deadline has been approached and you can't do anything anymore."*

### Q9: "How is the system secured?"
> **Answer:** *"We use stateless JWT (JSON Web Tokens) with Spring Security. When a user logs in, a signed JWT token is issued. Every subsequent HTTP request sends this token in the `Authorization: Bearer <token>` header. The `JwtAuthenticationFilter` intercepts requests, validates the signature and expiration, extracts the user ID, and sets up the Spring `SecurityContext`."*

### Q10: "If you had another month to work on this, what would you improve?" (Evaluation Criterion)
> **Answer:** *"I would implement machine learning for auto-tuning estimation correction—learning each individual user's velocity factor per category over time (e.g., if a student consistently underestimates coding tasks by 25%). I would also integrate Google Calendar two-way sync via the Google Calendar API."*

---

## 7. The 24-Hour Study Plan

1. **Tonight (30 minutes):**
   - Read Section 1 (Elevator Pitch) and practice saying it out loud twice.
   - Read Section 3 (The Core Algorithms) and Section 4 (Data Structures).
2. **Tonight (15 minutes):**
   - Open VS Code / IntelliJ. Navigate to `SchedulerEngine.java`, `ScheduleBuilder.java`, and `GroupScheduleBuilder.java`. Locate each method from Section 5.
3. **Tomorrow Morning (15 minutes before school):**
   - Skim Section 6 (Top 10 Questions & Answers). Test yourself on Q2, Q4, Q5, and Q6.
