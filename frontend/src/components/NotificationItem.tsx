import {
    CalendarCheckIcon,
    WarningIcon,
    ClockIcon,
    CheckIcon,
    BellIcon,
} from '@phosphor-icons/react'
import type { NotificationResponse } from '@/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { formatNotification } from '@/lib/notification-format'
import { cn } from '@/lib/utils'

interface NotificationItemProps {
    notification: NotificationResponse
    onMarkRead: (id: string) => void
    compact?: boolean
}

export function NotificationItem({
                                     notification,
                                     onMarkRead,
                                     compact = false,
                                 }: NotificationItemProps) {
    const parsed = formatNotification(notification)

    return (
        <div
            className={cn(
                'group flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 transition-colors hover:bg-muted/40',
                compact && 'p-3 gap-2',
            )}
        >
            <div className="flex items-start gap-3.5 min-w-0">
                {/* Category Icon */}
                <div
                    className={cn(
                        'flex size-10 shrink-0 items-center justify-center rounded-xl border transition-shadow',
                        parsed.mode === 'SERENITY' &&
                        'border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
                        parsed.mode === 'CRUNCH' &&
                        'border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400',
                        parsed.category === 'SCHEDULE' && !parsed.mode &&
                        'border-violet-500/30 bg-violet-500/10 text-violet-600 dark:text-violet-400',
                        parsed.category === 'TASK' &&
                        'border-sky-500/30 bg-sky-500/10 text-sky-600 dark:text-sky-400',
                        parsed.category === 'GENERAL' &&
                        'border-border bg-muted text-muted-foreground',
                    )}
                >
                    {parsed.mode === 'SERENITY' ? (
                        <CalendarCheckIcon className="size-5" weight="duotone" />
                    ) : parsed.mode === 'CRUNCH' ? (
                        <WarningIcon className="size-5" weight="duotone" />
                    ) : parsed.category === 'SCHEDULE' ? (
                        <CalendarCheckIcon className="size-5" weight="duotone" />
                    ) : parsed.category === 'TASK' ? (
                        <ClockIcon className="size-5" weight="duotone" />
                    ) : (
                        <BellIcon className="size-5" weight="duotone" />
                    )}
                </div>

                {/* Content */}
                <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
            <span className="font-semibold text-sm text-foreground tracking-tight">
              {parsed.title}
            </span>

                        {parsed.mode === 'SERENITY' && (
                            <Badge
                                variant="secondary"
                                className="border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 text-[11px] h-5"
                            >
                                Relaxed Pace
                            </Badge>
                        )}
                        {parsed.mode === 'CRUNCH' && (
                            <Badge variant="destructive" className="text-[11px] h-5">
                                High Workload
                            </Badge>
                        )}

                        <span
                            className="text-xs text-muted-foreground ml-auto sm:ml-0"
                            title={new Date(notification.createdAt).toLocaleString()}
                        >
              • {parsed.relativeTime}
            </span>
                    </div>

                    <p className="text-xs sm:text-sm text-muted-foreground leading-relaxed break-words">
                        {parsed.body}
                    </p>

                    {/* Workload vs Capacity Badges */}
                    {parsed.stats?.workload && parsed.stats?.capacity && (
                        <div className="flex items-center gap-2 pt-1 text-xs">
              <span className="inline-flex items-center gap-1 rounded-md bg-secondary/80 px-2 py-0.5 font-medium text-foreground text-[11px]">
                Workload: <strong className="font-semibold">{parsed.stats.workload}</strong>
              </span>
                            <span className="inline-flex items-center gap-1 rounded-md bg-secondary/80 px-2 py-0.5 font-medium text-foreground text-[11px]">
                Available: <strong className="font-semibold">{parsed.stats.capacity}</strong>
              </span>
                        </div>
                    )}
                </div>
            </div>

            {/* Action Buttons */}
            <div className="flex shrink-0 items-center gap-2 self-end sm:self-center">
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onMarkRead(notification.id)}
                    className="text-xs text-muted-foreground hover:text-foreground hover:bg-muted"
                    title="Mark as read"
                >
                    <CheckIcon className="size-3.5 mr-1" />
                    Mark read
                </Button>
            </div>
        </div>
    )
}
