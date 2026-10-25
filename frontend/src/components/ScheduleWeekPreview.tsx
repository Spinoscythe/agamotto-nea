import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { TrashIcon, UserIcon, XCircleIcon } from '@phosphor-icons/react'
import type { ScheduleBlockResponse, TaskResponse } from '@/api'
import { DecisionBadge } from '@/components/DecisionBadge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import {
  deadlineDatePart,
  eachDateInclusive,
  occupiedHourRange,
  toDateInputValue,
} from '@/lib/datetime'
import { complexityLabel } from '@/lib/complexity'
import { cn } from '@/lib/utils'

const DEFAULT_HOURS = [9, 10, 11, 12, 13, 14, 15, 16]

function dayKey(iso: string | null): string | null {
  if (!iso || typeof iso !== 'string') return null
  return iso.slice(0, 10)
}

function formatHour(hour: number): string {
  if (hour === 0) return '12 AM'
  if (hour === 12) return '12 PM'
  if (hour > 12) return `${hour - 12} PM`
  return `${hour} AM`
}

interface ParsedReason {
  raw: string
  score: string | null
  priority: number | null
  deadlineStr: string | null
  durationHours: string | null
  sessionCap: string | null
  decomposedNote: string | null
  isDelayed: boolean
  delayedMessage: string | null
  scheduledMemberName: string | null
}

function parseReason(reason: string | null | undefined): ParsedReason {
  if (!reason) {
    return {
      raw: '',
      score: null,
      priority: null,
      deadlineStr: null,
      durationHours: null,
      sessionCap: null,
      decomposedNote: null,
      isDelayed: false,
      delayedMessage: null,
      scheduledMemberName: null,
    }
  }

  const isDelayed = reason.toLowerCase().startsWith('delayed')
  const scoreMatch = reason.match(/score=([0-9.]+)/)
  const priorityMatch = reason.match(/priority=([0-9]+)/)
  const deadlineMatch = reason.match(/deadline=([0-9-T:]+)/)
  const durationMatch = reason.match(/duration=([0-9.]+h?)/)
  const sessionCapMatch = reason.match(/Session split:\s*([^.]+)/) || reason.match(/capped at\s*([0-9.]+h?)/)
  const decomposedMatch = reason.match(/Decomposed across days[^.]*/) || reason.match(/Split across days[^.]*/)
  const memberMatch = reason.match(/for\s+([^(]+?)\s+\(currently/)
  const scheduledMemberName = memberMatch ? memberMatch[1].trim() : null

  let deadlineFormatted: string | null = null
  if (deadlineMatch) {
    const d = new Date(deadlineMatch[1])
    if (!Number.isNaN(d.getTime())) {
      deadlineFormatted = d.toLocaleDateString(undefined, {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
      })
    } else {
      deadlineFormatted = deadlineMatch[1].slice(0, 10)
    }
  }

  return {
    raw: reason,
    score: scoreMatch ? scoreMatch[1] : null,
    priority: priorityMatch ? Number(priorityMatch[1]) : null,
    deadlineStr: deadlineFormatted,
    durationHours: durationMatch ? durationMatch[1] : null,
    sessionCap: sessionCapMatch ? sessionCapMatch[1].trim() : null,
    decomposedNote: decomposedMatch ? decomposedMatch[0].trim() : null,
    isDelayed,
    delayedMessage: isDelayed ? reason : null,
    scheduledMemberName,
  }
}

function formatSittingDateTime(startIso: string | null | undefined, endIso: string | null | undefined): string {
  if (!startIso || !endIso) return '—'
  const start = new Date(startIso)
  const end = new Date(endIso)
  const dateStr = start.toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
  const startStr = start.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', hour12: false })
  const endStr = end.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', hour12: false })
  const hours = (end.getTime() - start.getTime()) / (1000 * 60 * 60)
  const durStr = hours === 1 ? '1h' : `${hours.toFixed(1)}h`
  return `${dateStr} · ${startStr} – ${endStr} (${durStr})`
}

const palette = [
  'bg-chart-1/80 text-white',
  'bg-chart-2/80 text-white',
  'bg-chart-3/80 text-white',
  'bg-chart-4/80 text-foreground',
]

type StatusItem = { id: string; title: string }

export function ScheduleWeekPreview({
  blocks = [],
  tasks = [],
  startDate,
  endDate,
  projectId,
  onDeleteTask,
  onMarkUnattended,
}: {
  blocks?: ScheduleBlockResponse[] | null
  tasks?: TaskResponse[] | null
  startDate: string
  endDate: string
  projectId?: string
  onDeleteTask?: (taskId: string) => void
  onMarkUnattended?: (blockId: string) => void | Promise<void>
}) {
  const safeBlocks = blocks ?? []
  const safeTasks = tasks ?? []
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
    const list: string[][] = []
    for (let i = 0; i < allDays.length; i += 7) {
      list.push(allDays.slice(i, i + 7))
    }
    return list.length > 0 ? list : [[startDate]]
  }, [allDays, startDate])

  const [weekIndex, setWeekIndex] = useState(0)
  const activeWeekIndex = Math.min(weekIndex, Math.max(0, weeks.length - 1))
  const days = weeks[activeWeekIndex] ?? weeks[0]

  const scheduled = safeBlocks.filter((b) => b.decision === 'SCHEDULED' && b.startTime)

  const assignees = useMemo(() => {
    const names = new Set<string>()
    for (const t of safeTasks) {
      if (t.assigneeName) names.add(t.assigneeName)
      else if (t.assigneeEmail) names.add(t.assigneeEmail)
    }
    for (const b of scheduled) {
      const p = parseReason(b.reason)
      if (p.scheduledMemberName) names.add(p.scheduledMemberName)
    }
    return Array.from(names).sort()
  }, [safeTasks, scheduled])

  const [assigneeFilter, setAssigneeFilter] = useState<string>('ALL')

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

  const taskDecisions = useMemo(() => {
    const taskIds = [...new Set(explainedBlocks.map((b) => b.taskId))]

    return taskIds.map((taskId) => {
      const task = taskById.get(taskId)
      const blocksForTask = explainedBlocks.filter((b) => b.taskId === taskId)
      const scheduled = blocksForTask.filter((b) => b.decision === 'SCHEDULED')
      const delayed = blocksForTask.filter((b) => b.decision === 'DELAYED')
      const excluded = blocksForTask.filter((b) => b.decision === 'EXCLUDED')

      let totalHours = 0
      for (const b of scheduled) {
        if (b.startTime && b.endTime) {
          const s = new Date(b.startTime).getTime()
          const e = new Date(b.endTime).getTime()
          if (!Number.isNaN(s) && !Number.isNaN(e) && e > s) {
            totalHours += (e - s) / (1000 * 60 * 60)
          }
        }
      }

      const sortedSittings = [...scheduled].sort((a, b) =>
        (a.startTime ?? '').localeCompare(b.startTime ?? '')
      )

      const primaryReason = blocksForTask[0]?.reason ?? ''
      const parsed = parseReason(primaryReason)

      const overallDecision =
        delayed.length > 0 && scheduled.length === 0
          ? 'DELAYED'
          : excluded.length > 0 && scheduled.length === 0
          ? 'EXCLUDED'
          : 'SCHEDULED'

      return {
        taskId,
        task,
        overallDecision,
        totalHours,
        sittingsCount: scheduled.length,
        sittings: sortedSittings,
        delayedBlocks: delayed,
        parsed,
      }
    })
  }, [explainedBlocks, taskById])

  const statusGroups = useMemo(() => {
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

      <Card size="sm" className="gap-0 py-0">
        <div className="grid lg:grid-cols-[13rem_minmax(0,1fr)]">
          <aside className="border-b p-4 lg:border-r lg:border-b-0">
            <CardHeader className="px-0 pt-0 flex flex-row items-center justify-between">
              <CardTitle>Tasks</CardTitle>
              {projectId ? (
                <Link
                  to={`/generate?projectId=${projectId}`}
                  className="text-xs text-primary hover:underline font-normal"
                  title="Add or manage tasks"
                >
                  + Manage
                </Link>
              ) : null}
            </CardHeader>
            <CardContent className="px-0">
              <ul className="flex flex-col gap-3">
                {statusGroups.map((group) => (
                  <li key={group.label}>
                    <div className="mb-1 flex items-center gap-2">
                      <p className="text-xs font-medium text-muted-foreground">{group.label}</p>
                      {group.items.length > 0 ? (
                        <Badge variant="secondary">{group.items.length}</Badge>
                      ) : null}
                    </div>
                    {group.items.length === 0 ? (
                      <p className="text-sm text-muted-foreground/60">—</p>
                    ) : (
                      <ul className="flex flex-col gap-1">
                        {group.items.map((item) => {
                          const task = taskById.get(item.id)
                          const taskAssignee = task?.assigneeName || task?.assigneeEmail
                          return (
                            <li
                              key={item.id}
                              className="group flex items-center justify-between gap-1.5 rounded px-1.5 py-1 hover:bg-muted/60 text-sm"
                            >
                              <div className="flex flex-col min-w-0">
                                <span className="truncate" title={item.title}>
                                  {item.title}
                                </span>
                                {taskAssignee ? (
                                  <span className="flex items-center gap-1 text-[0.68rem] text-muted-foreground">
                                    <UserIcon size={10} className="shrink-0 text-primary/80" />
                                    <span className="truncate">{taskAssignee}</span>
                                  </span>
                                ) : null}
                              </div>
                              {onDeleteTask ? (
                                <button
                                  type="button"
                                  onClick={(e) => {
                                    e.stopPropagation()
                                    if (window.confirm(`Delete task "${item.title}"?`)) {
                                      onDeleteTask(item.id)
                                    }
                                  }}
                                  className="opacity-0 group-hover:opacity-100 transition-opacity p-0.5 text-muted-foreground hover:text-destructive rounded shrink-0"
                                  title={`Delete ${item.title}`}
                                  aria-label={`Delete task ${item.title}`}
                                >
                                  <TrashIcon size={14} />
                                </button>
                              ) : null}
                            </li>
                          )
                        })}
                      </ul>
                    )}
                  </li>
                ))}
              </ul>
            </CardContent>
          </aside>

          <div className="overflow-x-auto p-3">
            {assignees.length > 0 ? (
              <div className="mb-2.5 flex flex-wrap items-center gap-1.5 text-xs">
                <span className="text-muted-foreground font-medium flex items-center gap-1">
                  <UserIcon size={12} /> Assignee:
                </span>
                <button
                  type="button"
                  onClick={() => setAssigneeFilter('ALL')}
                  className={cn(
                    'rounded-full px-2.5 py-0.5 text-xs transition-colors',
                    assigneeFilter === 'ALL'
                      ? 'bg-primary text-primary-foreground font-semibold'
                      : 'bg-muted text-muted-foreground hover:bg-muted/80'
                  )}
                >
                  All ({scheduled.length})
                </button>
                {assignees.map((name) => {
                  const count = scheduled.filter((b) => {
                    const t = taskById.get(b.taskId)
                    const p = parseReason(b.reason)
                    return (t?.assigneeName || p.scheduledMemberName || t?.assigneeEmail) === name
                  }).length
                  return (
                    <button
                      key={name}
                      type="button"
                      onClick={() => setAssigneeFilter((prev) => (prev === name ? 'ALL' : name))}
                      className={cn(
                        'rounded-full px-2.5 py-0.5 text-xs transition-colors flex items-center gap-1',
                        assigneeFilter === name
                          ? 'bg-primary text-primary-foreground font-semibold'
                          : 'bg-muted text-muted-foreground hover:bg-muted/80'
                      )}
                    >
                      <UserIcon size={10} />
                      <span>{name}</span>
                      <span className="opacity-75 text-[0.65rem]">({count})</span>
                    </button>
                  )
                })}
              </div>
            ) : null}

            {weeks.length > 1 ? (
              <div className="mb-3 flex items-center justify-between rounded-md border bg-muted/30 px-3 py-2">
                <span className="text-xs font-semibold text-foreground">
                  Week {activeWeekIndex + 1} of {weeks.length}:{' '}
                  <span className="font-normal text-muted-foreground">
                    {new Date(`${days[0]}T12:00:00`).toLocaleDateString(undefined, {
                      month: 'short',
                      day: 'numeric',
                    })}{' '}
                    –{' '}
                    {new Date(`${days[days.length - 1]}T12:00:00`).toLocaleDateString(undefined, {
                      month: 'short',
                      day: 'numeric',
                    })}
                  </span>
                </span>
                <div className="flex items-center gap-1.5">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-7 px-2.5 text-xs"
                    disabled={activeWeekIndex === 0}
                    onClick={() => setWeekIndex((i) => Math.max(0, i - 1))}
                  >
                    Previous week
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-7 px-2.5 text-xs"
                    disabled={activeWeekIndex >= weeks.length - 1}
                    onClick={() => setWeekIndex((i) => Math.min(weeks.length - 1, i + 1))}
                  >
                    Next week
                  </Button>
                </div>
              </div>
            ) : null}
            <div
              className="grid min-w-[36rem] gap-px rounded-md border bg-border"
              style={{
                gridTemplateColumns: `3.5rem repeat(${days.length}, minmax(0, 1fr))`,
                gridTemplateRows: `auto repeat(${hours.length}, minmax(3.5rem, 1fr))`,
              }}
            >
              <div className="bg-card p-2 text-xs text-muted-foreground" />
              {days.map((day, dayIndex) => (
                <div
                  key={day}
                  className="bg-card p-2 text-center text-xs font-medium text-muted-foreground"
                  style={{ gridColumn: dayIndex + 2, gridRow: 1 }}
                >
                  {new Date(`${day}T12:00:00`).toLocaleDateString(undefined, {
                    weekday: 'short',
                    day: 'numeric',
                  })}
                </div>
              ))}

              {hours.map((hour, hourIndex) => (
                <div
                  key={`label-${hour}`}
                  className="bg-card px-2 py-3 text-xs text-muted-foreground"
                  style={{ gridColumn: 1, gridRow: hourIndex + 2 }}
                >
                  {formatHour(hour)}
                </div>
              ))}

              {hours.map((hour, hourIndex) =>
                days.map((day, dayIndex) => (
                  <div
                    key={`${day}-${hour}`}
                    className="min-h-14 bg-background"
                    style={{ gridColumn: dayIndex + 2, gridRow: hourIndex + 2 }}
                  />
                )),
              )}

              {scheduled.map((b) => {
                const range = occupiedHourRange(b.startTime, b.endTime)
                const day = dayKey(b.startTime)
                if (!range || !day) return null
                const dayIndex = days.indexOf(day)
                if (dayIndex < 0 || hours.length === 0) return null
                const first = Math.max(range.startHour, hours[0])
                const last = Math.min(range.endHourExclusive - 1, hours[hours.length - 1])
                if (last < hours[0] || first > hours[hours.length - 1]) return null
                const startIndex = hours.indexOf(first)
                const endIndex = hours.indexOf(last)
                if (startIndex < 0 || endIndex < 0) return null
                const span = Math.max(1, endIndex - startIndex + 1)
                const task = taskById.get(b.taskId)
                const sitting = sittingByBlockId.get(b.id)
                const label = task?.title ?? 'Block'
                const sittingLabel =
                  sitting && sitting.m > 1 ? `${label} ${sitting.n}/${sitting.m}` : label
                const parsed = parseReason(b.reason)
                const assigneeDisplay = task?.assigneeName || parsed.scheduledMemberName || task?.assigneeEmail
                const isMatch = assigneeFilter === 'ALL' || assigneeDisplay === assigneeFilter
                const tip = [
                  sittingLabel,
                  assigneeDisplay ? `Assigned to: ${assigneeDisplay}` : null,
                  b.reason,
                ].filter(Boolean).join(' — ')
                const color = palette[(colorByTaskId.get(b.taskId) ?? 0) % palette.length]
                const isUnattended = b.reason?.toLowerCase().startsWith('unattended session')
                return (
                  <div
                    key={b.id}
                    className={cn(
                      'z-[1] m-0.5 overflow-hidden rounded-sm px-1.5 py-1 text-xs transition-opacity',
                      color,
                      !isMatch && 'opacity-20 hover:opacity-85',
                      isUnattended && 'ring-2 ring-amber-500/70'
                    )}
                    style={{
                      gridColumn: dayIndex + 2,
                      gridRow: `${startIndex + 2} / span ${span}`,
                    }}
                    title={tip}
                  >
                    <div className="flex items-center justify-between gap-1">
                      <p className="truncate font-semibold">{sittingLabel}</p>
                      <div className="flex items-center gap-1 shrink-0">
                        {isUnattended ? (
                          <span
                            className="inline-flex shrink-0 items-center rounded bg-amber-500/90 px-1 py-0.5 text-[0.6rem] font-bold text-white uppercase tracking-wider"
                            title="Unattended / missed session"
                          >
                            Missed
                          </span>
                        ) : null}
                        {assigneeDisplay ? (
                          <span
                            className="inline-flex shrink-0 items-center gap-0.5 rounded bg-black/30 backdrop-blur-xs px-1 py-0.5 text-[0.62rem] font-medium text-white/95"
                            title={`Assigned to ${assigneeDisplay}`}
                          >
                            <UserIcon size={10} weight="bold" />
                            <span className="max-w-[4rem] truncate">{assigneeDisplay.split(' ')[0]}</span>
                          </span>
                        ) : null}
                      </div>
                    </div>
                    {assigneeDisplay && span > 1 ? (
                      <p className="mt-0.5 flex items-center gap-1 text-[0.68rem] text-white/90">
                        <span className="truncate opacity-95">👤 {assigneeDisplay}</span>
                      </p>
                    ) : null}
                    {b.reason ? (
                      <p className="mt-0.5 line-clamp-2 text-[0.65rem] opacity-85">
                        {b.reason}
                      </p>
                    ) : null}
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      </Card>

      {explainedBlocks.length > 0 ? (
        <Card size="sm">
          <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <CardTitle>Schedule decisions</CardTitle>
              <p className="text-xs text-muted-foreground mt-0.5">
                Rationales, priority scores, and pacing policies for scheduled tasks
              </p>
            </div>
            <div className="flex items-center gap-1">
              <Button
                type="button"
                variant={decisionTab === 'byTask' ? 'secondary' : 'ghost'}
                size="sm"
                className="h-7 px-2.5 text-xs font-medium"
                onClick={() => setDecisionTab('byTask')}
              >
                Grouped by task ({taskDecisions.length})
              </Button>
              <Button
                type="button"
                variant={decisionTab === 'bySitting' ? 'secondary' : 'ghost'}
                size="sm"
                className="h-7 px-2.5 text-xs font-medium"
                onClick={() => setDecisionTab('bySitting')}
              >
                All sittings ({explainedBlocks.length})
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {decisionTab === 'byTask' ? (
              <div className="flex flex-col gap-3">
                {taskDecisions.map((item) => (
                  <div
                    key={item.taskId}
                    className="rounded-lg border bg-card p-3.5 transition-colors hover:border-primary/30"
                  >
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                      <div className="flex flex-col gap-1.5">
                        <div className="flex flex-wrap items-center gap-2">
                          <DecisionBadge decision={item.overallDecision as any} />
                          <span className="text-sm font-bold text-foreground">
                            {item.task?.title ?? item.taskId}
                          </span>
                          {item.task?.category ? (
                            <Badge variant="outline" className="text-xs px-1.5 py-0">
                              {item.task.category}
                            </Badge>
                          ) : null}
                          {item.task?.assigneeName || item.task?.assigneeEmail ? (
                            <Badge variant="outline" className="text-xs px-1.5 py-0 gap-1 border-primary/30 text-primary font-normal">
                              <UserIcon size={11} />
                              {item.task.assigneeName || item.task.assigneeEmail}
                            </Badge>
                          ) : null}
                        </div>

                        <div className="flex flex-wrap items-center gap-1.5 text-xs">
                          {item.task?.priority ? (
                            <Badge variant="secondary" className="font-normal text-[0.7rem] px-1.5 py-0">
                              Priority {item.task.priority}
                            </Badge>
                          ) : null}
                          {item.task?.complexity ? (
                            <Badge variant="secondary" className="font-normal text-[0.7rem] px-1.5 py-0">
                              {complexityLabel(item.task.complexity)}
                            </Badge>
                          ) : null}
                          {item.sittingsCount > 0 ? (
                            <Badge variant="secondary" className="font-normal text-[0.7rem] px-1.5 py-0">
                              {item.sittingsCount} {item.sittingsCount === 1 ? 'sitting' : 'sittings'} ({item.totalHours.toFixed(1)}h total)
                            </Badge>
                          ) : null}
                          {item.parsed.deadlineStr ? (
                            <span className="text-muted-foreground text-[0.75rem]">
                              Due {item.parsed.deadlineStr}
                            </span>
                          ) : null}
                        </div>
                      </div>

                      {item.parsed.score ? (
                        <div className="flex items-center gap-1.5 rounded-md border bg-muted/40 px-2.5 py-1 text-xs shrink-0">
                          <span className="text-muted-foreground font-medium">Priority score:</span>
                          <span className="font-bold text-foreground">{item.parsed.score}</span>
                        </div>
                      ) : null}
                    </div>

                    {/* Policy / Explanation Notes */}
                    <div className="mt-3 rounded-md bg-muted/30 p-2.5 text-xs text-muted-foreground leading-relaxed flex flex-col gap-1">
                      {item.parsed.sessionCap ? (
                        <div className="flex items-center gap-2 text-foreground/90 font-medium">
                          <span className="size-1.5 rounded-full bg-primary shrink-0" />
                          <span>Pacing policy: {item.parsed.sessionCap}</span>
                        </div>
                      ) : null}
                      {item.parsed.decomposedNote ? (
                        <div className="flex items-center gap-2 text-foreground/90">
                          <span className="size-1.5 rounded-full bg-primary shrink-0" />
                          <span>{item.parsed.decomposedNote}</span>
                        </div>
                      ) : null}
                      {item.delayedBlocks.length > 0 ? (
                        item.delayedBlocks[0].reason?.toLowerCase().includes("you can't do anything anymore") ? (
                          <div className="mt-1 rounded-md border border-destructive/40 bg-destructive/15 p-2.5 text-xs text-destructive flex items-start gap-2">
                            <XCircleIcon size={16} className="shrink-0 mt-0.5" />
                            <div className="flex flex-col gap-0.5">
                              <span className="font-bold">Deadline Approached — Cannot Complete Task:</span>
                              <span className="text-destructive/90">{item.delayedBlocks[0].reason}</span>
                            </div>
                          </div>
                        ) : (
                          <div className="text-destructive font-medium flex items-center gap-2">
                            <span className="size-1.5 rounded-full bg-destructive shrink-0" />
                            <span>{item.delayedBlocks[0].reason}</span>
                          </div>
                        )
                      ) : null}
                    </div>

                    {/* Sittings breakdown chips */}
                    {item.sittings.length > 0 ? (
                      <div className="mt-3 border-t border-border/50 pt-2.5">
                        <p className="mb-2 text-xs font-medium text-muted-foreground">
                          Scheduled sittings:
                        </p>
                        <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                          {item.sittings.map((sitting, idx) => (
                            <div
                              key={sitting.id}
                              className="flex flex-col gap-0.5 rounded-md border bg-background/80 p-2 text-xs"
                            >
                              <span className="font-semibold text-foreground">
                                Sitting {idx + 1} of {item.sittings.length}
                              </span>
                              <span className="text-muted-foreground">
                                {formatSittingDateTime(sitting.startTime, sitting.endTime)}
                              </span>
                            </div>
                          ))}
                        </div>
                      </div>
                    ) : null}
                  </div>
                ))}
              </div>
            ) : (
              <ul className="flex flex-col gap-3">
                {explainedBlocks.map((b) => {
                  const task = taskById.get(b.taskId)
                  const parsed = parseReason(b.reason)
                  return (
                    <li
                      key={b.id}
                      className="flex flex-col gap-1.5 border-b border-border/60 pb-3 last:border-b-0 last:pb-0"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <div className="flex flex-wrap items-center gap-2">
                          <DecisionBadge decision={b.decision} />
                          <span className="text-sm font-semibold text-foreground">
                            {task?.title ?? b.taskId}
                          </span>
                          {b.startTime && b.endTime ? (
                            <span className="text-xs text-muted-foreground font-medium">
                              {formatSittingDateTime(b.startTime, b.endTime)}
                            </span>
                          ) : null}
                          {task?.assigneeName || parsed.scheduledMemberName || task?.assigneeEmail ? (
                            <Badge variant="outline" className="text-xs px-1.5 py-0 gap-1 border-primary/30 text-primary font-normal">
                              <UserIcon size={11} />
                              {task?.assigneeName || parsed.scheduledMemberName || task?.assigneeEmail}
                            </Badge>
                          ) : null}
                          {b.reason?.toLowerCase().startsWith('unattended session') ? (
                            <Badge variant="outline" className="text-xs px-1.5 py-0 border-amber-500/40 text-amber-600 dark:text-amber-400 font-semibold">
                              Missed Session
                            </Badge>
                          ) : null}
                        </div>
                        <div className="flex items-center gap-2">
                          {onMarkUnattended && b.decision === 'SCHEDULED' && !b.reason?.toLowerCase().startsWith('unattended session') ? (
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              className="h-6 px-2 text-[0.68rem] text-amber-600 dark:text-amber-400 hover:text-amber-700 hover:bg-amber-500/10 border-amber-500/30"
                              onClick={() => void onMarkUnattended(b.id)}
                              title="Mark session as unattended and rearrange remaining schedule"
                            >
                              Mark Unattended
                            </Button>
                          ) : null}
                          {parsed.score ? (
                            <Badge variant="outline" className="text-xs">
                              Score {parsed.score}
                            </Badge>
                          ) : null}
                        </div>
                      </div>

                      <div className="flex flex-wrap items-center gap-2 text-xs">
                        {parsed.sessionCap ? (
                          <Badge variant="secondary" className="font-normal text-[0.7rem]">
                            {parsed.sessionCap}
                          </Badge>
                        ) : null}
                        {parsed.decomposedNote ? (
                          <span className="text-muted-foreground">
                            {parsed.decomposedNote}
                          </span>
                        ) : null}
                        {parsed.deadlineStr ? (
                          <span className="text-muted-foreground">
                            Due {parsed.deadlineStr}
                          </span>
                        ) : null}
                        {!parsed.sessionCap && !parsed.decomposedNote ? (
                          <span className="text-muted-foreground">
                            {b.reason?.trim() || 'No explanation provided.'}
                          </span>
                        ) : null}
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </CardContent>
        </Card>
      ) : null}
    </div>
  )
}
