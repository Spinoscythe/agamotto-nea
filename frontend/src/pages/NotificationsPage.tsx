import {useEffect, useState, useMemo} from 'react'
import {Link} from 'react-router-dom'
import {CheckCheck} from 'lucide-react'
import {
    ApiError,
    notificationsApi,
    type NotificationResponse,
} from '@/api'
import {useAuth} from '@/auth/AuthContext'
import {PageHeader} from '@/components/PageHeader'
import {NotificationItem} from '@/components/NotificationItem'
import {Alert, AlertDescription} from '@/components/ui/alert'
import {Button, buttonVariants} from '@/components/ui/button'
import {Card, CardContent} from '@/components/ui/card'
import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs'
import {
    Empty,
    EmptyContent,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
} from '@/components/ui/empty'
import {cn} from '@/lib/utils'

export function NotificationsPage() {
    const {user} = useAuth()
    const [notifications, setNotifications] = useState<NotificationResponse[]>([])
    const [filter, setFilter] = useState<'ALL' | 'SCHEDULE' | 'TASK'>('ALL')
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        if (!user) return
        let cancelled = false
        setLoading(true)
        void notificationsApi
            .listUnread(user.id)
            .then((list) => {
                if (!cancelled) setNotifications(list)
            })
            .catch((err) => {
                if (!cancelled) {
                    setError(err instanceof ApiError ? err.message : 'Failed to load notifications')
                }
            })
            .finally(() => {
                if (!cancelled) setLoading(false)
            })
        return () => {
            cancelled = true
        }
    }, [user])

    async function markRead(id: string) {
        try {
            await notificationsApi.markRead(id)
            setNotifications((prev) => prev.filter((n) => n.id !== id))
        } catch (err) {
            setError(err instanceof ApiError ? err.message : 'Could not mark notification read')
        }
    }

    async function markAllRead() {
        try {
            await Promise.all(notifications.map((n) => notificationsApi.markRead(n.id)))
            setNotifications([])
        } catch (err) {
            setError(err instanceof ApiError ? err.message : 'Could not clear notifications')
        }
    }

    const filtered = useMemo(() => {
        if (filter === 'ALL') return notifications
        if (filter === 'SCHEDULE') {
            return notifications.filter((n) => n.type === 'SCHEDULE' || /schedule|serenity|crunch/i.test(n.message))
        }
        if (filter === 'TASK') {
            return notifications.filter((n) => n.taskId != null || /task/i.test(n.message))
        }
        return notifications
    }, [notifications, filter])

    return (
        <div className="flex flex-col gap-6">
            <PageHeader
                title="Notifications"
                description="Review schedule generation summaries and task updates."
                actions={
                    notifications.length > 0 ? (
                        <Button variant="outline" size="sm" onClick={() => void markAllRead()}>
                            <CheckCheck className="size-4 mr-1.5"/>
                            Mark all as read
                        </Button>
                    ) : undefined
                }
            />

            {error ? (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            ) : null}

            {/* Filter Tabs */}
            {notifications.length > 0 && (
                <Tabs value={filter} onValueChange={(val) => setFilter(val as typeof filter)}>
                    <TabsList>
                        <TabsTrigger value="ALL">All ({notifications.length})</TabsTrigger>
                        <TabsTrigger value="SCHEDULE">Schedules</TabsTrigger>
                        <TabsTrigger value="TASK">Tasks</TabsTrigger>
                    </TabsList>
                </Tabs>
            )}

            {loading ? (
                <p className="text-sm text-muted-foreground">Loading…</p>
            ) : notifications.length === 0 ? (
                <Empty className="border border-dashed">
                    <EmptyHeader>
                        <EmptyTitle>No unread notifications</EmptyTitle>
                        <EmptyDescription>You&apos;re all caught up for now.</EmptyDescription>
                    </EmptyHeader>
                    <EmptyContent>
                        <Link to="/projects" className={cn(buttonVariants({variant: 'outline'}))}>
                            View projects
                        </Link>
                    </EmptyContent>
                </Empty>
            ) : filtered.length === 0 ? (
                <p className="text-sm text-muted-foreground py-8 text-center">
                    No notifications matching this category.
                </p>
            ) : (
                <Card size="sm" className="py-0 overflow-hidden shadow-sm">
                    <CardContent className="p-0">
                        <div className="divide-y divide-border">
                            {filtered.map((n) => (
                                <NotificationItem
                                    key={n.id}
                                    notification={n}
                                    onMarkRead={markRead}
                                />
                            ))}
                        </div>
                    </CardContent>
                </Card>
            )}
        </div>
    )
}
