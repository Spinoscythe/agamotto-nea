import { Link } from 'react-router-dom'
import type { NotificationResponse } from '@/api'
import { NotificationItem } from '@/components/NotificationItem'
import { Button } from '@/components/ui/button'
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
import { Skeleton } from '@/components/ui/skeleton'

interface NotificationsFeedCardProps {
  loading: boolean
  notifications: NotificationResponse[]
  onMarkRead: (id: string) => void
}

export function NotificationsFeedCard({
  loading,
  notifications,
  onMarkRead,
}: NotificationsFeedCardProps) {
  return (
    <Card className="flex-1 p-5">
      <CardHeader className="p-0">
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-base font-semibold">Notifications</CardTitle>
            <CardDescription className="text-xs">Schedule updates & deadlines</CardDescription>
          </div>
          {!loading && notifications.length > 0 ? (
            <CardAction>
              <Button
                variant="link"
                size="sm"
                render={<Link to="/notifications" />}
                nativeButton={false}
              >
                View all
              </Button>
            </CardAction>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="mt-4 p-0">
        {loading ? (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        ) : notifications.length === 0 ? (
          <Empty className="py-6">
            <EmptyHeader>
              <EmptyTitle className="text-sm">All caught up</EmptyTitle>
              <EmptyDescription className="text-xs">No unread notifications.</EmptyDescription>
            </EmptyHeader>
            <EmptyContent>
              <Button
                variant="link"
                size="sm"
                render={<Link to="/notifications" />}
                nativeButton={false}
              >
                View all notifications
              </Button>
            </EmptyContent>
          </Empty>
        ) : (
          <div className="divide-y rounded-md border overflow-hidden">
            {notifications.slice(0, 5).map((notification) => (
              <NotificationItem
                key={notification.id}
                notification={notification}
                onMarkRead={onMarkRead}
                compact
              />
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
