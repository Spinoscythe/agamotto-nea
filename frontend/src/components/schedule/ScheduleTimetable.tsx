import { Link } from 'react-router-dom'
import { TrashIcon } from '@phosphor-icons/react'
import { CalendarX2, CheckCircle2 } from 'lucide-react'
import type { LeaveResponse, ScheduleBlockResponse, TaskResponse } from '@/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { occupiedHourRange } from '@/lib/datetime'
import { cn } from '@/lib/utils'
import {
  dayKey,
  formatHour,
  getLeaveEmoji,
  palette,
  type StatusGroup,
} from './preview-utils'

interface ScheduleTimetableProps {
  scheduled: ScheduleBlockResponse[]
  taskById: Map<string, TaskResponse>
  weeks: string[][]
  days: string[]
  activeWeekIndex: number
  onWeekChange: (index: number) => void
  hours: number[]
  leaves: LeaveResponse[]
  leavesOnDay: Map<string, LeaveResponse[]>
  colorByTaskId: Map<string, number>
  sittingByBlockId: Map<string, { n: number; m: number }>
  statusGroups: StatusGroup[]
  projectId?: string
  onDeleteTask?: (taskId: string) => void
  onSelectBlock?: (blockId: string) => void
}

export function ScheduleTimetable({
  scheduled,
  taskById,
  weeks,
  days,
  activeWeekIndex,
  onWeekChange,
  hours,
  leaves,
  leavesOnDay,
  colorByTaskId,
  sittingByBlockId,
  statusGroups,
  projectId,
  onDeleteTask,
  onSelectBlock,
}: ScheduleTimetableProps) {
  return (
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
                        return (
                          <li
                            key={item.id}
                            className="group flex items-center justify-between gap-1.5 rounded px-1.5 py-1 hover:bg-muted/60 text-sm"
                          >
                            <div className="flex flex-col min-w-0">
                              <span className="truncate" title={item.title}>
                                {item.title}
                              </span>
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
            <div className="mt-4 border-t pt-3">
              <Link
                to="/leaves"
                className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground"
                title="Manage your leaves and breaks"
              >
                <CalendarX2 className="size-3.5 text-amber-500" />
                <span>Leaves & Breaks</span>
              </Link>
            </div>
          </CardContent>
        </aside>

        <div className="overflow-x-auto p-3">
          {leaves.length > 0 ? (
            <div className="mb-2.5 flex items-center justify-between gap-2 rounded-md border border-amber-500/30 bg-amber-500/10 px-3 py-1.5 text-xs text-amber-700 dark:text-amber-300">
              <span className="flex items-center gap-1.5 font-medium">
                <span>🌴</span>
                <span>{leaves.length} planned leave / break{leaves.length > 1 ? 's' : ''} active during this schedule window.</span>
              </span>
              <Link to="/leaves" className="text-[0.7rem] font-semibold underline hover:text-amber-800 dark:hover:text-amber-200">
                View all
              </Link>
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
                  onClick={() => onWeekChange(Math.max(0, activeWeekIndex - 1))}
                >
                  Previous week
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="h-7 px-2.5 text-xs"
                  disabled={activeWeekIndex >= weeks.length - 1}
                  onClick={() => onWeekChange(Math.min(weeks.length - 1, activeWeekIndex + 1))}
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
            {days.map((day, dayIndex) => {
              const dayLeaves = leavesOnDay.get(day) ?? []
              return (
                <div
                  key={day}
                  className={cn(
                    'bg-card p-2 text-center text-xs font-medium text-muted-foreground',
                    dayLeaves.length > 0 && 'bg-amber-500/10 dark:bg-amber-950/20 text-amber-700 dark:text-amber-400'
                  )}
                  style={{ gridColumn: dayIndex + 2, gridRow: 1 }}
                >
                  <div>
                    {new Date(`${day}T12:00:00`).toLocaleDateString(undefined, {
                      weekday: 'short',
                      day: 'numeric',
                    })}
                  </div>
                  {dayLeaves.length > 0 ? (
                    <div className="mt-0.5 truncate text-[0.65rem] font-semibold">
                      {getLeaveEmoji(dayLeaves[0].type)} {dayLeaves[0].type.replace('_', ' ')}
                    </div>
                  ) : null}
                </div>
              )
            })}

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
              const tip = [
                sittingLabel,
                b.startTime && b.endTime
                  ? `${b.startTime.slice(11, 16)} – ${b.endTime.slice(11, 16)}`
                  : null,
              ].filter(Boolean).join(' — ')
              const color = palette[(colorByTaskId.get(b.taskId) ?? 0) % palette.length]
              const isUnattended = b.reason?.toLowerCase().startsWith('unattended session')
              const timeRange = b.startTime && b.endTime
                ? `${b.startTime.slice(11, 16)} – ${b.endTime.slice(11, 16)}`
                : null
              return (
                <div
                  key={b.id}
                  role={onSelectBlock ? 'button' : undefined}
                  tabIndex={onSelectBlock ? 0 : undefined}
                  aria-label={onSelectBlock ? `Open details for ${sittingLabel}` : undefined}
                  onClick={onSelectBlock ? () => onSelectBlock(b.id) : undefined}
                  onKeyDown={
                    onSelectBlock
                      ? (event) => {
                          if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault()
                            onSelectBlock(b.id)
                          }
                        }
                      : undefined
                  }
                  className={cn(
                    'z-[1] m-0.5 overflow-hidden rounded-sm px-1.5 py-1 text-xs transition-opacity',
                    color,
                    onSelectBlock && 'cursor-pointer hover:brightness-110 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-foreground',
                    b.completed && 'opacity-60',
                    isUnattended && 'ring-2 ring-amber-500/70'
                  )}
                  style={{
                    gridColumn: dayIndex + 2,
                    gridRow: `${startIndex + 2} / span ${span}`,
                  }}
                  title={tip}
                >
                  <div className="flex items-center justify-between gap-1">
                    <p className={cn('truncate font-semibold', b.completed && 'line-through')}>
                      {sittingLabel}
                    </p>
                    <div className="flex items-center gap-1 shrink-0">
                      {b.completed ? (
                        <CheckCircle2 className="size-3.5 shrink-0" aria-label="Completed" />
                      ) : null}
                      {isUnattended ? (
                        <span
                          className="inline-flex shrink-0 items-center rounded bg-amber-500/90 px-1 py-0.5 text-[0.6rem] font-bold text-white uppercase tracking-wider"
                          title="Unattended / missed session"
                        >
                          Missed
                        </span>
                      ) : null}
                    </div>
                  </div>
                  {timeRange ? (
                    <p className="mt-0.5 truncate text-[0.65rem] font-medium opacity-85">
                      {timeRange}
                    </p>
                  ) : null}
                </div>
              )
            })}

            {days.flatMap((day, dayIndex) => {
              const dayLeaves = leavesOnDay.get(day) ?? []
              return dayLeaves.map((l) => {
                const lStart = new Date(l.startTime)
                const lEnd = new Date(l.endTime)
                const startHour = l.startTime.startsWith(day) ? lStart.getHours() : 0
                const endHour = l.endTime.startsWith(day)
                  ? Math.max(startHour + 1, Math.ceil(lEnd.getHours() + (lEnd.getMinutes() > 0 ? 1 : 0)))
                  : 24

                const first = Math.max(startHour, hours[0])
                const last = Math.min(endHour - 1, hours[hours.length - 1])
                if (last < hours[0] || first > hours[hours.length - 1]) return null

                const startIndex = hours.indexOf(first)
                const endIndex = hours.indexOf(last)
                if (startIndex < 0 || endIndex < 0) return null
                const span = Math.max(1, endIndex - startIndex + 1)

                return (
                  <div
                    key={`leave-${l.id}-${day}`}
                    className="z-[2] m-0.5 flex flex-col justify-between overflow-hidden rounded-sm border border-dashed border-amber-500/70 bg-amber-500/20 p-1.5 text-xs text-foreground backdrop-blur-xs select-none"
                    style={{
                      gridColumn: dayIndex + 2,
                      gridRow: `${startIndex + 2} / span ${span}`,
                    }}
                    title={`${l.title} (${l.type.replace('_', ' ')}) — ${l.reason || 'Protected absence'}`}
                  >
                    <div className="flex items-center gap-1 font-semibold text-amber-800 dark:text-amber-300">
                      <span>{getLeaveEmoji(l.type)}</span>
                      <span className="truncate">{l.title}</span>
                    </div>
                    <div className="flex items-center justify-between text-[0.65rem] text-muted-foreground font-medium">
                      <span className="truncate uppercase tracking-wider">{l.type.replace('_', ' ')}</span>
                      <span className="rounded bg-amber-500/20 px-1 py-0.5 font-mono text-[0.6rem] text-amber-700 dark:text-amber-300 font-bold">
                        BLOCKED
                      </span>
                    </div>
                  </div>
                )
              })
            })}
          </div>
        </div>
      </div>
    </Card>
  )
}
