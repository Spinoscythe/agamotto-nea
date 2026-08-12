import { useMemo } from 'react'
import type { ScheduleBlockResponse, TaskResponse } from '@/api'
import { DecisionBadge } from '@/components/DecisionBadge'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { cn } from '@/lib/utils'

const HOURS = [9, 10, 11, 12, 13, 14]

function dayKey(iso: string | null): string | null {
  if (!iso) return null
  return iso.slice(0, 10)
}

function hourOf(iso: string | null): number | null {
  if (!iso || iso.length < 13) return null
  return Number(iso.slice(11, 13))
}

const palette = [
  'bg-chart-1/80 text-white',
  'bg-chart-2/80 text-white',
  'bg-chart-3/80 text-white',
  'bg-chart-4/80 text-foreground',
]

export function ScheduleWeekPreview({
  blocks,
  tasks,
  startDate,
  endDate,
}: {
  blocks: ScheduleBlockResponse[]
  tasks: TaskResponse[]
  startDate: string
  endDate: string
}) {
  const taskById = useMemo(() => {
    const map = new Map<string, TaskResponse>()
    for (const t of tasks) map.set(t.id, t)
    return map
  }, [tasks])

  const days = useMemo(() => {
    const list: string[] = []
    const cursor = new Date(`${startDate}T12:00:00`)
    const end = new Date(`${endDate}T12:00:00`)
    let guard = 0
    while (cursor <= end && guard < 7) {
      list.push(cursor.toISOString().slice(0, 10))
      cursor.setDate(cursor.getDate() + 1)
      guard += 1
    }
    return list.length > 0 ? list : [startDate]
  }, [startDate, endDate])

  const scheduled = blocks.filter((b) => b.decision === 'SCHEDULED' && b.startTime)
  const explainedBlocks = useMemo(
    () =>
      blocks.filter(
        (b) =>
          Boolean(b.reason?.trim()) ||
          b.decision === 'DELAYED' ||
          b.decision === 'EXCLUDED',
      ),
    [blocks],
  )

  const statusGroups = useMemo(() => {
    const overdue: string[] = []
    const today: string[] = []
    const upcoming: string[] = []
    const notStarted: string[] = []
    const inProgress: string[] = []
    const done: string[] = []
    const archived: string[] = []
    const now = new Date().toISOString().slice(0, 10)

    for (const t of tasks) {
      if (t.status === 'COMPLETED') done.push(t.title)
      else if (t.status === 'CANCELLED') archived.push(t.title)
      else if (t.status === 'IN_PROGRESS') inProgress.push(t.title)
      else if (t.deadline.slice(0, 10) < now) overdue.push(t.title)
      else if (t.deadline.slice(0, 10) === now) today.push(t.title)
      else if (t.status === 'PENDING') {
        notStarted.push(t.title)
        upcoming.push(t.title)
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
  }, [tasks])

  if (scheduled.length === 0 && tasks.length === 0) {
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
                        {group.items.slice(0, 3).map((title) => (
                          <li key={title} className="truncate text-sm">
                            {title}
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
              style={{ gridTemplateColumns: `3.5rem repeat(${days.length}, minmax(0, 1fr))` }}
            >
              <div className="bg-card p-2 text-xs text-muted-foreground" />
              {days.map((day) => (
                <div
                  key={day}
                  className="bg-card p-2 text-center text-xs font-medium text-muted-foreground"
                >
                  {new Date(`${day}T12:00:00`).toLocaleDateString(undefined, {
                    weekday: 'short',
                    day: 'numeric',
                  })}
                </div>
              ))}

              {HOURS.map((hour) => (
                <div key={`row-${hour}`} className="contents">
                  <div className="bg-card px-2 py-3 text-xs text-muted-foreground">
                    {hour > 12 ? `${hour - 12} PM` : hour === 12 ? '12 PM' : `${hour} AM`}
                  </div>
                  {days.map((day, dayIndex) => {
                    const cellBlocks = scheduled.filter((b) => {
                      return dayKey(b.startTime) === day && hourOf(b.startTime) === hour
                    })
                    return (
                      <div key={`${day}-${hour}`} className="min-h-14 bg-background p-1">
                        {cellBlocks.map((b, i) => {
                          const task = taskById.get(b.taskId)
                          const label = task?.title ?? 'Block'
                          const tip = b.reason ? `${label} — ${b.reason}` : label
                          return (
                            <div
                              key={b.id}
                              className={cn(
                                'mb-1 rounded-sm px-1.5 py-1 text-xs',
                                palette[(dayIndex + i) % palette.length],
                              )}
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
                    )
                  })}
                </div>
              ))}
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
