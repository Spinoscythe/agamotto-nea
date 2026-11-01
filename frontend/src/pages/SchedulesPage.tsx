import {useEffect, useRef, useState} from 'react'
import {Link, useSearchParams} from 'react-router-dom'
import {PlusIcon} from '@phosphor-icons/react'
import {toast} from 'sonner'
import {
    ApiError,
    projectsApi,
    schedulesApi,
    tasksApi,
    type ProjectResponse,
    type SchedulePlanResponse,
    type TaskResponse,
} from '@/api'
import {useAuth} from '@/auth/AuthContext'
import {PageHeader} from '@/components/PageHeader'
import {CreateProjectCard} from '@/components/projects/CreateProjectCard'
import {ProjectGrid} from '@/components/projects/ProjectGrid'
import {SelectedScheduleSection} from '@/components/projects/SelectedScheduleSection'
import {Alert, AlertDescription} from '@/components/ui/alert'
import {Button, buttonVariants} from '@/components/ui/button'
import {cn} from '@/lib/utils'
import {toDateInputValue} from '@/lib/datetime'

export function SchedulesPage() {
    const {user} = useAuth()
    const [searchParams, setSearchParams] = useSearchParams()
    const [projects, setProjects] = useState<ProjectResponse[]>([])
    const [selectedProjectId, setSelectedProjectId] = useState(
        searchParams.get('projectId') ?? '',
    )
    const [selectedPlan, setSelectedPlan] = useState<SchedulePlanResponse | null>(null)
    const [tasks, setTasks] = useState<TaskResponse[]>([])
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)
    const [showCreate, setShowCreate] = useState(false)
    const creatingRef = useRef(false)

    const [projectName, setProjectName] = useState('')
    const [projectDesc, setProjectDesc] = useState('')
    const [startDate, setStartDate] = useState(() => toDateInputValue())
    const [endDate, setEndDate] = useState(() => {
        const d = new Date()
        d.setDate(d.getDate() + 14)
        return toDateInputValue(d)
    })

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
            {replace: true},
        )
        let cancelled = false
        ;(async () => {
            try {
                const [list, taskList] = await Promise.all([
                    schedulesApi.listByProject(selectedProjectId),
                    tasksApi.listByProject(selectedProjectId),
                ])
                if (cancelled) return
                setSelectedPlan(list.find((p) => p.status === 'ACTIVE') ?? list[0] ?? null)
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
        if (!projectName.trim()) {
            setError('Project name is required.')
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
        creatingRef.current = true
        setError(null)
        try {
            const created = await projectsApi.create({
                ownerId: user.id,
                name: projectName.trim(),
                description: projectDesc.trim() || undefined,
                startDate,
                endDate,
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
        if (!window.confirm('Are you sure you want to delete this project? All associated tasks and schedules will be permanently deleted.')) {
            return
        }
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

    async function handleDeleteTask(taskId: string) {
        if (!user) return
        setError(null)
        try {
            await tasksApi.remove(taskId, user.id)
            setTasks((prev) => prev.filter((t) => t.id !== taskId))
            if (selectedProjectId) {
                const [updatedTasks, updatedPlans] = await Promise.all([
                    tasksApi.listByProject(selectedProjectId),
                    schedulesApi.listByProject(selectedProjectId),
                ])
                setTasks(updatedTasks)
                if (selectedPlan) {
                    const matchingPlan = updatedPlans.find((p) => p.id === selectedPlan.id)
                    setSelectedPlan(matchingPlan ?? updatedPlans.find((p) => p.status === 'ACTIVE') ?? updatedPlans[0] ?? null)
                }
            }
        } catch (err) {
            setError(err instanceof ApiError ? err.message : 'Could not delete task')
        }
    }

    async function handleMarkUnattended(blockId: string) {
        setError(null)
        try {
            const response = await schedulesApi.markUnattended(blockId)
            if (response.regeneratedPlan) {
                setSelectedPlan(response.regeneratedPlan)
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
                    <PlusIcon data-icon="inline-start"/>
                    {showCreate ? 'Cancel' : 'New project'}
                </Button>
            </div>

            {showCreate ? (
                <CreateProjectCard
                    projectName={projectName}
                    projectDesc={projectDesc}
                    startDate={startDate}
                    endDate={endDate}
                    onNameChange={setProjectName}
                    onDescriptionChange={setProjectDesc}
                    onStartDateChange={setStartDate}
                    onEndDateChange={setEndDate}
                    onSubmit={createProject}
                />
            ) : null}

            <ProjectGrid
                projects={projects}
                selectedProjectId={selectedProjectId}
                currentUserId={user?.id}
                loading={loading}
                onSelectProject={setSelectedProjectId}
                onDeleteProject={(projectId) => void deleteProject(projectId)}
                onCreateFirst={() => setShowCreate(true)}
            />

            <SelectedScheduleSection
                selectedPlan={selectedPlan}
                tasks={tasks}
                selectedProjectId={selectedProjectId}
                loading={loading}
                onDeleteTask={(taskId) => void handleDeleteTask(taskId)}
                onMarkUnattended={(blockId) => void handleMarkUnattended(blockId)}
            />
        </div>
    )
}
