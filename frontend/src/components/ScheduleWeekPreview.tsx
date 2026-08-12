import { useMemo } from 'react'
import type { ScheduleBlockResponse, TaskResponse } from '@/api'
import { DecisionBadge } from '@/components/DecisionBadge'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import {
  deadlineDatePart,
  eachDateInclusive,
  occupiedHourRange,
  toDateInputValue,
} from '@/lib/datetime'
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
}: {
  blocks?: ScheduleBlockResponse[] | null
  tasks?: TaskResponse[] | null
  startDate: string
  endDate: string
}) {
  const safeBlocks = blocks ?? []
  const safeTasks = tasks ?? []
  const taskById = useMemo(() => {
    const map = new Map<string, TaskResponse>()
    for (const t of safeTasks) map.set(t.id, t)
    return map
  }, [safeTasks])

  const days = useMemo(
    () => eachDateInclusive(startDate, endDate, 7),
    [startDate, endDate],
  )

  const scheduled = safeBlocks.filter((b) => b.decision === 'SCHEDULED' && b.startTime)

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
      <Card size="sm" className="gap-0 py-0">
        <div className="grid lg:grid-cols-[13rem_minmax(0,1fr)]">
          <aside className="border-b p-4 lg:border-r lg:border-b-0">
            <CardHeader className="px-0 pt-0">
              <CardTitle>Tasks</CardTitle>
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
                      <ul className="flex flex-col gap-0.5">
                        {group.items.slice(0, 3).map((item) => (
                          <li key={item.id} className="truncate text-sm">
                            {item.title}
                          </li>
                        ))}
                        {group.items.length > 3 ? (
                          <li className="text-xs text-muted-foreground">
                            +{group.items.length - 3} more
                          </li>
                        ) : null}
                      </ul>
                    )}
                  </li>
                ))}
              </ul>
            </CardContent>
          </aside>

          <div className="overflow-x-auto p-3">
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

              {scheduled.map((b, i) => {
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
                const label = task?.title ?? 'Block'
                const tip = b.reason ? `${label} — ${b.reason}` : label
                return (
                  <div
                    key={b.id}
                    className={cn(
                      'z-[1] m-0.5 overflow-hidden rounded-sm px-1.5 py-1 text-xs',
                      palette[(dayIndex + i) % palette.length],
                    )}
                    style={{
                      gridColumn: dayIndex + 2,
                      gridRow: `${startIndex + 2} / span ${span}`,
                    }}
                    title={tip}
                  >
                    <p className="truncate font-medium">{label}</p>
                    {b.reason ? (
                      <p className="mt-0.5 line-clamp-2 text-[0.65rem] opacity-90">
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
          <CardHeader>
            <CardTitle>Schedule decisions</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="flex flex-col gap-3">
              {explainedBlocks.map((b) => {
                const task = taskById.get(b.taskId)
                return (
                  <li
                    key={b.id}
                    className="flex flex-col gap-1 border-b border-border/60 pb-3 last:border-b-0 last:pb-0"
                  >
                    <div className="flex flex-wrap items-center gap-2">
                      <DecisionBadge decision={b.decision} />
                      <span className="text-sm font-medium">
                        {task?.title ?? b.taskId}
                      </span>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {b.reason?.trim() || 'No explanation provided.'}
                    </p>
                  </li>
                )
              })}
            </ul>
          </CardContent>
        </Card>
      ) : null}
    </div>
  )
}
