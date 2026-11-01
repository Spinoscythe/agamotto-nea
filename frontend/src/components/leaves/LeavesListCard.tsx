import { CalendarX2, Clock, Plus, Trash2 } from 'lucide-react'
import type { LeaveResponse } from '@/api'
import { Badge } from '@/components/ui/badge'
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
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import { formatLeaveRange, LEAVE_TYPES } from './leave-utils'

interface LeavesListCardProps {
  leaves: LeaveResponse[]
  filteredLeaves: LeaveResponse[]
  loading: boolean
  selectedFilter: string
  onFilterChange: (value: string) => void
  onAddFirst: () => void
  onDelete: (id: string, leaveTitle: string) => void
}

export function LeavesListCard({
  leaves,
  filteredLeaves,
  loading,
  selectedFilter,
  onFilterChange,
  onAddFirst,
  onDelete,
}: LeavesListCardProps) {
  return (
    <Card className="p-0">
      <CardHeader className="border-b p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <CardTitle className="text-base font-semibold">Your Leaves & Breaks</CardTitle>
            <CardDescription className="text-xs">
              All personal and system-wide blocked times
            </CardDescription>
          </div>

          <div className="flex flex-wrap gap-1">
            <button
              type="button"
              onClick={() => onFilterChange('ALL')}
              className={cn(
                'rounded-full px-2.5 py-1 text-xs font-medium transition-colors',
                selectedFilter === 'ALL'
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              )}
            >
              All ({leaves.length})
            </button>
            {LEAVE_TYPES.map((t) => {
              const count = leaves.filter((l) => l.type === t.value).length
              if (count === 0 && selectedFilter !== t.value) return null
              return (
                <button
                  key={t.value}
                  type="button"
                  onClick={() => onFilterChange(t.value)}
                  className={cn(
                    'rounded-full px-2.5 py-1 text-xs font-medium transition-colors',
                    selectedFilter === t.value
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-muted text-muted-foreground hover:bg-muted/80'
                  )}
                >
                  {t.label.split('/')[0].trim()} ({count})
                </button>
              )
            })}
          </div>
        </div>
      </CardHeader>

      <CardContent className="p-0">
        {loading ? (
          <div className="flex flex-col gap-3 p-4">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        ) : filteredLeaves.length === 0 ? (
          <Empty className="py-12">
            <EmptyHeader>
              <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-muted/60 text-muted-foreground">
                <CalendarX2 className="size-6" />
              </div>
              <EmptyTitle className="mt-3 text-sm font-semibold">No leaves scheduled</EmptyTitle>
              <EmptyDescription className="text-xs">
                {selectedFilter === 'ALL'
                  ? 'Book vacations or rest intervals to prevent Agamotto from cramming tasks when you need time off.'
                  : 'No absences found under this filter category.'}
              </EmptyDescription>
            </EmptyHeader>
            {selectedFilter === 'ALL' ? (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={onAddFirst}
                className="mt-3 gap-1.5"
              >
                <Plus className="size-4" />
                Add First Leave
              </Button>
            ) : null}
          </Empty>
        ) : (
          <div className="divide-y">
            {filteredLeaves.map((leave) => {
              const config = LEAVE_TYPES.find((t) => t.value === leave.type) ?? LEAVE_TYPES[0]
              const Icon = config.icon
              const { formatted, durationHours } = formatLeaveRange(leave.startTime, leave.endTime)
              const isPast = new Date(leave.endTime).getTime() < Date.now()

              return (
                <div
                  key={leave.id}
                  className={cn(
                    'flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 transition-colors hover:bg-muted/30',
                    isPast && 'opacity-60'
                  )}
                >
                  <div className="flex items-start gap-3">
                    <div
                      className={cn(
                        'flex size-10 shrink-0 items-center justify-center rounded-lg border',
                        config.color
                      )}
                    >
                      <Icon className="size-5" />
                    </div>
                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="font-semibold text-sm leading-none">{leave.title}</p>
                        <Badge variant="outline" className={cn('text-[0.65rem] px-1.5 py-0', config.color)}>
                          {config.label.split('/')[0].trim()}
                        </Badge>
                        {leave.isGlobal ? (
                          <Badge variant="secondary" className="text-[0.65rem] px-1.5 py-0">
                            System Holiday
                          </Badge>
                        ) : null}
                        {isPast ? (
                          <Badge variant="secondary" className="text-[0.65rem] px-1.5 py-0">
                            Completed
                          </Badge>
                        ) : null}
                      </div>
                      <p className="text-xs text-muted-foreground flex items-center gap-1.5">
                        <Clock className="size-3 shrink-0" />
                        <span>{formatted}</span>
                        <span>•</span>
                        <span className="font-medium">{durationHours}h total</span>
                      </p>
                      {leave.reason ? (
                        <p className="text-xs text-muted-foreground/90 italic">
                          "{leave.reason}"
                        </p>
                      ) : null}
                    </div>
                  </div>

                  {!leave.isGlobal ? (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="self-end sm:self-center text-muted-foreground hover:text-destructive hover:bg-destructive/10"
                      onClick={() => onDelete(leave.id, leave.title)}
                      title="Delete leave"
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  ) : null}
                </div>
              )
            })}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
