# Agamotto — Plain-English Project Guide & Code Walkthrough

> **What is Agamotto?**  
> Agamotto is an automated study and project timetable generator. Instead of forcing you to drag and drop boxes on a
> calendar, it automatically designs your daily schedule, stops you from burning out on hard tasks, and automatically
> replans your week if you miss a study session.

---

## 1. What Problem Does Agamotto Solve?

Traditional calendar apps (like Google Calendar, Apple Calendar, or Trello) are completely **manual and dumb**:

- If you miss a 2-hour study session on Tuesday, Google Calendar does nothing. You have to manually drag every single
  box around to catch up.
- If you have a big 6-hour essay due on Friday, a naive planner might cram all 6 hours into Thursday night, leaving you
  completely exhausted and burned out.
- In team projects, tasks are usually split by raw count rather than checking who is working part-time versus full-time,
  leaving some people swamped while others have nothing to do.

**Agamotto solves this automatically:**

1. **Workload Check (Serenity vs. Crunch Mode):** It compares how much free time you have with how much work you need to
   do.
    - **Serenity Mode:** If all your tasks fit comfortably into your schedule, it places them at a relaxed, steady pace.
    - **Crunch Mode:** If you have more work than hours in the week, it automatically prioritizes your most critical
      tasks and excludes lower-priority items so you don't miss key deadlines.
2. **Smart Task Ordering:** It ranks tasks using three simple factors:
    - **Importance:** How high the task's priority is (rated 1 to 5).
    - **Urgency:** How close the deadline is (tasks due today or tomorrow get bumped to the front).
    - **Effort:** How long the task will take.
3. **Burnout Protection (Session Limits & Daily Pacing):**
    - **Sitting Limits:** It stops you from working on difficult tasks for too long in a single sitting. Easy tasks max
      out at 30 minutes, medium tasks at 1 hour, and hard tasks at 2 hours.
    - **Daily Pacing:** If a 10-hour project is due in 5 days, Agamotto spreads it out to 2 hours a day
      (`Remaining Hours / Days Left`) rather than giving you 10 hours on day one. If the deadline is tomorrow, it lifts
      the limit so you still finish in time.
4. **Automatic Catch-Up (Dynamic Rescheduling):** If you miss a study block, your schedule doesn't break. Agamotto locks
   in what you've already completed, puts the missed hours back onto your to-do list, and automatically reschedules the
   rest of your week.
5. **Fair Team Balancing (Group Projects):** In team projects, unassigned tasks are automatically handed to whichever
   team member has the lowest percentage of their available hours booked so far.
6. **Live Collaboration:** Teammates see schedule changes, new task assignments, and who is online in real time using
   WebSockets.

---

## 2. The 3 Main Parts of the System

```
┌────────────────────────────────────────────────────────┐
│               1. FRONTEND (What You See)               │
│  - Built with React 19, TypeScript, Vite & Tailwind CSS│
│  - Interactive Weekly Timetable & Task Manager         │
│  - Sends login tokens (JWT) with every HTTP request    │
└───────────────────────────┬────────────────────────────┘
                            │ Web Requests & Real-Time Sync
                            ▼
┌────────────────────────────────────────────────────────┐
│               2. BACKEND (The Engine)                  │
│  - Built with Java 25 & Spring Boot                   │
│  - Security: Checks login tokens & locks private data │
│  - Scheduling Engine: Calculates the optimal timetable │
│  - Real-Time Hub: Broadcasts live updates via WebSocket│
└───────────────────────────┬────────────────────────────┘
                            │ Saves & Loads Data
                            ▼
┌────────────────────────────────────────────────────────┐
│               3. DATABASE (The Memory)                 │
│  - MySQL 8 database storing users, projects, tasks,    │
│    timetables, and history logs                        │
└────────────────────────────────────────────────────────┘
```

---

## 3. How the Core Scheduling Engine Works

Here is a step-by-step breakdown of what happens when you click **"Generate Schedule"**:

### Step 1: Can It All Fit? (Serenity vs. Crunch)

- The engine calculates your **Total Workload** (the sum of all task hours).
- It calculates your **Available Hours** (counting the working days between your start and end dates multiplied by your
  daily working hours).
- **If Workload <= Free Hours:** It chooses **Serenity Mode**. Every task will be scheduled.
- **If Workload > Free Hours:** It switches to **Crunch Mode**. It sorts tasks to drop the cheapest, lower-priority
  tasks first until the remaining work fits into your week. Dropped tasks are marked as `EXCLUDED` with an explanation.

### Step 2: Ranking Tasks Each Morning

Every day in the simulation, unfinished tasks are placed in a **Priority Queue** (a high-performance binary heap). The
task with the highest composite score gets scheduled first:

- `Score = (Priority Weight * Priority) + (Urgency Weight * Urgency) + (Duration Weight * Duration)`
- Tasks due today or overdue get top urgency, ensuring they are scheduled before tomorrow's work.

### Step 3: Splitting Tasks to Prevent Burnout

Instead of scheduling a whole 8-hour task at once:

1. **Sitting Limit:** A hard task is capped at 2 hours per sitting.
2. **Daily Pacing:** It calculates `Remaining Hours / Days Until Deadline`. If an essay needs 6 hours over 3 days,
   today's quota is 2 hours.
3. Once a task hits its 2-hour quota for today, it yields and moves to tomorrow's list, letting other tasks have a turn
   today.

### Step 4: What Happens If You Miss a Session?

When you click **"Mark Unattended"**:

1. All sessions you finished *before* the missed block stay locked in place.
2. The missed session stays on the calendar as a historical note (`"Unattended session: 1.5h missed"`), but its hours
   are returned to the task backlog.
3. Starting from the exact moment the missed session ended, Agamotto replans all unfinished tasks into future available
   slots.
4. If there aren't enough working hours left before the deadline, it marks the remaining hours as `DELAYED` with the
   clear note: `"Remaining work cannot be accommodated before deadline — you can't do anything anymore"`.

---

## 4. Why Specific Data Structures Were Chosen

If asked in your exam or presentation why you used specific Java collections instead of basic lists or arrays:

| Data Structure                    | Where It Is Used                    | Plain-English Reason                                                                                                                                                                                                                                                            |
|:----------------------------------|:------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`PriorityQueue`** (Binary Heap) | Ordering tasks in `ScheduleBuilder` | The pool of tasks changes constantly as pieces are completed and re-inserted. A heap gives very fast insertion and removal (`O(log n)`). Re-sorting a regular list after every single chunk would be much slower (`O(n log n)` repeatedly).                                     |
| **`IdentityHashMap`**             | Tracking remaining hours per task   | Normal `HashMap` uses entity IDs. For newly created tasks that haven't been saved to MySQL yet, the ID is empty (`null`), which would cause tasks to overwrite each other! `IdentityHashMap` compares objects by memory location (`==`), giving each task its own safe tracker. |
| **`EnumSet`**                     | Tracking active task statuses       | Extremely fast bitmask under the hood that prevents accidental typos or invalid statuses from being passed in.                                                                                                                                                                  |
| **`LinkedHashSet`**               | Carrying tasks over to tomorrow     | Keeps tasks in the exact order they were queued while guaranteeing no duplicates are added.                                                                                                                                                                                     |
| **`Epsilon (1e-9)`**              | Checking if hours are zero          | Computer floating-point numbers have tiny rounding inaccuracies (e.g. `0.1 + 0.2 = 0.30000000000000004`). Checking `hours > 0.000000001` prevents infinite loops caused by tiny rounding leftovers.                                                                             |

---

## 5. Recommended Code Improvements

Here are the 7 key code changes to make the project cleaner, safer, and bug-free:

---

### Change 1: Fix Database Column Name in `DashboardReport.java`

* **File:** [
  `DashboardReport.java:L25-L28`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/entities/DashboardReport.java#L25-L28)
* **The Problem:** In [
  `User.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/entities/User.java),
  the primary key column is named `user_id`. In `DashboardReport`, the `@ManyToOne private User user;` is missing an
  explicit `@JoinColumn`. Hibernate tries to link to `user_user_id`, causing a database warning on startup.
* **The Fix:** Add `@JoinColumn(name = "user_id", nullable = false)`.

```diff
--- a/src/main/java/com/srikrishnanethi/agamotto/entities/DashboardReport.java
+++ b/src/main/java/com/srikrishnanethi/agamotto/entities/DashboardReport.java
@@ -25,3 +25,4 @@ public class DashboardReport extends BaseEntity {
     @Comment("User the counts belong to (FK users.user_id)")
     @ManyToOne(fetch = FetchType.LAZY, optional = false)
+    @JoinColumn(name = "user_id", nullable = false)
     private User user;
```

---

### Change 2: Allow Teammates to Work at the Same Time in `SchedulePlanServiceImpl.java`

* **File:** [
  `SchedulePlanServiceImpl.java:L327-L348`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/impl/SchedulePlanServiceImpl.java#L327-L348)
* **The Problem:** When moving or editing a block, `assertNoScheduledOverlap` checks if *any* block in the project
  overlaps. In a group project, Member A and Member B *can* work at the same time on different tasks! The current code
  throws an error if two teammates work during the same hour.
* **The Fix:** Only throw an overlap conflict if both blocks belong to the **same person**.

```diff
--- a/src/main/java/com/srikrishnanethi/agamotto/service/impl/SchedulePlanServiceImpl.java
+++ b/src/main/java/com/srikrishnanethi/agamotto/service/impl/SchedulePlanServiceImpl.java
@@ -327,6 +327,8 @@ public class SchedulePlanServiceImpl implements SchedulePlanService {
     private void assertNoScheduledOverlap(ScheduleBlock moving, LocalDateTime start, LocalDateTime end) {
         String planId = moving.getSchedule().getId();
+        String movingUserId = (moving.getTask() != null && moving.getTask().getAssignee() != null)
+                ? moving.getTask().getAssignee().getId() : null;
         List<ScheduleBlock> siblings = scheduleBlockRepository.findByScheduleIdOrderByStartTime(planId);
         for (ScheduleBlock other : siblings) {
             if (other.getId() != null && other.getId().equals(moving.getId())) {
                 continue;
             }
             if (other.getDecision() != BlockDecision.SCHEDULED) {
                 continue;
             }
+            String otherUserId = (other.getTask() != null && other.getTask().getAssignee() != null)
+                    ? other.getTask().getAssignee().getId() : null;
+            // Allow different teammates to work at the same time
+            if (movingUserId != null && otherUserId != null && !movingUserId.equals(otherUserId)) {
+                continue;
+            }
             LocalDateTime otherStart = other.getStartTime();
             LocalDateTime otherEnd = other.getEndTime();
             if (otherStart == null || otherEnd == null) {
                 continue;
             }
             if (start.isBefore(otherEnd) && otherStart.isBefore(end)) {
                 throw new ScheduleConflictException(
                         "Schedule conflict: block overlaps another SCHEDULED block on the same plan"
                                 + " (" + otherStart + " – " + otherEnd + ")");
             }
         }
     }
```

---

### Change 3: Remove Hardcoded Password in `application.properties`

* **File:** [
  `application.properties:L8`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/resources/application.properties#L8)
* **The Problem:** A private local database password was committed in the configuration file.
* **The Fix:** Remove the hardcoded password fallback so it defaults to empty.

```diff
--- a/src/main/resources/application.properties
+++ b/src/main/resources/application.properties
@@ -8,1 +8,1 @@
-spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:Knoteion@43215}
+spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
```

---

### Change 4: Support Additional Frontend Ports in `SecurityConfig.java`

* **File:** [
  `SecurityConfig.java:L69-L72`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/config/SecurityConfig.java#L69-L72)
* **The Problem:** CORS is locked only to port 5173. If port 5173 is occupied and Vite opens on 5174, or if you run
  `pnpm preview` on 4173, the frontend cannot talk to the backend.
* **The Fix:** Add ports `5174` and `4173` to the allowed origins list.

```diff
--- a/src/main/java/com/srikrishnanethi/agamotto/config/SecurityConfig.java
+++ b/src/main/java/com/srikrishnanethi/agamotto/config/SecurityConfig.java
@@ -69,4 +69,7 @@ public class SecurityConfig {
 		config.setAllowedOrigins(List.of(
 				"http://localhost:5173",
-				"http://127.0.0.1:5173"
+				"http://127.0.0.1:5173",
+				"http://localhost:5174",
+				"http://127.0.0.1:5174",
+				"http://localhost:4173",
+				"http://127.0.0.1:4173"
 		));
```

---

### Change 5: Clean Up Empty Group Scheduler Stubs

* **Files:**
    - [
      `GroupSchedulerEngine.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/group/GroupSchedulerEngine.java)
    - [
      `GroupBestFitSelector.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/group/GroupBestFitSelector.java)
    - [
      `GroupTaskChooser.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/group/GroupTaskChooser.java)
    - [
      `GroupScheduleResult.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/group/GroupScheduleResult.java)
* **The Problem:** These four files are empty 5-line placeholder stubs. The real group logic is completely handled
  inside [
  `GroupScheduleBuilder.java`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/service/scheduler/group/GroupScheduleBuilder.java).
* **The Fix:** Delete these empty files to keep your codebase clean and avoid questions about dead code during your
  presentation.

---

### Change 6: Allow Editing Tasks That Are Already Past Due

* **File:** [
  `UpdateTaskRequest.java:L19`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/src/main/java/com/srikrishnanethi/agamotto/dto/request/UpdateTaskRequest.java#L19)
* **The Problem:** The update request currently has `@Future LocalDateTime deadline`. If a task's deadline was
  yesterday, and you try to edit its title, change its notes, or mark it as `COMPLETED`, the server rejects the request
  with an error saying the deadline must be in the future!
* **The Fix:** Remove `@Future` from the update request (keep it on the creation request).

```diff
--- a/src/main/java/com/srikrishnanethi/agamotto/dto/request/UpdateTaskRequest.java
+++ b/src/main/java/com/srikrishnanethi/agamotto/dto/request/UpdateTaskRequest.java
@@ -19,1 +19,1 @@
-		@Future LocalDateTime deadline,
+		LocalDateTime deadline,
```

---

### Change 7: Fix React Hook Warnings in `ScheduleWeekPreview.tsx`

* **File:** [
  `ScheduleWeekPreview.tsx:L143-L144`](file:///c:/Users/Nekretaur/Documents/school-projects/agamotto/frontend/src/components/ScheduleWeekPreview.tsx#L143-L144)
* **The Problem:** Writing `const safeBlocks = blocks ?? []` creates a brand new array in memory every single time the
  component renders. Because this variable is used inside `useMemo`, the memoization is broken and triggers 9 linter
  warnings.
* **The Fix:** Define constant empty arrays outside the component.

```diff
--- a/frontend/src/components/ScheduleWeekPreview.tsx
+++ b/frontend/src/components/ScheduleWeekPreview.tsx
@@ -139,4 +139,7 @@
+const EMPTY_BLOCKS: ScheduleBlockResponse[] = []
+const EMPTY_TASKS: TaskResponse[] = []
+
 export function ScheduleWeekPreview({
   blocks,
   tasks,
   startDate,
   endDate,
 }: {
   blocks?: ScheduleBlockResponse[]
   tasks?: TaskResponse[]
   startDate: string
   endDate: string
 }) {
-  const safeBlocks = blocks ?? []
-  const safeTasks = tasks ?? []
+  const safeBlocks = blocks ?? EMPTY_BLOCKS
+  const safeTasks = tasks ?? EMPTY_TASKS
```
