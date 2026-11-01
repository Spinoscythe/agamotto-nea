import { useEffect, useMemo, useState } from 'react'
import { XCircleIcon } from '@phosphor-icons/react'
import {
  leavesApi,
  type LeaveResponse,
  type ScheduleBlockResponse,
  type TaskResponse,
} from '@/api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import {
  deadlineDatePart,
  eachDateInclusive,
  occupiedHourRange,
  toDateInputValue,
} from '@/lib/datetime'
import { ScheduleDecisions } from './schedule/ScheduleDecisions'
import { ScheduleTimetable } from './schedule/ScheduleTimetable'
import {
  DEFAULT_HOURS,
  parseReason,
  type StatusGroup,
  type StatusItem,
  type TaskDecision,
} from './schedule/preview-utils'

export function ScheduleWeekPreview({
  blocks = [],
  tasks = [],
  startDate,
  endDate,
  projectId,
  onDeleteTask,
  onMarkUnattended,
  onSelectBlock,
}: {
  blocks?: ScheduleBlockResponse[] | null
  tasks?: TaskResponse[] | null
  startDate: string
  endDate: string
  projectId?: string
  onDeleteTask?: (taskId: string) => void
  onMarkUnattended?: (blockId: string) => void | Promise<void>
  onSelectBlock?: (blockId: string) => void
}) {
  const safeBlocks = useMemo(() => blocks ?? [], [blocks])
  const safeTasks = useMemo(() => tasks ?? [], [tasks])
  const taskById = useMemo(() => {
    const map = new Map<string, TaskResponse>()
    for (const t of safeTasks) map.set(t.id, t)
    return map
  }, [safeTasks])

  const allDays = useMemo(
    () => eachDateInclusive(startDate, endDate, 90),
    [startDate, endDate],
  )

  const weeks = useMemo(() => {
    // Always Mon-Sun: week starts on Monday on or before first day
    const start = new Date(`${startDate}T12:00:00`)
    const monday = new Date(start)
    monday.setDate(start.getDate() - ((start.getDay() + 6) % 7))
    const mondayStr = toDateInputValue(monday)
    const all = eachDateInclusive(mondayStr, endDate, 90)
    const list: string[][] = []
    for (let i = 0; i < all.length; i += 7) {
      list.push(all.slice(i, i + 7))
    }
    return list.length > 0 ? list : [[startDate]]
  }, [startDate, endDate])

  const [weekIndex, setWeekIndex] = useState(0)
  const activeWeekIndex = Math.min(weekIndex, Math.max(0, weeks.length - 1))
  const days = weeks[activeWeekIndex] ?? weeks[0]

  const [leaves, setLeaves] = useState<LeaveResponse[]>([])

  useEffect(() => {
    let cancelled = false
    void leavesApi
      .list({ start: `${startDate}T00:00:00`, end: `${endDate}T23:59:59` })
      .then((res) => {
        if (!cancelled) setLeaves(res)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [startDate, endDate])

  const leavesOnDay = useMemo(() => {
    const map = new Map<string, LeaveResponse[]>()
    for (const day of days) {
      const dayStart = new Date(`${day}T00:00:00`).getTime()
      const dayEnd = new Date(`${day}T23:59:59`).getTime()
      const matching = leaves.filter((l) => {
        const s = new Date(l.startTime).getTime()
        const e = new Date(l.endTime).getTime()
        return e > dayStart && s < dayEnd
      })
      map.set(day, matching)
    }
    return map
  }, [days, leaves])

  const scheduled = useMemo(
    () => safeBlocks.filter((b) => b.decision === 'SCHEDULED' && b.startTime),
    [safeBlocks],
  )

  const colorByTaskId = useMemo(() => {
    const ids = [...new Set(scheduled.map((b) => b.taskId))].sort()
    return new Map(ids.map((id, i) => [id, i] as const))
  }, [scheduled])

  const sittingByBlockId = useMemo(() => {
    const byTask = new Map<string, typeof scheduled>()
    for (const block of scheduled) {
      const list = byTask.get(block.taskId) ?? []
      list.push(block)
      byTask.set(block.taskId, list)
    }
    const map = new Map<string, { n: number; m: number }>()
    for (const list of byTask.values()) {
      list.sort((a, b) => (a.startTime ?? '').localeCompare(b.startTime ?? ''))
      list.forEach((block, i) => {
        map.set(block.id, { n: i + 1, m: list.length })
      })
    }
    return map
  }, [scheduled])

  const hours = useMemo(() => {
    const ranges = scheduled
      .map((b) => occupiedHourRange(b.startTime, b.endTime))
      .filter((r): r is NonNullable<typeof r> => r != null)
    if (ranges.length === 0) return DEFAULT_HOURS
    const minH = Math.min(9, ...ranges.map((r) => r.startHour))
    const maxH = Math.max(16, ...ranges.map((r) => r.endHourExclusive - 1))
    const list: number[] = []
    const lo = Math.max(0, minH)
    const hi = Math.min(23, maxH)
    for (let h = lo; h <= hi; h += 1) list.push(h)
    return list.length > 0 ? list : DEFAULT_HOURS
  }, [scheduled])

  const explainedBlocks = useMemo(
    () =>
      safeBlocks.filter(
        (b) =>
          Boolean(b.reason?.trim()) ||
          b.decision === 'DELAYED' ||
          b.decision === 'EXCLUDED',
      ),
    [safeBlocks],
  )

  const deadlineApproachedErrors = useMemo(() => {
    return safeBlocks
      .filter(
        (b) =>
          b.decision === 'DELAYED' &&
          b.reason &&
          (b.reason.toLowerCase().includes("you can't do anything anymore") ||
           b.reason.toLowerCase().includes('deadline approached')),
      )
      .map((b) => {
        const task = taskById.get(b.taskId)
        return {
          id: b.id,
          taskTitle: task?.title ?? 'Task',
          reason: b.reason!,
        }
      })
  }, [safeBlocks, taskById])

  const [decisionTab, setDecisionTab] = useState<'byTask' | 'bySitting'>('byTask')

  const taskDecisions = useMemo<TaskDecision[]>(() => {
    const taskIds = [...new Set(explainedBlocks.map((b) => b.taskId))]

    return taskIds.map((taskId) => {
      const task = taskById.get(taskId)
      const blocksForTask = explainedBlocks.filter((b) => b.taskId === taskId)
      const scheduledBlocks = blocksForTask.filter((b) => b.decision === 'SCHEDULED')
      const delayed = blocksForTask.filter((b) => b.decision === 'DELAYED')
      const excluded = blocksForTask.filter((b) => b.decision === 'EXCLUDED')

      let totalHours = 0
      for (const b of scheduledBlocks) {
        if (b.startTime && b.endTime) {
          const s = new Date(b.startTime).getTime()
          const e = new Date(b.endTime).getTime()
          if (!Number.isNaN(s) && !Number.isNaN(e) && e > s) {
            totalHours += (e - s) / (1000 * 60 * 60)
          }
        }
      }

      const sortedSittings = [...scheduledBlocks].sort((a, b) =>
        (a.startTime ?? '').localeCompare(b.startTime ?? '')
      )

      const primaryReason = blocksForTask[0]?.reason ?? ''
      const parsed = parseReason(primaryReason)

      const overallDecision: TaskDecision['overallDecision'] =
        delayed.length > 0 && scheduledBlocks.length === 0
          ? 'DELAYED'
          : excluded.length > 0 && scheduledBlocks.length === 0
          ? 'EXCLUDED'
          : 'SCHEDULED'

      return {
        taskId,
        task,
        overallDecision,
        totalHours,
        sittingsCount: scheduledBlocks.length,
        sittings: sortedSittings,
        delayedBlocks: delayed,
        parsed,
      }
    })
  }, [explainedBlocks, taskById])

  const statusGroups = useMemo<StatusGroup[]>(() => {
    const overdue: StatusItem[] = []
    const today: StatusItem[] = []
    const upcoming: StatusItem[] = []
    const notStarted: StatusItem[] = []
    const inProgress: StatusItem[] = []
    const done: StatusItem[] = []
    const archived: StatusItem[] = []
    const now = toDateInputValue()

    for (const t of safeTasks) {
      const due = deadlineDatePart(t.deadline)
      const item = { id: t.id, title: t.title }
      if (t.status === 'COMPLETED') done.push(item)
      else if (t.status === 'CANCELLED') archived.push(item)
      else if (t.status === 'IN_PROGRESS') inProgress.push(item)
      else if (due != null && due < now) overdue.push(item)
      else if (due != null && due === now) today.push(item)
      else if (t.status === 'PENDING') {
        notStarted.push(item)
        upcoming.push(item)
      }
    }

    return [
      { label: 'Overdue', items: overdue },
      { label: 'Today', items: today },
      { label: 'Upcoming', items: upcoming },
      { label: 'Not started', items: notStarted },
      { label: 'In progress', items: inProgress },
      { label: 'Done', items: done },
      { label: 'Archived', items: archived },
    ]
  }, [safeTasks])

  if (scheduled.length === 0 && safeTasks.length === 0) {
    return (
      <Empty className="border border-dashed">
        <EmptyHeader>
          <EmptyTitle>No preview yet</EmptyTitle>
          <EmptyDescription>
            Select a project with tasks or a generated schedule to preview the week.
          </EmptyDescription>
        </EmptyHeader>
      </Empty>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      {deadlineApproachedErrors.length > 0 ? (
        <Alert variant="destructive" className="border-destructive/50 bg-destructive/10 text-destructive shadow-sm">
          <XCircleIcon size={22} className="text-destructive shrink-0 mt-0.5" />
          <div className="flex flex-col gap-1.5 w-full">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <AlertTitle className="text-sm font-semibold text-destructive flex items-center gap-2">
                <span>Cannot Complete Schedule — Deadline Approached</span>
              </AlertTitle>
              <Badge variant="destructive" className="text-[0.65rem] uppercase font-bold tracking-wider px-1.5 py-0">
                Action Required
              </Badge>
            </div>
            <AlertDescription className="text-xs text-destructive/95 font-medium space-y-1.5">
              {deadlineApproachedErrors.map((err) => (
                <div
                  key={err.id}
                  className="rounded-md border border-destructive/30 bg-destructive/15 px-3 py-2 text-xs leading-relaxed"
                >
                  <p className="font-semibold text-destructive mb-0.5">
                    Task: {err.taskTitle}
                  </p>
                  <p className="text-destructive/90">{err.reason}</p>
                </div>
              ))}
            </AlertDescription>
          </div>
        </Alert>
      ) : null}

      <ScheduleTimetable
        scheduled={scheduled}
        taskById={taskById}
        weeks={weeks}
        days={days}
        activeWeekIndex={activeWeekIndex}
        onWeekChange={setWeekIndex}
        hours={hours}
        leaves={leaves}
        leavesOnDay={leavesOnDay}
        colorByTaskId={colorByTaskId}
        sittingByBlockId={sittingByBlockId}
        statusGroups={statusGroups}
        projectId={projectId}
        onDeleteTask={onDeleteTask}
        onSelectBlock={onSelectBlock}
      />

      {explainedBlocks.length > 0 ? (
        <ScheduleDecisions
          explainedBlocks={explainedBlocks}
          taskDecisions={taskDecisions}
          decisionTab={decisionTab}
          onDecisionTabChange={setDecisionTab}
          taskById={taskById}
          onMarkUnattended={onMarkUnattended}
        />
      ) : null}
    </div>
  )
}
