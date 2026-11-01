import {type SubmitEvent, useEffect, useMemo, useRef, useState} from 'react'
import {Link, useSearchParams} from 'react-router-dom'
import {toast} from 'sonner'
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
import {PageHeader} from '@/components/PageHeader'
import {ScheduleBlockSidebar, type ScheduleBlockSaveValues} from '@/components/ScheduleBlockSidebar'
import {GeneratedScheduleCard} from '@/components/schedule/GeneratedScheduleCard'
import {ProjectSetupCard} from '@/components/schedule/ProjectSetupCard'
import {TaskCreationCard} from '@/components/schedule/TaskCreationCard'
import {emptyTaskDraft, type TaskDraft} from '@/components/schedule/task-draft'
import {Alert, AlertDescription} from '@/components/ui/alert'
import {buttonVariants} from '@/components/ui/button'
import {Empty, EmptyDescription, EmptyHeader, EmptyTitle} from '@/components/ui/empty'
import {addDays, deadlineDatePart, toDateInputValue, toLocalDateTimePayload} from '@/lib/datetime'
import {cn} from '@/lib/utils'

export function GenerateSchedulePage() {
    const {user} = useAuth()
    const [params] = useSearchParams()
    const [projects, setProjects] = useState<ProjectResponse[]>([])
    const [projectId, setProjectId] = useState(params.get('projectId') ?? '')
    const [tasks, setTasks] = useState<TaskResponse[]>([])
    const [draft, setDraft] = useState<TaskDraft>(() => emptyTaskDraft())
    const [startDate, setStartDate] = useState(() => toDateInputValue())
    const [endDate, setEndDate] = useState(() => {
        const d = new Date()
        d.setDate(d.getDate() + 7)
        return toDateInputValue(d)
    })
    const [result, setResult] = useState<SchedulePlanResponse | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [busy, setBusy] = useState(false)
    const [selectedBlockId, setSelectedBlockId] = useState('')
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

    const selectedBlock = useMemo(
        () => result?.blocks?.find((b) => b.id === selectedBlockId) ?? null,
        [result?.blocks, selectedBlockId],
    )
    const selectedTask = useMemo(
        () => (selectedBlock ? taskById.get(selectedBlock.taskId) ?? null : null),
        [selectedBlock, taskById],
    )
    const selectedSitting = selectedBlock ? sittingByBlockId.get(selectedBlock.id) ?? null : null

    useEffect(() => {
        if (!selectedBlockId) return
        if (!result?.blocks?.some((b) => b.id === selectedBlockId)) {
            setSelectedBlockId('')
        }
    }, [result?.blocks, selectedBlockId])

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
        if (start) {
            setStartDate(start)
            setDraft((current) => ({...current, deadline: addDays(start, 7)}))
        }
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
        const deadlineDate = deadlineDatePart(draft.deadline)
        const projectStart = deadlineDatePart(selectedProject?.startDate)
        const projectEnd = deadlineDatePart(selectedProject?.endDate)
        if (deadlineDate && projectStart && deadlineDate < projectStart) {
            setError(`Deadline must be on or after the project start date (${projectStart}).`)
            return
        }
        if (deadlineDate && projectEnd && deadlineDate > projectEnd) {
            setError(`Deadline must be on or before the project end date (${projectEnd}).`)
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
                status: draft.status,
            })
            if (projectIdRef.current === targetProjectId) {
                setTasks((prev) => [...prev, created])
                setDraft(emptyTaskDraft(selectedProject?.startDate))
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

    async function saveBlockDetails(blockId: string, values: ScheduleBlockSaveValues) {
        setError(null)
        setBusy(true)
        const previousCompleted = result?.blocks?.find((b) => b.id === blockId)?.completed
        try {
            const updated = await schedulesApi.overrideBlock(blockId, {
                startTime: values.startTime,
                endTime: values.endTime,
                completed: values.completed,
            })
            replaceBlock(updated)
            if (previousCompleted !== undefined && previousCompleted !== values.completed) {
                setTasks((prev) =>
                    prev.map((task) =>
                        task.id === updated.taskId && task.status !== 'CANCELLED'
                            ? {
                                  ...task,
                                  status: values.completed ? 'COMPLETED' : 'IN_PROGRESS',
                              }
                            : task,
                    ),
                )
            }
            setSelectedBlockId('')
            toast.success('Sitting updated.')
        } catch (err) {
            const msg = err instanceof ApiError ? err.message : 'Could not update sitting'
            setError(msg)
            toast.error(msg)
            throw err
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
                    <ProjectSetupCard
                        projects={projects}
                        projectId={projectId}
                        selectedProject={selectedProject}
                        startDate={startDate}
                        endDate={endDate}
                        onProjectChange={setProjectId}
                        onStartDateChange={setStartDate}
                        onEndDateChange={setEndDate}
                    />

                    <TaskCreationCard
                        tasks={tasks}
                        draft={draft}
                        onDraftChange={setDraft}
                        busy={busy}
                        projectId={projectId}
                        selectedProject={selectedProject}
                        schedulableCount={schedulableCount}
                        onAddTask={addTask}
                        onRemoveTask={(taskId) => void removeTask(taskId)}
                        onGenerate={generate}
                    />

                    {result ? (
                        <div ref={resultRef} className="flex flex-col gap-4">
                            <GeneratedScheduleCard
                                result={result}
                                tasks={tasks}
                                taskById={taskById}
                                scheduledBlocks={scheduledBlocks}
                                totalScheduledHours={totalScheduledHours}
                                tasksScheduledCount={tasksScheduledCount}
                                sittingByBlockId={sittingByBlockId}
                                selectedBlockId={selectedBlockId}
                                projectId={projectId}
                                busy={busy}
                                onSelectBlock={setSelectedBlockId}
                                onMarkUnattended={markUnattended}
                                onDelayBlock={(blockId) => void delayBlock(blockId)}
                                onDeleteTask={(taskId) => void removeTask(taskId)}
                            />
                        </div>
                    ) : null}
                </>
            )}

            <ScheduleBlockSidebar
                block={selectedBlock}
                task={selectedTask}
                sitting={selectedSitting}
                scheduleStartDate={result?.startDate}
                scheduleEndDate={result?.endDate}
                busy={busy}
                onClose={() => setSelectedBlockId('')}
                onSave={saveBlockDetails}
            />
        </div>
    )
}
