import {type FormEvent, useEffect, useMemo, useRef, useState} from 'react'
import {Link, useSearchParams} from 'react-router-dom'
import {TrashIcon} from '@phosphor-icons/react'
import {
  ApiError,
  type ProjectResponse,
  type ScheduleBlockResponse,
  projectsApi,
  type SchedulePlanResponse,
  schedulesApi,
  type TaskResponse,
  tasksApi,
} from '@/api'
import {useAuth} from '@/auth/AuthContext'
import {DecisionBadge} from '@/components/DecisionBadge'
import {PageHeader} from '@/components/PageHeader'
import {Alert, AlertDescription} from '@/components/ui/alert'
import {Badge} from '@/components/ui/badge'
import {Button, buttonVariants} from '@/components/ui/button'
import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle,} from '@/components/ui/card'
import {Empty, EmptyDescription, EmptyHeader, EmptyTitle,} from '@/components/ui/empty'
import {Field, FieldGroup, FieldLabel} from '@/components/ui/field'
import {Input} from '@/components/ui/input'
import {Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue,} from '@/components/ui/select'
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table'
import {deadlineDatePart, toDateInputValue, toDateTimeLocalValue, toLocalDateTimePayload} from '@/lib/datetime'
import {cn} from '@/lib/utils'

interface TaskDraft {
  title: string
  category: string
  projectType: string
  priority: number
  deadline: string
  estimatedDurationHours: number
  complexity: number
}

const emptyDraft = (): TaskDraft => {
  const deadline = new Date()
  deadline.setDate(deadline.getDate() + 7)
  deadline.setMinutes(0, 0, 0)
  return {
    title: '',
    category: 'work',
    projectType: 'General',
    priority: 3,
    deadline: toDateTimeLocalValue(deadline),
    estimatedDurationHours: 2,
    complexity: 3,
  }
}

export function GenerateSchedulePage() {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const [projects, setProjects] = useState<ProjectResponse[]>([])
  const [projectId, setProjectId] = useState(params.get('projectId') ?? '')
  const [tasks, setTasks] = useState<TaskResponse[]>([])
  const [draft, setDraft] = useState<TaskDraft>(emptyDraft)
  const [startDate, setStartDate] = useState(() => toDateInputValue())
  const [endDate, setEndDate] = useState(() => {
    const d = new Date()
    d.setDate(d.getDate() + 7)
    return toDateInputValue(d)
  })
  const [result, setResult] = useState<SchedulePlanResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [moveStart, setMoveStart] = useState('')
  const [moveEnd, setMoveEnd] = useState('')
  const [movingBlockId, setMovingBlockId] = useState('')
  const projectIdRef = useRef(projectId)
  projectIdRef.current = projectId

  const selectedProject = useMemo(
    () => projects.find((p) => p.id === projectId) ?? null,
    [projects, projectId],
  )

  const schedulableCount = useMemo(
    () => tasks.filter((t) => t.status === 'PENDING' || t.status === 'IN_PROGRESS').length,
    [tasks],
  )

  useEffect(() => {
    if (!user) return
    void projectsApi
      .list(user.id)
      .then((list) => {
        setProjects(list)
        setProjectId((current) => {
          if (current && list.some((p) => p.id === current)) return current
          return list[0]?.id ?? ''
        })
      })
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : 'Failed to load projects')
      })
  }, [user])

  useEffect(() => {
    if (!projectId) {
      setTasks([])
      return
    }
    let cancelled = false
    void tasksApi
      .listByProject(projectId)
      .then((list) => {
        if (!cancelled) {
          setTasks(list.filter((t) => t.status !== 'CANCELLED'))
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError ? err.message : 'Failed to load tasks')
      })
    return () => {
      cancelled = true
    }
  }, [projectId])

  useEffect(() => {
    if (!selectedProject) return
    const start = deadlineDatePart(selectedProject.startDate)
    const end = deadlineDatePart(selectedProject.endDate)
    if (start) setStartDate(start)
    if (end) setEndDate(end)
  }, [selectedProject])

  async function addTask(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!user || !projectId) return
    const title = draft.title.trim()
    const category = draft.category.trim()
    if (!title || !category) {
      setError('Task name and category are required.')
      return
    }
    if (!Number.isFinite(draft.priority) || draft.priority < 1 || draft.priority > 5) {
      setError('Priority must be a number from 1 to 5.')
      return
    }
    if (!Number.isFinite(draft.complexity) || draft.complexity < 1 || draft.complexity > 5) {
      setError('Complexity must be a number from 1 to 5.')
      return
    }
    if (!Number.isFinite(draft.estimatedDurationHours) || draft.estimatedDurationHours <= 0) {
      setError('Estimate hours must be greater than 0.')
      return
    }
    if (!draft.deadline) {
      setError('Deadline is required.')
      return
    }
    const deadlineMs = new Date(draft.deadline).getTime()
    if (!Number.isFinite(deadlineMs) || deadlineMs <= Date.now()) {
      setError('Deadline must be a future date and time.')
      return
    }
    setError(null)
    setBusy(true)
    const targetProjectId = projectId
    try {
      const created = await tasksApi.create(targetProjectId, {
        actorUserId: user.id,
        title,
        description: draft.projectType.trim() || undefined,
        category,
        priority: draft.priority,
        deadline: toLocalDateTimePayload(draft.deadline),
        estimatedDurationHours: draft.estimatedDurationHours,
        complexity: draft.complexity,
      })
      if (projectIdRef.current === targetProjectId) {
        setTasks((prev) => [...prev, created])
        setDraft(emptyDraft())
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create task')
    } finally {
      setBusy(false)
    }
  }

  async function removeTask(taskId: string) {
    if (!user) return
    setError(null)
    setBusy(true)
    try {
      await tasksApi.remove(taskId, user.id)
      setTasks((prev) => prev.filter((t) => t.id !== taskId))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not delete task')
    } finally {
      setBusy(false)
    }
  }

  async function generate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!projectId) return
    if (schedulableCount === 0) {
      setError('Add at least one pending task before generating.')
      return
    }
    if (!startDate || !endDate) {
      setError('Start and end dates are required.')
      return
    }
    if (endDate < startDate) {
      setError('End date must be on or after the start date.')
      return
    }
    setError(null)
    setBusy(true)
    try {
      const plan = await schedulesApi.generate(projectId, { startDate, endDate })
      setResult(plan)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Schedule generation failed')
    } finally {
      setBusy(false)
    }
  }

  function replaceBlock(updated: ScheduleBlockResponse) {
    setResult((current) => {
      if (!current) return current
      return {
        ...current,
        blocks: (current.blocks ?? []).map((b) => (b.id === updated.id ? updated : b)),
      }
    })
  }

  async function rescheduleSelected() {
    if (!movingBlockId || !moveStart || !moveEnd) {
      setError('Choose a start and end time to reschedule.')
      return
    }
    setError(null)
    setBusy(true)
    try {
      const response = await schedulesApi.rescheduleBlock(movingBlockId, {
        startTime: toLocalDateTimePayload(moveStart),
        endTime: toLocalDateTimePayload(moveEnd),
        reason: 'Moved from Generate view',
      })
      if (response.movedBlock) replaceBlock(response.movedBlock)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not reschedule block')
    } finally {
      setBusy(false)
    }
  }

  async function delayBlock(blockId: string) {
    setError(null)
    setBusy(true)
    try {
      const updated = await schedulesApi.overrideBlock(blockId, {
        decision: 'DELAYED',
        reason: 'Manually delayed from Generate view',
      })
      replaceBlock(updated)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not override block')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Generate schedule"
        description="Pick a project, add tasks, then create an optimised timetable."
        actions={
          <Link to="/projects" className={cn(buttonVariants({ variant: 'outline' }))}>
            Back to projects
          </Link>
        }
      />

      {error ? (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      {projects.length === 0 ? (
        <Empty className="border border-dashed">
          <EmptyHeader>
            <EmptyTitle>Create a project first</EmptyTitle>
            <EmptyDescription>
              You need a project before you can add tasks or generate a schedule.{' '}
              <Link to="/projects">Go to Projects</Link>
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <>
          <Card>
            <CardHeader>
              <CardTitle>Project & dates</CardTitle>
              <CardDescription>
                Choose which project to schedule and the date range to fill.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <FieldGroup className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <Field>
                  <FieldLabel htmlFor="project">Project</FieldLabel>
                  <Select
                    value={projectId}
                    onValueChange={(value) => {
                      if (value != null) setProjectId(String(value))
                    }}
                  >
                    <SelectTrigger id="project" className="w-full">
                      <SelectValue>
                        {selectedProject?.name ?? 'Select a project'}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      <SelectGroup>
                        {projects.map((p) => (
                          <SelectItem key={p.id} value={p.id}>
                            {p.name}
                          </SelectItem>
                        ))}
                      </SelectGroup>
                    </SelectContent>
                  </Select>
                </Field>
                <Field>
                  <FieldLabel htmlFor="schedStart">Start Date</FieldLabel>
                  <Input
                    id="schedStart"
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    required
                  />
                </Field>
                <Field>
                  <FieldLabel htmlFor="schedEnd">End Date</FieldLabel>
                  <Input
                    id="schedEnd"
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    required
                  />
                </Field>
              </FieldGroup>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Tasks Creation</CardTitle>
              <CardDescription>
                Tasks are registered when added, then used by Serenity or Crunch generation.
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="overflow-x-auto rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Task Name</TableHead>
                      <TableHead>Category</TableHead>
                      <TableHead>Project Type</TableHead>
                      <TableHead>Complexity</TableHead>
                      <TableHead>Priority</TableHead>
                      <TableHead>Estimate hours</TableHead>
                      <TableHead>Deadline</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="w-12">
                        <span className="sr-only">Remove</span>
                      </TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {tasks.map((t) => (
                      <TableRow key={t.id}>
                        <TableCell className="font-medium">{t.title}</TableCell>
                        <TableCell>{t.category}</TableCell>
                        <TableCell className="max-w-[10rem] truncate">
                          {t.description ?? '—'}
                        </TableCell>
                        <TableCell>{t.complexity}</TableCell>
                        <TableCell>{t.priority}</TableCell>
                        <TableCell>{t.estimatedDurationHours}</TableCell>
                        <TableCell>{deadlineDatePart(t.deadline) ?? '—'}</TableCell>
                        <TableCell>
                          <Badge variant="secondary">{t.status}</Badge>
                        </TableCell>
                        <TableCell>
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon-sm"
                            aria-label={`Remove ${t.title}`}
                            disabled={busy}
                            onClick={() => void removeTask(t.id)}
                          >
                            <TrashIcon />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                    {tasks.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={9}
                          className="py-8 text-center text-muted-foreground"
                        >
                          No tasks yet — add one below.
                        </TableCell>
                      </TableRow>
                    ) : null}
                  </TableBody>
                </Table>
              </div>

              <form
                onSubmit={addTask}
                className="grid gap-2 rounded-md border bg-muted/30 p-3 md:grid-cols-4 lg:grid-cols-8"
              >
                <Input
                  aria-label="Task name"
                  placeholder="Task name"
                  value={draft.title}
                  onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                  required
                  className="md:col-span-2"
                />
                <Input
                  aria-label="Category"
                  placeholder="Category"
                  value={draft.category}
                  onChange={(e) => setDraft({ ...draft, category: e.target.value })}
                  required
                />
                <Input
                  aria-label="Project type"
                  placeholder="Project type"
                  value={draft.projectType}
                  onChange={(e) => setDraft({ ...draft, projectType: e.target.value })}
                />
                <Input
                  aria-label="Complexity"
                  type="number"
                  min={1}
                  max={5}
                  value={Number.isFinite(draft.complexity) ? draft.complexity : ''}
                  onChange={(e) =>
                    setDraft({
                      ...draft,
                      complexity: e.target.value === '' ? Number.NaN : Number(e.target.value),
                    })
                  }
                  required
                />
                <Input
                  aria-label="Priority"
                  type="number"
                  min={1}
                  max={5}
                  value={Number.isFinite(draft.priority) ? draft.priority : ''}
                  onChange={(e) =>
                    setDraft({
                      ...draft,
                      priority: e.target.value === '' ? Number.NaN : Number(e.target.value),
                    })
                  }
                  required
                />
                <Input
                  aria-label="Estimate hours"
                  type="number"
                  min={0.25}
                  step={0.25}
                  value={Number.isFinite(draft.estimatedDurationHours) ? draft.estimatedDurationHours : ''}
                  onChange={(e) =>
                    setDraft({
                      ...draft,
                      estimatedDurationHours:
                        e.target.value === '' ? Number.NaN : Number(e.target.value),
                    })
                  }
                  required
                />
                <Input
                  aria-label="Deadline"
                  type="datetime-local"
                  min={toDateTimeLocalValue()}
                  value={draft.deadline}
                  onChange={(e) => setDraft({ ...draft, deadline: e.target.value })}
                  required
                />
                <Button type="submit" disabled={busy || !projectId} className="w-full">
                  Add task
                </Button>
              </form>
            </CardContent>
            <CardFooter className="justify-between border-t">
              <Link
                to="/settings"
                className={cn(buttonVariants({ variant: 'outline' }))}
              >
                Edit User preferences
              </Link>
              <form onSubmit={generate}>
                <Button
                  type="submit"
                  disabled={busy || !projectId || schedulableCount === 0}
                >
                  {busy ? 'Generating…' : 'Generate schedule'}
                </Button>
              </form>
            </CardFooter>
          </Card>

          {result ? (
            <Card>
              <CardHeader>
                <CardTitle className="flex flex-wrap items-center gap-2">
                  Result
                  <Badge>{result.mode}</Badge>
                  <Badge variant="secondary">{result.status}</Badge>
                </CardTitle>
                {result.explanationSummary ? (
                  <CardDescription>{result.explanationSummary}</CardDescription>
                ) : null}
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Decision</TableHead>
                        <TableHead>Window</TableHead>
                        <TableHead>Reason</TableHead>
                        <TableHead>Adjust</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {(result.blocks ?? []).length === 0 ? (
                        <TableRow>
                          <TableCell
                            colSpan={4}
                            className="py-8 text-center text-muted-foreground"
                          >
                            No blocks were placed for this range.
                          </TableCell>
                        </TableRow>
                      ) : (
                        (result.blocks ?? []).map((b) => (
                          <TableRow key={b.id}>
                            <TableCell>
                              <DecisionBadge decision={b.decision} />
                            </TableCell>
                            <TableCell className="text-xs">
                              {b.startTime ?? '—'} → {b.endTime ?? '—'}
                            </TableCell>
                            <TableCell className="max-w-xs text-muted-foreground">
                              {b.reason ?? '—'}
                            </TableCell>
                            <TableCell>
                              {b.decision === 'SCHEDULED' ? (
                                <div className="flex flex-col gap-1">
                                  <Button
                                    type="button"
                                    size="sm"
                                    variant="outline"
                                    disabled={busy}
                                    onClick={() => {
                                      setMovingBlockId(b.id)
                                      setMoveStart(
                                        b.startTime ? b.startTime.slice(0, 16) : toDateTimeLocalValue(),
                                      )
                                      setMoveEnd(
                                        b.endTime ? b.endTime.slice(0, 16) : toDateTimeLocalValue(),
                                      )
                                    }}
                                  >
                                    Move
                                  </Button>
                                  <Button
                                    type="button"
                                    size="sm"
                                    variant="ghost"
                                    disabled={busy}
                                    onClick={() => void delayBlock(b.id)}
                                  >
                                    Delay
                                  </Button>
                                </div>
                              ) : (
                                <span className="text-xs text-muted-foreground">—</span>
                              )}
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </div>
                {movingBlockId ? (
                  <form
                    className="mt-4 grid gap-2 rounded-md border p-3 sm:grid-cols-3"
                    onSubmit={(e) => {
                      e.preventDefault()
                      void rescheduleSelected()
                    }}
                  >
                    <Field>
                      <FieldLabel htmlFor="moveStart">New start</FieldLabel>
                      <Input
                        id="moveStart"
                        type="datetime-local"
                        value={moveStart}
                        onChange={(e) => setMoveStart(e.target.value)}
                        required
                      />
                    </Field>
                    <Field>
                      <FieldLabel htmlFor="moveEnd">New end</FieldLabel>
                      <Input
                        id="moveEnd"
                        type="datetime-local"
                        value={moveEnd}
                        onChange={(e) => setMoveEnd(e.target.value)}
                        required
                      />
                    </Field>
                    <div className="flex items-end gap-2">
                      <Button type="submit" disabled={busy}>
                        Save move
                      </Button>
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() => setMovingBlockId('')}
                      >
                        Cancel
                      </Button>
                    </div>
                  </form>
                ) : null}
              </CardContent>
              <CardFooter>
                <Link
                  to={`/projects?projectId=${projectId}`}
                  className={cn(buttonVariants({ variant: 'link' }), 'px-0')}
                >
                  Back to project
                </Link>
              </CardFooter>
            </Card>
          ) : null}
        </>
      )}
    </div>
  )
}
