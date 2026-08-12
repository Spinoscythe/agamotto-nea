import {type SubmitEvent, useEffect, useMemo, useState} from 'react'
import {Link, useSearchParams} from 'react-router-dom'
import {
  ApiError,
  type ProjectResponse,
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
    deadline: deadline.toISOString().slice(0, 16),
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
  const [startDate, setStartDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [endDate, setEndDate] = useState(() => {
    const d = new Date()
    d.setDate(d.getDate() + 7)
    return d.toISOString().slice(0, 10)
  })
  const [result, setResult] = useState<SchedulePlanResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const selectedProject = useMemo(
    () => projects.find((p) => p.id === projectId) ?? null,
    [projects, projectId],
  )

  useEffect(() => {
    if (!user) return
    void projectsApi
      .list(user.id)
      .then((list) => {
        setProjects(list)
        if (!projectId && list.length > 0) setProjectId(list[0].id)
      })
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : 'Failed to load projects')
      })
  }, [user, projectId])

  useEffect(() => {
    if (!projectId) {
      setTasks([])
      return
    }
    let cancelled = false
    void tasksApi
      .listByProject(projectId)
      .then((list) => {
        if (!cancelled) setTasks(list)
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
    setStartDate(selectedProject.startDate)
    setEndDate(selectedProject.endDate)
  }, [selectedProject])

  async function addTask(e: SubmitEvent) {
    e.preventDefault()
    if (!user || !projectId) return
    setError(null)
    setBusy(true)
    try {
      const created = await tasksApi.create(projectId, {
        actorUserId: user.id,
        title: draft.title.trim(),
        description: draft.projectType.trim() || undefined,
        category: draft.category.trim(),
        priority: draft.priority,
        deadline: draft.deadline.length === 16 ? `${draft.deadline}:00` : draft.deadline,
        estimatedDurationHours: draft.estimatedDurationHours,
        complexity: draft.complexity,
      })
      setTasks((prev) => [...prev, created])
      setDraft(emptyDraft())
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create task')
    } finally {
      setBusy(false)
    }
  }

  async function generate(e: SubmitEvent) {
    e.preventDefault()
    if (!projectId) return
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
                      <SelectValue />
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
                        <TableCell>{t.deadline.slice(0, 10)}</TableCell>
                        <TableCell>
                          <Badge variant="secondary">{t.status}</Badge>
                        </TableCell>
                      </TableRow>
                    ))}
                    {tasks.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={8}
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
                  value={draft.complexity}
                  onChange={(e) => setDraft({ ...draft, complexity: Number(e.target.value) })}
                  required
                />
                <Input
                  aria-label="Priority"
                  type="number"
                  min={1}
                  max={5}
                  value={draft.priority}
                  onChange={(e) => setDraft({ ...draft, priority: Number(e.target.value) })}
                  required
                />
                <Input
                  aria-label="Estimate hours"
                  type="number"
                  min={0.25}
                  step={0.25}
                  value={draft.estimatedDurationHours}
                  onChange={(e) =>
                    setDraft({ ...draft, estimatedDurationHours: Number(e.target.value) })
                  }
                  required
                />
                <Input
                  aria-label="Deadline"
                  type="datetime-local"
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
                  disabled={busy || !projectId || tasks.length === 0}
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
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {result.blocks.map((b) => (
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
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
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
