import { useEffect, useState, type FormEvent } from 'react'
import { CheckCircle2, X } from 'lucide-react'
import type { ScheduleBlockResponse, TaskResponse } from '@/api'
import { DecisionBadge } from '@/components/DecisionBadge'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { complexityLabel } from '@/lib/complexity'
import { deadlineDatePart, toLocalDateTimePayload } from '@/lib/datetime'
import { formatDateTime } from '@/lib/schedule-format'

export interface ScheduleBlockSaveValues {
  startTime?: string
  endTime?: string
  completed: boolean
}

interface ScheduleBlockSidebarProps {
  block: ScheduleBlockResponse | null
  task: TaskResponse | null
  sitting?: { n: number; m: number } | null
  scheduleStartDate?: string | null
  scheduleEndDate?: string | null
  busy?: boolean
  onClose: () => void
  onSave: (blockId: string, values: ScheduleBlockSaveValues) => void | Promise<void>
}

export function ScheduleBlockSidebar({
  block,
  task,
  sitting,
  scheduleStartDate,
  scheduleEndDate,
  busy = false,
  onClose,
  onSave,
}: ScheduleBlockSidebarProps) {
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [completed, setCompleted] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!block) return
    setStart(block.startTime ? block.startTime.slice(0, 16) : '')
    setEnd(block.endTime ? block.endTime.slice(0, 16) : '')
    setCompleted(block.completed)
    setError(null)
  }, [block])

  useEffect(() => {
    if (!block) return
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [block, onClose])

  if (!block) return null

  const isScheduled = block.decision === 'SCHEDULED'
  const deadline = deadlineDatePart(task?.deadline)
  const minDateTime = scheduleStartDate ? `${scheduleStartDate}T00:00` : undefined
  const maxDateTime = scheduleEndDate ? `${scheduleEndDate}T23:59` : undefined

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!block) return
    setError(null)

    if (isScheduled) {
      if (!start || !end) {
        setError('Start and end time are required.')
        return
      }
      if (end <= start) {
        setError('End time must be after start time.')
        return
      }
    }

    try {
      await onSave(block.id, {
        startTime: isScheduled && start ? toLocalDateTimePayload(start) : undefined,
        endTime: isScheduled && end ? toLocalDateTimePayload(end) : undefined,
        completed,
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not update sitting.')
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <button
        type="button"
        aria-label="Close sitting details"
        className="absolute inset-0 cursor-default bg-black/40 backdrop-blur-[1px]"
        onClick={onClose}
      />
      <aside
        role="dialog"
        aria-modal="true"
        aria-labelledby="block-details-title"
        className="relative z-10 flex h-full w-full max-w-md flex-col border-l bg-background shadow-2xl"
      >
        <header className="flex items-start justify-between gap-3 border-b p-4">
          <div className="flex min-w-0 flex-col gap-1.5">
            <div className="flex flex-wrap items-center gap-1.5">
              <DecisionBadge decision={block.decision} />
              {sitting && sitting.m > 1 ? (
                <Badge variant="outline" className="text-[0.65rem] px-1.5 py-0">
                  Sitting {sitting.n}/{sitting.m}
                </Badge>
              ) : null}
              {completed ? (
                <Badge variant="secondary" className="gap-1 text-[0.65rem] px-1.5 py-0 font-medium text-emerald-700 dark:text-emerald-400">
                  <CheckCircle2 className="size-3" />
                  Completed
                </Badge>
              ) : null}
            </div>
            <h2 id="block-details-title" className="truncate text-lg font-semibold tracking-tight">
              {task?.title ?? 'Scheduled sitting'}
            </h2>
            {task?.category ? (
              <span className="text-xs text-muted-foreground">{task.category}</span>
            ) : null}
          </div>
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            aria-label="Close sitting details"
            onClick={onClose}
          >
            <X className="size-4" />
          </Button>
        </header>

        <form
          id="block-details-form"
          onSubmit={handleSubmit}
          className="flex flex-1 flex-col gap-5 overflow-y-auto p-4"
        >
          <section className="flex flex-col gap-1.5">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Description
            </h3>
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-foreground/90">
              {task?.description?.trim() || 'No description provided for this task.'}
            </p>
          </section>

          <section className="grid grid-cols-2 gap-3 rounded-lg border bg-muted/20 p-3 text-xs">
            <div className="flex flex-col gap-0.5">
              <span className="text-muted-foreground">Priority</span>
              <span className="font-medium text-foreground">{task?.priority ?? '—'}</span>
            </div>
            <div className="flex flex-col gap-0.5">
              <span className="text-muted-foreground">Complexity</span>
              <span className="font-medium text-foreground">
                {task?.complexity ? complexityLabel(task.complexity) : '—'}
              </span>
            </div>
            <div className="flex flex-col gap-0.5">
              <span className="text-muted-foreground">Estimate</span>
              <span className="font-medium text-foreground">
                {task ? `${task.estimatedDurationHours}h` : '—'}
              </span>
            </div>
            <div className="flex flex-col gap-0.5">
              <span className="text-muted-foreground">Deadline</span>
              <span className="font-medium text-foreground">{deadline ?? '—'}</span>
            </div>
            <div className="col-span-2 flex flex-col gap-0.5">
              <span className="text-muted-foreground">Current slot</span>
              <span className="font-medium text-foreground">
                {formatDateTime(block.startTime)} → {formatDateTime(block.endTime)}
              </span>
            </div>
          </section>

          <label className="flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition-colors hover:bg-muted/40">
            <input
              type="checkbox"
              checked={completed}
              onChange={(event) => setCompleted(event.target.checked)}
              className="mt-0.5 size-4 shrink-0 accent-primary"
            />
            <span className="flex flex-col gap-0.5">
              <span className="text-sm font-medium text-foreground">Mark this sitting as completed</span>
              <span className="text-xs text-muted-foreground">
                Tick this once you have finished this scheduled block.
              </span>
            </span>
          </label>

          {isScheduled ? (
            <section className="flex flex-col gap-3">
              <div className="flex flex-col gap-0.5">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Date & time range
                </h3>
                <p className="text-xs text-muted-foreground">
                  Adjust when this sitting starts and ends.
                </p>
              </div>
              <Field>
                <FieldLabel htmlFor="block-start">Start</FieldLabel>
                <Input
                  id="block-start"
                  type="datetime-local"
                  value={start}
                  min={minDateTime}
                  max={maxDateTime}
                  onChange={(event) => setStart(event.target.value)}
                  required
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="block-end">End</FieldLabel>
                <Input
                  id="block-end"
                  type="datetime-local"
                  value={end}
                  min={start || minDateTime}
                  max={maxDateTime}
                  onChange={(event) => setEnd(event.target.value)}
                  required
                />
              </Field>
            </section>
          ) : (
            <p className="rounded-md border bg-muted/30 p-3 text-xs text-muted-foreground">
              This sitting is {block.decision.toLowerCase()} and has no editable time range.
            </p>
          )}

          {error ? (
            <p className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive">
              {error}
            </p>
          ) : null}
        </form>

        <footer className="flex items-center justify-end gap-2 border-t p-4">
          <Button type="button" variant="outline" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button type="submit" form="block-details-form" disabled={busy}>
            {busy ? 'Saving…' : 'Save changes'}
          </Button>
        </footer>
      </aside>
    </div>
  )
}
