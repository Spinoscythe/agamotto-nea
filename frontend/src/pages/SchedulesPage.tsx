import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { DotsThreeIcon, PlusIcon } from '@phosphor-icons/react'
import {
  ApiError,
  projectsApi,
  schedulesApi,
  tasksApi,
  type ProjectResponse,
  type SchedulePlanResponse,
  type TaskResponse,
} from '@/api'
import { useAuth } from '@/auth/AuthContext'
import { PageHeader } from '@/components/PageHeader'
import { ScheduleWeekPreview } from '@/components/ScheduleWeekPreview'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { cn } from '@/lib/utils'

export function SchedulesPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const [projects, setProjects] = useState<ProjectResponse[]>([])
  const [selectedProjectId, setSelectedProjectId] = useState(
    searchParams.get('projectId') ?? '',
  )
  const [plans, setPlans] = useState<SchedulePlanResponse[]>([])
  const [selectedPlan, setSelectedPlan] = useState<SchedulePlanResponse | null>(null)
  const [tasks, setTasks] = useState<TaskResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const creatingRef = useRef(false)

  const [projectName, setProjectName] = useState('')
  const [projectDesc, setProjectDesc] = useState('')
  const [startDate, setStartDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [endDate, setEndDate] = useState(() => {
    const d = new Date()
    d.setDate(d.getDate() + 14)
    return d.toISOString().slice(0, 10)
  })
  const [effortHours, setEffortHours] = useState(40)

  useEffect(() => {
    if (!user) return
    let cancelled = false
    setLoading(true)
    setError(null)
    void projectsApi
      .list(user.id)
      .then((list) => {
        if (cancelled) return
        setProjects(list)
        setSelectedProjectId((current) => {
          const fromQuery = searchParams.get('projectId')
          if (fromQuery && list.some((p) => p.id === fromQuery)) return fromQuery
          if (current && list.some((p) => p.id === current)) return current
          return list[0]?.id ?? ''
        })
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : 'Failed to load projects')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [user, searchParams])

  useEffect(() => {
    if (!selectedProjectId) {
      setPlans([])
      setSelectedPlan(null)
      setTasks([])
      return
    }
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        next.set('projectId', selectedProjectId)
        return next
      },
      { replace: true },
    )
    let cancelled = false
    ;(async () => {
      try {
        const [list, taskList] = await Promise.all([
          schedulesApi.listByProject(selectedProjectId),
          tasksApi.listByProject(selectedProjectId),
        ])
        if (cancelled) return
        setPlans(list)
        setSelectedPlan(list[0] ?? null)
        setTasks(taskList)
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : 'Failed to load schedules')
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [selectedProjectId, setSearchParams])

  async function createProject(e: React.SubmitEvent) {
    e.preventDefault()
    if (!user || creatingRef.current) return
    creatingRef.current = true
    setError(null)
    try {
      const created = await projectsApi.create({
        ownerId: user.id,
        name: projectName.trim(),
        description: projectDesc.trim() || undefined,
        startDate,
        endDate,
        estimatedEffortHours: effortHours,
      })
      setProjectName('')
      setProjectDesc('')
      setProjects((prev) => [created, ...prev])
      setSelectedProjectId(created.id)
      setShowCreate(false)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create project')
    } finally {
      creatingRef.current = false
    }
  }

  async function deleteProject(projectId: string) {
    setError(null)
    try {
      await projectsApi.remove(projectId)
      setProjects((prev) => prev.filter((p) => p.id !== projectId))
      if (selectedProjectId === projectId) {
        setSelectedProjectId('')
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not delete project')
    }
  }

  async function openPlan(planId: string) {
    setError(null)
    try {
      const plan = await schedulesApi.get(planId)
      setSelectedPlan(plan)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not load plan')
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Projects"
        description="Start with a project, add tasks, then generate a schedule."
        actions={
          selectedProjectId ? (
            <Link
              to={`/generate?projectId=${selectedProjectId}`}
              className={cn(buttonVariants())}
            >
              Generate schedule
            </Link>
          ) : null
        }
      />

      {error ? (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm text-muted-foreground">
          {loading
            ? 'Loading…'
            : `${projects.length} project${projects.length === 1 ? '' : 's'}`}
        </p>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setShowCreate((v) => !v)}
        >
          <PlusIcon data-icon="inline-start" />
          {showCreate ? 'Cancel' : 'New project'}
        </Button>
      </div>

      {showCreate ? (
        <Card>
          <CardHeader>
            <CardTitle>New project</CardTitle>
            <CardDescription>
              A project holds your tasks. After you add tasks, you can generate a schedule.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form className="flex flex-col gap-4" onSubmit={createProject}>
              <FieldGroup>
                <Field>
                  <FieldLabel htmlFor="projectName">Project name</FieldLabel>
                  <Input
                    id="projectName"
                    value={projectName}
                    onChange={(e) => setProjectName(e.target.value)}
                    required
                    maxLength={200}
                    placeholder="e.g. A-level coursework"
                  />
                </Field>
                <Field>
                  <FieldLabel htmlFor="projectDesc">Description</FieldLabel>
                  <Textarea
                    id="projectDesc"
                    value={projectDesc}
                    onChange={(e) => setProjectDesc(e.target.value)}
                    rows={2}
                  />
                </Field>
                <div className="grid gap-3 sm:grid-cols-3">
                  <Field>
                    <FieldLabel htmlFor="startDate">Start</FieldLabel>
                    <Input
                      id="startDate"
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      required
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="endDate">End</FieldLabel>
                    <Input
                      id="endDate"
                      type="date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      required
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="effort">Effort (hours)</FieldLabel>
                    <Input
                      id="effort"
                      type="number"
                      min={0}
                      step={0.5}
                      value={effortHours}
                      onChange={(e) => setEffortHours(Number(e.target.value))}
                      required
                    />
                  </Field>
                </div>
              </FieldGroup>
              <Button type="submit" className="w-fit">
                Create project
              </Button>
            </form>
          </CardContent>
        </Card>
      ) : null}

      {projects.length > 0 ? (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((p) => (
            <Card
              key={p.id}
              size="sm"
              className={cn(
                'relative cursor-pointer transition-colors hover:bg-muted/40',
                p.id === selectedProjectId && 'ring-2 ring-ring',
              )}
              onClick={() => setSelectedProjectId(p.id)}
            >
              <CardHeader className="pr-12">
                <CardTitle className="text-base">{p.name}</CardTitle>
                <CardDescription>
                  {p.startDate} → {p.endDate} · {p.estimatedEffortHours}h
                </CardDescription>
              </CardHeader>
              <div
                className="absolute top-2 right-2"
                onClick={(e) => e.stopPropagation()}
                onKeyDown={(e) => e.stopPropagation()}
              >
                <DropdownMenu>
                  <DropdownMenuTrigger
                    render={
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        aria-label={`Actions for ${p.name}`}
                      />
                    }
                  >
                    <DotsThreeIcon />
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="min-w-44">
                    <DropdownMenuGroup>
                      <DropdownMenuItem
                        render={<Link to={`/generate?projectId=${p.id}`} />}
                      >
                        Add tasks & generate
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        variant="destructive"
                        onClick={() => void deleteProject(p.id)}
                      >
                        Delete project
                      </DropdownMenuItem>
                      <DropdownMenuItem render={<Link to="/settings" />}>
                        Edit preferences
                      </DropdownMenuItem>
                    </DropdownMenuGroup>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </Card>
          ))}
        </div>
      ) : !loading ? (
        <Empty className="border border-dashed">
          <EmptyHeader>
            <EmptyTitle>No projects yet</EmptyTitle>
            <EmptyDescription>
              Create a project first. Then add tasks and generate a schedule.
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button type="button" onClick={() => setShowCreate(true)}>
              <PlusIcon data-icon="inline-start" />
              New project
            </Button>
          </EmptyContent>
        </Empty>
      ) : null}

      {selectedProjectId && plans.length > 1 ? (
        <div className="flex flex-wrap gap-2">
          {plans.map((plan) => (
            <Button
              key={plan.id}
              type="button"
              size="sm"
              variant={plan.id === selectedPlan?.id ? 'secondary' : 'outline'}
              onClick={() => void openPlan(plan.id)}
            >
              {plan.mode}
              <Badge variant="outline" className="ml-1">
                {plan.status}
              </Badge>
            </Button>
          ))}
        </div>
      ) : null}

      {selectedPlan ? (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium">Generated schedule</p>
          <p className="text-sm text-muted-foreground">
            {selectedPlan.mode} · {selectedPlan.startDate} to {selectedPlan.endDate}
            {selectedPlan.explanationSummary
              ? ` · ${selectedPlan.explanationSummary}`
              : null}
          </p>
          <ScheduleWeekPreview
            blocks={selectedPlan.blocks}
            tasks={tasks}
            startDate={selectedPlan.startDate}
            endDate={selectedPlan.endDate}
          />
        </div>
      ) : selectedProjectId && !loading ? (
        <Empty className="border border-dashed">
          <EmptyHeader>
            <EmptyTitle>No schedule for this project yet</EmptyTitle>
            <EmptyDescription>
              Add your tasks, then generate a timetable for the selected project.
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Link
              to={`/generate?projectId=${selectedProjectId}`}
              className={cn(buttonVariants())}
            >
              Add tasks & generate schedule
            </Link>
          </EmptyContent>
        </Empty>
      ) : null}
    </div>
  )
}
