import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ApiError,
  dashboardApi,
  notificationsApi,
  type DashboardReportResponse,
  type NotificationResponse,
  type ReportPeriod,
} from '@/api'
import { useAuth } from '@/auth/AuthContext'
import { PageHeader } from '@/components/PageHeader'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
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
import { Skeleton } from '@/components/ui/skeleton'

const PERIODS: ReportPeriod[] = ['DAILY', 'WEEKLY', 'MONTHLY']

const metrics = [
  { key: 'scheduledCount', label: 'Scheduled' },
  { key: 'delayedCount', label: 'Delayed' },
  { key: 'excludedCount', label: 'Excluded' },
  { key: 'completedCount', label: 'Completed' },
] as const

export function DashboardPage() {
  const { user } = useAuth()
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
        errors.push(err instanceof ApiError ? err.message : 'Failed to load dashboard')
      }

      if (notesResult.status === 'fulfilled') {
        setNotifications(notesResult.value)
      } else {
        setNotifications([])
        const err = notesResult.reason
        errors.push(err instanceof ApiError ? err.message : 'Failed to load notifications')
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
                  <Skeleton className="h-7 w-10" />
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
        <CardHeader className="flex-row items-center justify-between gap-2">
          <div>
            <CardTitle>Unread notifications</CardTitle>
            <CardDescription>Recent alerts from schedule and task updates.</CardDescription>
          </div>
          {!loading && notifications.length > 0 ? (
            <Button variant="link" render={<Link to="/notifications" />} nativeButton={false}>
              View all
            </Button>
          ) : null}
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex flex-col gap-2">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          ) : notifications.length === 0 ? (
            <Empty>
              <EmptyHeader>
                <EmptyTitle>All caught up</EmptyTitle>
                <EmptyDescription>No unread notifications.</EmptyDescription>
              </EmptyHeader>
              <EmptyContent>
                <Button variant="link" render={<Link to="/notifications" />} nativeButton={false}>
                  View all
                </Button>
              </EmptyContent>
            </Empty>
          ) : (
            <ul className="flex flex-col divide-y rounded-md border">
              {notifications.slice(0, 5).map((n) => (
                <li
                  key={n.id}
                  className="flex flex-wrap items-center justify-between gap-2 px-3 py-2.5"
                >
                  <div className="min-w-0">
                    <p className="text-sm">{n.message}</p>
                    <p className="text-xs text-muted-foreground">
                      {new Date(n.createdAt).toLocaleString()}
                    </p>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => void markRead(n.id)}
                  >
                    Mark read
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
