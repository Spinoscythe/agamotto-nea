import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Sparkles } from 'lucide-react'
import {
    ApiError,
    dashboardApi,
    leavesApi,
    notificationsApi,
    projectsApi,
    type DashboardReportResponse,
    type LeaveResponse,
    type NotificationResponse,
    type ProjectResponse,
    type ReportPeriod,
} from '@/api'
import { useAuth } from '@/auth/AuthContext'
import { DashboardKpiCards } from '@/components/dashboard/DashboardKpiCards'
import { NotificationsFeedCard } from '@/components/dashboard/NotificationsFeedCard'
import { PlannedTimeOffCard } from '@/components/dashboard/PlannedTimeOffCard'
import { ProjectsOverviewCard } from '@/components/dashboard/ProjectsOverviewCard'
import { QuickActionsCard } from '@/components/dashboard/QuickActionsCard'
import { ScheduleDistributionCard } from '@/components/dashboard/ScheduleDistributionCard'
import { PageHeader } from '@/components/PageHeader'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { buttonVariants } from '@/components/ui/button'
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select'
import { cn } from '@/lib/utils'

const PERIODS: ReportPeriod[] = ['DAILY', 'WEEKLY', 'MONTHLY']

export function DashboardPage() {
    const { user } = useAuth()
    const [period, setPeriod] = useState<ReportPeriod>('WEEKLY')
    const [report, setReport] = useState<DashboardReportResponse | null>(null)
    const [notifications, setNotifications] = useState<NotificationResponse[]>([])
    const [projects, setProjects] = useState<ProjectResponse[]>([])
    const [leaves, setLeaves] = useState<LeaveResponse[]>([])
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        if (!user) return
        let cancelled = false
        setLoading(true)
        setError(null)
            ; (async () => {
                const [dashResult, notesResult, projectsResult, leavesResult] = await Promise.allSettled([
                    dashboardApi.get(user.id, period),
                    notificationsApi.listUnread(user.id),
                    projectsApi.list(user.id),
                    leavesApi.list(),
                ])
                if (cancelled) return

                const errors: string[] = []

                if (dashResult.status === 'fulfilled') {
                    setReport(dashResult.value)
                } else {
                    setReport(null)
                    const err = dashResult.reason
                    if (!(err instanceof ApiError && err.status === 401)) {
                        errors.push(err instanceof ApiError ? err.message : 'Failed to load dashboard')
                    }
                }

                if (notesResult.status === 'fulfilled') {
                    setNotifications(notesResult.value)
                } else {
                    setNotifications([])
                    const err = notesResult.reason
                    if (!(err instanceof ApiError && err.status === 401)) {
                        errors.push(err instanceof ApiError ? err.message : 'Failed to load notifications')
                    }
                }

                if (projectsResult.status === 'fulfilled') {
                    setProjects(projectsResult.value)
                } else {
                    setProjects([])
                }

                if (leavesResult.status === 'fulfilled') {
                    setLeaves(leavesResult.value)
                } else {
                    setLeaves([])
                }

                setError(errors.length > 0 ? errors.join(' · ') : null)
                setLoading(false)
            })()

        return () => {
            cancelled = true
        }
    }, [user, period])

    async function markRead(id: string) {
        try {
            await notificationsApi.markRead(id)
            setNotifications((prev) => prev.filter((n) => n.id !== id))
        } catch (err) {
            setError(err instanceof ApiError ? err.message : 'Could not mark notification read')
        }
    }

    // Calculate metrics and percentages
    const scheduled = report?.scheduledCount ?? 0
    const delayed = report?.delayedCount ?? 0
    const excluded = report?.excludedCount ?? 0
    const completed = report?.completedCount ?? 0
    const totalTasks = scheduled + delayed + excluded + completed
    const completionRate = totalTasks > 0 ? Math.round((completed / totalTasks) * 100) : 0

    return (
        <div className="flex flex-col gap-6">
            <PageHeader
                title={`Hello, ${(user?.fullName || user?.displayName)?.split(' ')[0] ?? 'there'}`}
                description="Here is what's on your agenda and the performance overview for this period."
                actions={
                    <div className="flex items-center gap-3">
                        <Select
                            value={period}
                            onValueChange={(value) => {
                                if (value != null) setPeriod(value as ReportPeriod)
                            }}
                        >
                            <SelectTrigger className="w-36" aria-label="Report period">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectGroup>
                                    {PERIODS.map((p) => (
                                        <SelectItem key={p} value={p}>
                                            {p.charAt(0) + p.slice(1).toLowerCase()}
                                        </SelectItem>
                                    ))}
                                </SelectGroup>
                            </SelectContent>
                        </Select>

                        <Link
                            to="/generate"
                            className={cn(buttonVariants({ size: 'sm' }), 'hidden sm:inline-flex')}
                        >
                            <Sparkles className="size-4" />
                            Generate schedule
                        </Link>
                    </div>
                }
            />

            {error ? (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            ) : null}

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
                <div className="flex flex-col gap-6 lg:col-span-8">
                    <DashboardKpiCards
                        loading={loading}
                        scheduled={scheduled}
                        completed={completed}
                        delayed={delayed}
                        excluded={excluded}
                    />

                    <ScheduleDistributionCard
                        loading={loading}
                        period={period}
                        scheduled={scheduled}
                        delayed={delayed}
                        excluded={excluded}
                        completed={completed}
                        totalTasks={totalTasks}
                        completionRate={completionRate}
                    />

                    <ProjectsOverviewCard loading={loading} projects={projects} />
                </div>

                <div className="flex flex-col gap-6 lg:col-span-4">
                    <QuickActionsCard />
                    <PlannedTimeOffCard loading={loading} leaves={leaves} />
                    <NotificationsFeedCard
                        loading={loading}
                        notifications={notifications}
                        onMarkRead={markRead}
                    />
                </div>
            </div>
        </div>
    )
}
