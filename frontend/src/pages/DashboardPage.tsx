import {useEffect, useState} from 'react'
import {Link} from 'react-router-dom'
import {
    ApiError,
    dashboardApi,
    notificationsApi,
    type DashboardReportResponse,
    type NotificationResponse,
    type ReportPeriod,
} from '@/api'
import {useAuth} from '@/auth/AuthContext'
import {PageHeader} from '@/components/PageHeader'
import {Alert, AlertDescription} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {
    Card,
    CardAction,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from '@/components/ui/card'
import {
    Empty,
    EmptyContent,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
} from '@/components/ui/empty'
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select'
import {Skeleton} from '@/components/ui/skeleton'
import {NotificationItem} from "@/components/NotificationItem.tsx";

const PERIODS: ReportPeriod[] = ['DAILY', 'WEEKLY', 'MONTHLY']

const metrics = [
    {key: 'scheduledCount', label: 'Scheduled'},
    {key: 'delayedCount', label: 'Delayed'},
    {key: 'excludedCount', label: 'Excluded'},
    {key: 'completedCount', label: 'Completed'},
] as const

export function DashboardPage() {
    const {user} = useAuth()
    const [period, setPeriod] = useState<ReportPeriod>('WEEKLY')
    const [report, setReport] = useState<DashboardReportResponse | null>(null)
    const [notifications, setNotifications] = useState<NotificationResponse[]>([])
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        if (!user) return
        let cancelled = false
        setLoading(true)
        setError(null)
        ;(async () => {
            const [dashResult, notesResult] = await Promise.allSettled([
                dashboardApi.get(user.id, period),
                notificationsApi.listUnread(user.id),
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

    return (
        <div className="flex flex-col gap-6">
            <PageHeader
                title={`Hello, ${(user?.fullName || user?.displayName)?.split(' ')[0] ?? 'there'}`}
                description="Summary of scheduled work for the selected period."
                actions={
                    <Select
                        value={period}
                        onValueChange={(value) => {
                            if (value != null) setPeriod(value as ReportPeriod)
                        }}
                    >
                        <SelectTrigger className="w-36" aria-label="Report period">
                            <SelectValue/>
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
                }
            />

            {error ? (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            ) : null}

            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {metrics.map((metric) => (
                    <Card key={metric.key} size="sm">
                        <CardHeader>
                            <CardDescription>{metric.label}</CardDescription>
                            <CardTitle className="text-2xl tabular-nums">
                                {loading ? (
                                    <Skeleton className="h-7 w-10"/>
                                ) : (
                                    (report?.[metric.key] ?? 0)
                                )}
                            </CardTitle>
                        </CardHeader>
                    </Card>
                ))}
            </div>

            {report && !loading ? (
                <p className="text-sm text-muted-foreground">
                    Period: {report.periodStart} to {report.periodEnd}. Generated{' '}
                    {new Date(report.generatedAt).toLocaleString()}.
                </p>
            ) : null}

            <Card>
                <CardHeader>
                    <CardTitle>Unread notifications</CardTitle>
                    <CardDescription>Recent alerts from schedule and task updates.</CardDescription>
                    {!loading && notifications.length > 0 ? (
                        <CardAction>
                            <Button variant="link" size="sm" render={<Link to="/notifications"/>} nativeButton={false}>
                                View all
                            </Button>
                        </CardAction>
                    ) : null}
                </CardHeader>
                <CardContent>
                    {loading ? (
                        <div className="flex flex-col gap-2">
                            <Skeleton className="h-12 w-full"/>
                            <Skeleton className="h-12 w-full"/>
                        </div>
                    ) : notifications.length === 0 ? (
                        <Empty>
                            <EmptyHeader>
                                <EmptyTitle>All caught up</EmptyTitle>
                                <EmptyDescription>No unread notifications.</EmptyDescription>
                            </EmptyHeader>
                            <EmptyContent>
                                <Button variant="link" render={<Link to="/notifications"/>} nativeButton={false}>
                                    View all
                                </Button>
                            </EmptyContent>
                        </Empty>
                    ) : (
                        <div className="divide-y rounded-md border overflow-hidden">
                            {notifications.slice(0, 5).map((n) => (
                                <NotificationItem
                                    key={n.id}
                                    notification={n}
                                    onMarkRead={markRead}
                                    compact
                                />
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    )
}
