import {type SubmitEvent, useEffect, useMemo, useRef, useState} from 'react'
import {Link, useSearchParams} from 'react-router-dom'
import {TrashIcon, UserIcon} from '@phosphor-icons/react'
import {Calendar, Clock, SlidersHorizontal} from 'lucide-react'
import {toast} from 'sonner'
import {
    ApiError,
    type ProjectMemberResponse,
    type ProjectResponse,
    type ScheduleBlockResponse,
    collaborationApi,
    projectsApi,
    type SchedulePlanResponse,
    schedulesApi,
    type TaskResponse,
    type TaskStatus,
    tasksApi,
} from '@/api'
import {useAuth} from '@/auth/AuthContext'
import {DecisionBadge} from '@/components/DecisionBadge'
import {PageHeader} from '@/components/PageHeader'
import {ScheduleWeekPreview} from '@/components/ScheduleWeekPreview'
import {StatusBadge} from '@/components/StatusBadge'
import {Alert, AlertDescription} from '@/components/ui/alert'
import {Badge} from '@/components/ui/badge'
import {Button, buttonVariants} from '@/components/ui/button'
import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle,} from '@/components/ui/card'
import {Empty, EmptyDescription, EmptyHeader, EmptyTitle,} from '@/components/ui/empty'
import {Field, FieldGroup, FieldLabel} from '@/components/ui/field'
import {Input} from '@/components/ui/input'
import {Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue,} from '@/components/ui/select'
import {Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow,} from '@/components/ui/table'
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs'
import {deadlineDatePart, toDateInputValue, toDateTimeLocalValue, toLocalDateTimePayload} from '@/lib/datetime'
import {COMPLEXITY_EASY, COMPLEXITY_HARD, COMPLEXITY_MEDIUM, complexityLabel} from '@/lib/complexity'
import {cn} from '@/lib/utils'

function formatBlockDate(iso: string | null | undefined): string {
    if (!iso) return '—'
    const date = new Date(iso)
    if (Number.isNaN(date.getTime())) return iso.slice(0, 10)
    return date.toLocaleDateString(undefined, {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
    })
}

function formatBlockTime(iso: string | null | undefined): string {
    if (!iso) return '—'
    const date = new Date(iso)
    if (Number.isNaN(date.getTime())) return iso.slice(11, 16)
    return date.toLocaleTimeString(undefined, {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
    })
}

function formatDurationHours(startIso: string | null | undefined, endIso: string | null | undefined): string {
    if (!startIso || !endIso) return '—'
    const start = new Date(startIso).getTime()
    const end = new Date(endIso).getTime()
    if (Number.isNaN(start) || Number.isNaN(end) || end <= start) return '—'
    const hours = (end - start) / (1000 * 60 * 60)
    return hours === 1 ? '1.0h' : `${hours.toFixed(1)}h`
}

function formatShortDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '—'
    const date = new Date(`${dateStr}T12:00:00`)
    if (Number.isNaN(date.getTime())) return dateStr
    return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

function formatReasonSummary(reason: string | null | undefined): string {
    if (!reason) return '—'
    const match = reason.match(/because score=([0-9.]+)/)
    if (match) {
        return `Score: ${match[1]} (High priority & urgency)`
    }
    return reason
}

interface TaskDraft {
    title: string
    category: string
    projectType: string
    assigneeId: string
    priority: number
    deadline: string
    estimatedDurationHours: number
    complexity: number
    status: TaskStatus
}

const emptyDraft = (): TaskDraft => {
    const deadline = new Date()
    deadline.setDate(deadline.getDate() + 7)
    deadline.setMinutes(0, 0, 0)
    return {
        title: '',
        category: 'work',
        projectType: 'General',
        assigneeId: '',
        priority: 3,
        deadline: toDateInputValue(deadline), // YYYY-MM-DD
        estimatedDurationHours: 2,
        complexity: COMPLEXITY_MEDIUM,
        status: 'PENDING'
    }
}

export function GenerateSchedulePage() {
    const {user} = useAuth()
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

    // Project admin (creator) decides task assignees
    const isProjectAdmin = useMemo(() => {
        if (!selectedProject || !user) return false
        return selectedProject.ownerId === user.id
    }, [selectedProject, user])

    const schedulableCount = useMemo(
        () => tasks.filter((t) => t.status === 'PENDING' || t.status === 'IN_PROGRESS').length,
        [tasks],
    )

    const resultRef = useRef<HTMLDivElement | null>(null)
    useEffect(() => {
        if (result && resultRef.current) {
            resultRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
    }, [result])

    const taskById = useMemo(() => {
        const map = new Map<string, TaskResponse>()
        for (const t of tasks) map.set(t.id, t)
        return map
    }, [tasks])

    const scheduledBlocks = useMemo(
        () => (result?.blocks ?? []).filter((b) => b.decision === 'SCHEDULED' && b.startTime && b.endTime),
        [result?.blocks],
    )

    const totalScheduledHours = useMemo(() => {
        let total = 0
        for (const b of scheduledBlocks) {
            if (b.startTime && b.endTime) {
                const s = new Date(b.startTime).getTime()
                const e = new Date(b.endTime).getTime()
                if (!Number.isNaN(s) && !Number.isNaN(e) && e > s) {
                    total += (e - s) / (1000 * 60 * 60)
                }
            }
        }
        return total
    }, [scheduledBlocks])

    const tasksScheduledCount = useMemo(() => {
        const ids = new Set(scheduledBlocks.map((b) => b.taskId))
        return ids.size
    }, [scheduledBlocks])

    const sittingByBlockId = useMemo(() => {
        const byTask = new Map<string, ScheduleBlockResponse[]>()
        for (const block of scheduledBlocks) {
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
    }, [scheduledBlocks])

    const movingBlock = useMemo(
        () => result?.blocks?.find((b) => b.id === movingBlockId) ?? null,
        [result?.blocks, movingBlockId],
    )
    const movingTaskTitle = useMemo(
        () => (movingBlock ? taskById.get(movingBlock.taskId)?.title ?? 'Task' : ''),
        [movingBlock, taskById],
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

    const [members, setMembers] = useState<ProjectMemberResponse[]>([])

    useEffect(() => {
        if (!projectId) {
            setMembers([])
            return
        }
        let cancelled = false
        void collaborationApi
            .listMembers(projectId)
            .then((list) => {
                if (!cancelled) setMembers(list)
            })
            .catch(() => {
                if (!cancelled) setMembers([])
            })
        return () => {
            cancelled = true
        }
    }, [projectId])

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

    async function addTask(e: SubmitEvent) {
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
        const deadlineIso = draft.deadline.includes('T')
            ? draft.deadline
            : `${draft.deadline}T23:59:59`
        const deadlineMs = new Date(deadlineIso).getTime()
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
                deadline: toLocalDateTimePayload(deadlineIso),
                estimatedDurationHours: draft.estimatedDurationHours,
                complexity: draft.complexity,
                // Only the project admin (creator) can decide the assignee for tasks
                assigneeId: isProjectAdmin && draft.assigneeId ? draft.assigneeId : undefined,
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

    async function updateTaskAssignee(taskId: string, newAssigneeId: string) {
        if (!user || !isProjectAdmin) return
        setError(null)
        setBusy(true)
        try {
            const updated = await tasksApi.update(taskId, {
                actorUserId: user.id,
                assigneeId: newAssigneeId || 'unassigned',
            })
            setTasks((prev) => prev.map((t) => (t.id === taskId ? updated : t)))
        } catch (err) {
            setError(err instanceof ApiError ? err.message : 'Could not update task assignee')
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

    async function generate(e: SubmitEvent) {
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
            const plan = await schedulesApi.generate(projectId, {startDate, endDate})
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

    async function markUnattended(blockId: string) {
        setError(null)
        setBusy(true)
        try {
            const response = await schedulesApi.markUnattended(blockId)
            if (response.regeneratedPlan) {
                setResult(response.regeneratedPlan)
                const errorBlock = response.regeneratedPlan.blocks?.find(
                    (b) =>
                        b.decision === 'DELAYED' &&
                        b.reason &&
                        (b.reason.includes("you can't do anything anymore") ||
                         b.reason.toLowerCase().includes('deadline approached')),
                )
                if (errorBlock?.reason) {
                    setError(errorBlock.reason)
                    toast.error(errorBlock.reason)
                } else {
                    toast.success('Schedule rearranged after unattended session.')
                }
            }
        } catch (err) {
            const msg = err instanceof ApiError ? err.message : 'Could not rearrange schedule for unattended session'
            setError(msg)
            toast.error(msg)
        } finally {
            setBusy(false)
        }
    }

    return (
        <div className="flex flex-col gap-6">
            <PageHeader
                title="Generate schedule"
                description="Pick a project, add tasks, then create an optimised timetable. Sittings are 30 minutes (Easy), 1 hour (Medium), or 2 hours (Hard), interleaved across tasks."
                actions={
                    <Link to="/projects" className={cn(buttonVariants({variant: 'outline'}))}>
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
                            {members.length > 1 ? (
                                <div className="flex items-center gap-2 rounded-lg border border-primary/20 bg-primary/5 px-3 py-2 text-xs text-foreground/85">
                                    <span className="font-semibold text-primary shrink-0">
                                        {isProjectAdmin ? 'Project Admin:' : 'Group Project:'}
                                    </span>
                                    <span>
                                        {isProjectAdmin
                                            ? 'You assign tasks to team members before generating the schedule.'
                                            : 'Task assignments are set by the project admin.'}
                                    </span>
                                </div>
                            ) : null}

                            <form onSubmit={addTask}>
                                <div className="overflow-x-auto rounded-md border">
                                    <Table>
                                        <TableHeader>
                                            <TableRow>
                                                <TableHead className="min-w-[150px] text-center">Task Name</TableHead>
                                                <TableHead className="min-w-[120px] text-center">Category</TableHead>
                                                <TableHead className="min-w-[130px] text-center">Description</TableHead>
                                                <TableHead className="min-w-[140px] text-center">Assignee</TableHead>
                                                <TableHead className="min-w-[120px] text-center">Complexity</TableHead>
                                                <TableHead className="min-w-[130px] text-center">Priority</TableHead>
                                                <TableHead className="min-w-[130px] text-center">Estimate hours</TableHead>
                                                <TableHead className="min-w-[150px] text-center">Deadline</TableHead>
                                                <TableHead className="min-w-[150px] text-center">Status</TableHead>
                                                <TableHead className="w-28 text-center">Action</TableHead>
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
                                                    <TableCell className="text-center">
                                                        {isProjectAdmin && members.length > 0 ? (
                                                            <Select
                                                                value={t.assigneeId || 'unassigned'}
                                                                onValueChange={(val) => {
                                                                    if (val != null) {
                                                                        void updateTaskAssignee(t.id, val === 'unassigned' ? '' : val)
                                                                    }
                                                                }}
                                                                disabled={busy}
                                                            >
                                                                <SelectTrigger
                                                                    aria-label={`Assignee for ${t.title}`}
                                                                    className="h-7 text-xs min-w-[120px] max-w-[150px] mx-auto px-2"
                                                                >
                                                                    <SelectValue>
                                                                        {t.assigneeName || 'Unassigned'}
                                                                    </SelectValue>
                                                                </SelectTrigger>
                                                                <SelectContent>
                                                                    <SelectGroup>
                                                                        <SelectItem value="unassigned">Unassigned</SelectItem>
                                                                        {members.map((m) => (
                                                                            <SelectItem key={m.userId} value={m.userId}>
                                                                                {m.fullName || m.email}
                                                                            </SelectItem>
                                                                        ))}
                                                                    </SelectGroup>
                                                                </SelectContent>
                                                            </Select>
                                                        ) : (
                                                            t.assigneeName ? (
                                                                <span
                                                                    className="inline-flex items-center gap-1.5 rounded-full bg-secondary/80 px-2.5 py-0.5 text-xs font-medium"
                                                                    title="Assigned by project admin"
                                                                >
                                                                    <UserIcon size={12} className="text-muted-foreground" />
                                                                    <span className="truncate max-w-[8rem]">{t.assigneeName}</span>
                                                                </span>
                                                            ) : (
                                                                <span
                                                                    className="text-xs text-muted-foreground/60 italic"
                                                                    title="Admin has not yet assigned this task"
                                                                >
                                                                    Unassigned
                                                                </span>
                                                            )
                                                        )}
                                                    </TableCell>
                                                    <TableCell className="text-center">{complexityLabel(t.complexity)}</TableCell>
                                                    <TableCell className="text-center">{t.priority}</TableCell>
                                                    <TableCell className="text-center">{t.estimatedDurationHours}</TableCell>
                                                    <TableCell className="text-center">{deadlineDatePart(t.deadline) ?? '—'}</TableCell>
                                                    <TableCell className="text-center">
                                                        <StatusBadge status={t.status}/>
                                                    </TableCell>
                                                    <TableCell className="text-center">
                                                        <Button
                                                            type="button"
                                                            variant="ghost"
                                                            size="icon-sm"
                                                            aria-label={`Remove ${t.title}`}
                                                            disabled={busy}
                                                            onClick={() => void removeTask(t.id)}
                                                        >
                                                            <TrashIcon/>
                                                        </Button>
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                            {tasks.length === 0 ? (
                                                <TableRow>
                                                    <TableCell
                                                        colSpan={10}
                                                        className="py-8 text-center text-muted-foreground"
                                                    >
                                                        No tasks yet — enter details below and click Add task.
                                                    </TableCell>
                                                </TableRow>
                                            ) : null}
                                        </TableBody>

                                        {/* --- INLINE ADD ROW (PERFECTLY ALIGNED) --- */}
                                        <TableFooter className="bg-muted/40 font-normal">
                                            <TableRow>
                                                {/* 1. Task Name */}
                                                <TableCell>
                                                    <Input
                                                        aria-label="Task name"
                                                        placeholder="Task name"
                                                        value={draft.title}
                                                        onChange={(e) => setDraft({...draft, title: e.target.value})}
                                                        required
                                                    />
                                                </TableCell>

                                                {/* 2. Category */}
                                                <TableCell>
                                                    <Input
                                                        aria-label="Category"
                                                        placeholder="Category"
                                                        value={draft.category}
                                                        onChange={(e) => setDraft({...draft, category: e.target.value})}
                                                        required
                                                    />
                                                </TableCell>

                                                {/* 3. Project Type */}
                                                <TableCell>
                                                    <Input
                                                        aria-label="Project type"
                                                        placeholder="Project type"
                                                        value={draft.projectType}
                                                        onChange={(e) => setDraft({
                                                            ...draft,
                                                            projectType: e.target.value
                                                        })}
                                                    />
                                                </TableCell>

                                                {/* 4. Assignee (Admin decides) */}
                                                <TableCell>
                                                    {isProjectAdmin || members.length <= 1 ? (
                                                        <Select
                                                            value={draft.assigneeId || 'unassigned'}
                                                            onValueChange={(value) => {
                                                                if (value != null) setDraft({
                                                                    ...draft,
                                                                    assigneeId: value === 'unassigned' ? '' : String(value),
                                                                })
                                                            }}
                                                            disabled={busy}
                                                        >
                                                            <SelectTrigger aria-label="Assignee" className="w-full">
                                                                <SelectValue>
                                                                    {draft.assigneeId
                                                                        ? (members.find((m) => m.userId === draft.assigneeId)?.fullName || 'Assigned')
                                                                        : 'Unassigned'}
                                                                </SelectValue>
                                                            </SelectTrigger>
                                                            <SelectContent>
                                                                <SelectGroup>
                                                                    <SelectItem value="unassigned">Unassigned</SelectItem>
                                                                    {members.map((m) => (
                                                                        <SelectItem key={m.userId} value={m.userId}>
                                                                            {m.fullName || m.email}
                                                                        </SelectItem>
                                                                    ))}
                                                                </SelectGroup>
                                                            </SelectContent>
                                                        </Select>
                                                    ) : (
                                                        <div
                                                            className="flex h-9 w-full items-center justify-center rounded-md border border-input bg-muted/40 px-2 text-xs text-muted-foreground text-center"
                                                            title="Only the project admin (who created the project) can decide task assignees"
                                                        >
                                                            Admin assigns
                                                        </div>
                                                    )}
                                                </TableCell>

                                                {/* 4. Complexity: Easy / Medium / Hard */}
                                                <TableCell>
                                                    <Select
                                                        value={String(draft.complexity)}
                                                        onValueChange={(value) => {
                                                            if (value != null) setDraft({
                                                                ...draft,
                                                                complexity: Number(value)
                                                            })
                                                        }}
                                                    >
                                                        <SelectTrigger aria-label="Complexity" className="w-full">
                                                            <SelectValue>{complexityLabel(draft.complexity)}</SelectValue>
                                                        </SelectTrigger>
                                                        <SelectContent>
                                                            <SelectGroup>
                                                                <SelectItem
                                                                    value={String(COMPLEXITY_EASY)}>Easy</SelectItem>
                                                                <SelectItem
                                                                    value={String(COMPLEXITY_MEDIUM)}>Medium</SelectItem>
                                                                <SelectItem
                                                                    value={String(COMPLEXITY_HARD)}>Hard</SelectItem>
                                                            </SelectGroup>
                                                        </SelectContent>
                                                    </Select>
                                                </TableCell>

                                                {/* 5. Priority: 1-5 (1 highest, 5 lowest) */}
                                                <TableCell>
                                                    <Select
                                                        value={String(draft.priority)}
                                                        onValueChange={(value) => {
                                                            if (value != null) setDraft({
                                                                ...draft,
                                                                priority: Number(value)
                                                            })
                                                        }}
                                                    >
                                                        <SelectTrigger aria-label="Priority" className="w-full">
                                                            <SelectValue>
                                                                {draft.priority === 1
                                                                    ? '1 (Highest)'
                                                                    : draft.priority === 5
                                                                        ? '5 (Lowest)'
                                                                        : String(draft.priority)}
                                                            </SelectValue>
                                                        </SelectTrigger>
                                                        <SelectContent>
                                                            <SelectGroup>
                                                                <SelectItem value="1">1 (Highest)</SelectItem>
                                                                <SelectItem value="2">2 (High)</SelectItem>
                                                                <SelectItem value="3">3 (Medium)</SelectItem>
                                                                <SelectItem value="4">4 (Low)</SelectItem>
                                                                <SelectItem value="5">5 (Lowest)</SelectItem>
                                                            </SelectGroup>
                                                        </SelectContent>
                                                    </Select>
                                                </TableCell>

                                                {/* 6. Estimate hours */}
                                                <TableCell>
                                                    <Input
                                                        aria-label="Estimate hours"
                                                        type="number"
                                                        min={0.25}
                                                        step={0.25}
                                                        placeholder="Hours"
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
                                                </TableCell>

                                                {/* 7. Deadline: Date picker (Calendar View) */}
                                                <TableCell>
                                                    <Input
                                                        aria-label="Deadline"
                                                        type="date"
                                                        min={toDateInputValue()}
                                                        value={draft.deadline}
                                                        onChange={(e) => setDraft({...draft, deadline: e.target.value})}
                                                        required
                                                    />
                                                </TableCell>

                                                {/* 8. Status: PENDING, IN_PROGRESS, COMPLETED, CANCELLED */}
                                                <TableCell>
                                                    <Select
                                                        value={draft.status}
                                                        onValueChange={(value) => {
                                                            if (value != null) setDraft({
                                                                ...draft,
                                                                status: value as TaskStatus
                                                            })
                                                        }}
                                                    >
                                                        <SelectTrigger aria-label="Status" className="w-full">
                                                            <SelectValue>{draft.status}</SelectValue>
                                                        </SelectTrigger>
                                                        <SelectContent>
                                                            <SelectGroup>
                                                                <SelectItem value="PENDING">PENDING</SelectItem>
                                                                <SelectItem value="IN_PROGRESS">IN_PROGRESS</SelectItem>
                                                                <SelectItem value="COMPLETED">COMPLETED</SelectItem>
                                                                <SelectItem value="CANCELLED">CANCELLED</SelectItem>
                                                            </SelectGroup>
                                                        </SelectContent>
                                                    </Select>
                                                </TableCell>

                                                {/* 9. Action Button */}
                                                <TableCell className="text-right">
                                                    <Button type="submit" size="sm" disabled={busy || !projectId}
                                                            className="whitespace-nowrap">
                                                        Add task
                                                    </Button>
                                                </TableCell>
                                            </TableRow>
                                        </TableFooter>
                                    </Table>
                                </div>
                            </form>
                        </CardContent>
                        <CardFooter className="justify-between border-t">
                            <Link
                                to="/settings"
                                className={cn(buttonVariants({variant: 'outline'}))}
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
                        <div ref={resultRef} className="flex flex-col gap-4">
                            <Card className="overflow-hidden border-2 border-primary/20 shadow-md">
                                <CardHeader className="border-b bg-muted/20 pb-4">
                                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                                        <div>
                                            <div className="flex flex-wrap items-center gap-2">
                                                <CardTitle className="text-xl font-bold tracking-tight">
                                                    Generated Timetable
                                                </CardTitle>
                                                <Badge
                                                    variant={result.mode === 'SERENITY' ? 'default' : 'secondary'}
                                                    className="font-semibold uppercase tracking-wider text-xs"
                                                >
                                                    {result.mode}
                                                </Badge>
                                                <StatusBadge status={result.status}/>
                                            </div>
                                            {result.explanationSummary ? (
                                                <CardDescription className="mt-1.5 text-sm leading-relaxed text-foreground/80">
                                                    {result.explanationSummary}
                                                </CardDescription>
                                            ) : null}
                                        </div>
                                        <div className="flex items-center gap-2 shrink-0">
                                            <Link
                                                to={`/projects?projectId=${projectId}`}
                                                className={cn(buttonVariants({variant: 'outline', size: 'sm'}))}
                                            >
                                                Back to project
                                            </Link>
                                        </div>
                                    </div>

                                    {/* Summary Metric Pills */}
                                    <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
                                        <div className="rounded-lg border bg-background/80 p-3 shadow-2xs">
                                            <p className="text-xs font-medium text-muted-foreground">Total Scheduled</p>
                                            <p className="mt-1 text-lg font-bold text-foreground">
                                                {totalScheduledHours.toFixed(1)} hrs
                                            </p>
                                        </div>
                                        <div className="rounded-lg border bg-background/80 p-3 shadow-2xs">
                                            <p className="text-xs font-medium text-muted-foreground">Sittings</p>
                                            <p className="mt-1 text-lg font-bold text-foreground">
                                                {scheduledBlocks.length} blocks
                                            </p>
                                        </div>
                                        <div className="rounded-lg border bg-background/80 p-3 shadow-2xs">
                                            <p className="text-xs font-medium text-muted-foreground">Tasks Included</p>
                                            <p className="mt-1 text-lg font-bold text-foreground">
                                                {tasksScheduledCount} / {tasks.length} tasks
                                            </p>
                                        </div>
                                        <div className="rounded-lg border bg-background/80 p-3 shadow-2xs">
                                            <p className="text-xs font-medium text-muted-foreground">Date Range</p>
                                            <p className="mt-1 text-sm font-bold text-foreground truncate">
                                                {formatShortDate(result.startDate)} → {formatShortDate(result.endDate)}
                                            </p>
                                        </div>
                                    </div>
                                </CardHeader>

                                <CardContent className="p-4 sm:p-6">
                                    <Tabs defaultValue="timetable" className="w-full">
                                        <TabsList className="mb-6 w-full justify-start sm:w-fit">
                                            <TabsTrigger value="timetable" className="gap-2 px-4 py-1.5 text-sm">
                                                <Calendar className="size-4"/>
                                                Weekly Timetable
                                            </TabsTrigger>
                                            <TabsTrigger value="blocks" className="gap-2 px-4 py-1.5 text-sm">
                                                <SlidersHorizontal className="size-4"/>
                                                All Sittings & Adjustments ({scheduledBlocks.length})
                                            </TabsTrigger>
                                        </TabsList>

                                        {/* TAB 1: VISUAL WEEKLY TIMETABLE CALENDAR */}
                                        <TabsContent value="timetable" className="focus-visible:outline-none">
                                            <ScheduleWeekPreview
                                                blocks={result.blocks}
                                                tasks={tasks}
                                                startDate={result.startDate}
                                                endDate={result.endDate}
                                                projectId={projectId}
                                                onDeleteTask={removeTask}
                                                onMarkUnattended={markUnattended}
                                            />
                                        </TabsContent>

                                        {/* TAB 2: DETAILED SITTING BLOCKS & ADJUSTMENTS */}
                                        <TabsContent value="blocks" className="focus-visible:outline-none">
                                            {movingBlockId ? (
                                                <div className="mb-6 rounded-lg border-2 border-primary/30 bg-muted/40 p-4 shadow-sm">
                                                    <div className="flex flex-col gap-3">
                                                        <div className="flex items-center justify-between">
                                                            <div className="flex items-center gap-2">
                                                                <Clock className="size-4 text-primary"/>
                                                                <h4 className="text-sm font-semibold text-foreground">
                                                                    Reschedule Sitting: <span className="text-primary">{movingTaskTitle}</span>
                                                                </h4>
                                                            </div>
                                                            <Button
                                                                type="button"
                                                                variant="ghost"
                                                                size="sm"
                                                                className="h-7 text-xs"
                                                                onClick={() => setMovingBlockId('')}
                                                            >
                                                                Cancel
                                                            </Button>
                                                        </div>
                                                        <p className="text-xs text-muted-foreground">
                                                            Choose a new start and end datetime for this sitting block.
                                                        </p>
                                                        <form
                                                            className="grid gap-3 sm:grid-cols-[1fr_1fr_auto]"
                                                            onSubmit={(e) => {
                                                                e.preventDefault()
                                                                void rescheduleSelected()
                                                            }}
                                                        >
                                                            <Field>
                                                                <FieldLabel htmlFor="moveStart" className="text-xs">
                                                                    New Start
                                                                </FieldLabel>
                                                                <Input
                                                                    id="moveStart"
                                                                    type="datetime-local"
                                                                    value={moveStart}
                                                                    onChange={(e) => setMoveStart(e.target.value)}
                                                                    required
                                                                    className="h-9"
                                                                />
                                                            </Field>
                                                            <Field>
                                                                <FieldLabel htmlFor="moveEnd" className="text-xs">
                                                                    New End
                                                                </FieldLabel>
                                                                <Input
                                                                    id="moveEnd"
                                                                    type="datetime-local"
                                                                    value={moveEnd}
                                                                    onChange={(e) => setMoveEnd(e.target.value)}
                                                                    required
                                                                    className="h-9"
                                                                />
                                                            </Field>
                                                            <div className="flex items-end">
                                                                <Button
                                                                    type="submit"
                                                                    size="sm"
                                                                    disabled={busy}
                                                                    className="h-9 whitespace-nowrap"
                                                                >
                                                                    Save move
                                                                </Button>
                                                            </div>
                                                        </form>
                                                    </div>
                                                </div>
                                            ) : null}

                                            <div className="overflow-x-auto rounded-lg border">
                                                <Table>
                                                    <TableHeader className="bg-muted/50">
                                                        <TableRow>
                                                            <TableHead className="w-12 text-center font-semibold">#</TableHead>
                                                            <TableHead className="min-w-[180px] font-semibold">Task</TableHead>
                                                            <TableHead className="min-w-[130px] font-semibold">Day & Date</TableHead>
                                                            <TableHead className="min-w-[150px] font-semibold">Time Window</TableHead>
                                                            <TableHead className="min-w-[90px] text-center font-semibold">Duration</TableHead>
                                                            <TableHead className="min-w-[110px] text-center font-semibold">Decision</TableHead>
                                                            <TableHead className="min-w-[200px] font-semibold">Algorithm Note</TableHead>
                                                            <TableHead className="w-28 text-right font-semibold">Adjust</TableHead>
                                                        </TableRow>
                                                    </TableHeader>
                                                    <TableBody>
                                                        {(result.blocks ?? []).length === 0 ? (
                                                            <TableRow>
                                                                <TableCell colSpan={8} className="py-12 text-center text-muted-foreground">
                                                                    No blocks were placed for this range.
                                                                </TableCell>
                                                            </TableRow>
                                                        ) : (
                                                            (result.blocks ?? []).map((b, index) => {
                                                                const task = taskById.get(b.taskId)
                                                                const sitting = sittingByBlockId.get(b.id)
                                                                const isMoving = b.id === movingBlockId
                                                                return (
                                                                    <TableRow key={b.id} className={cn(isMoving && 'bg-primary/5 font-medium')}>
                                                                        <TableCell className="text-center text-xs text-muted-foreground">
                                                                            {index + 1}
                                                                        </TableCell>
                                                                        <TableCell>
                                                                            <div className="flex flex-col gap-0.5">
                                                                                <div className="flex items-center gap-1.5">
                                                                                    <span className="font-semibold text-foreground text-sm">
                                                                                        {task?.title ?? b.taskId}
                                                                                    </span>
                                                                                    {sitting && sitting.m > 1 ? (
                                                                                        <Badge variant="outline" className="text-[0.65rem] px-1.5 py-0">
                                                                                            Sitting {sitting.n}/{sitting.m}
                                                                                        </Badge>
                                                                                    ) : null}
                                                                                </div>
                                                                                {task?.category ? (
                                                                                    <span className="text-xs text-muted-foreground">
                                                                                        {task.category}
                                                                                    </span>
                                                                                ) : null}
                                                                            </div>
                                                                        </TableCell>
                                                                        <TableCell className="text-sm font-medium">
                                                                            {formatBlockDate(b.startTime)}
                                                                        </TableCell>
                                                                        <TableCell>
                                                                            {b.startTime && b.endTime ? (
                                                                                <div className="flex items-center gap-1.5 text-sm font-medium font-mono text-foreground/90">
                                                                                    <span>{formatBlockTime(b.startTime)}</span>
                                                                                    <span className="text-muted-foreground">→</span>
                                                                                    <span>{formatBlockTime(b.endTime)}</span>
                                                                                </div>
                                                                            ) : (
                                                                                <span className="text-xs text-muted-foreground">—</span>
                                                                            )}
                                                                        </TableCell>
                                                                        <TableCell className="text-center">
                                                                            <Badge variant="secondary" className="font-normal text-xs">
                                                                                {formatDurationHours(b.startTime, b.endTime)}
                                                                            </Badge>
                                                                        </TableCell>
                                                                        <TableCell className="text-center">
                                                                            <DecisionBadge decision={b.decision}/>
                                                                        </TableCell>
                                                                        <TableCell className="max-w-xs text-xs text-muted-foreground" title={b.reason ?? undefined}>
                                                                            <span className="line-clamp-2">
                                                                                {formatReasonSummary(b.reason)}
                                                                            </span>
                                                                        </TableCell>
                                                                        <TableCell className="text-right">
                                                                            {b.decision === 'SCHEDULED' ? (
                                                                                <div className="flex items-center justify-end gap-1">
                                                                                    <Button
                                                                                        type="button"
                                                                                        size="sm"
                                                                                        variant={isMoving ? 'default' : 'outline'}
                                                                                        className="h-7 px-2 text-xs"
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
                                                                                        variant="outline"
                                                                                        className="h-7 px-2 text-xs text-amber-600 dark:text-amber-400 hover:text-amber-700 hover:bg-amber-500/10 border-amber-500/30"
                                                                                        disabled={busy}
                                                                                        title="Mark this session as unattended and rearrange the schedule"
                                                                                        onClick={() => void markUnattended(b.id)}
                                                                                    >
                                                                                        Unattended
                                                                                    </Button>
                                                                                    <Button
                                                                                        type="button"
                                                                                        size="sm"
                                                                                        variant="ghost"
                                                                                        className="h-7 px-2 text-xs text-muted-foreground hover:text-destructive"
                                                                                        disabled={busy}
                                                                                        onClick={() => void delayBlock(b.id)}
                                                                                    >
                                                                                        Delay
                                                                                    </Button>
                                                                                </div>
                                                                            ) : b.reason?.toLowerCase().startsWith('unattended session') ? (
                                                                                <span className="text-xs font-semibold text-amber-600 dark:text-amber-400">
                                                                                    Missed
                                                                                </span>
                                                                            ) : (
                                                                                <span className="text-xs text-muted-foreground">—</span>
                                                                            )}
                                                                        </TableCell>
                                                                    </TableRow>
                                                                )
                                                            })
                                                        )}
                                                    </TableBody>
                                                </Table>
                                            </div>
                                        </TabsContent>
                                    </Tabs>
                                </CardContent>
                                <CardFooter className="justify-between border-t bg-muted/10">
                                    <span className="text-xs text-muted-foreground">
                                        Changes to sitting blocks take effect immediately on your project schedule.
                                    </span>
                                    <Link
                                        to={`/projects?projectId=${projectId}`}
                                        className={cn(buttonVariants({variant: 'link'}), 'px-0 text-xs')}
                                    >
                                        Go to project page →
                                    </Link>
                                </CardFooter>
                            </Card>
                        </div>
                    ) : null}
                </>
            )}
        </div>
    )
}
