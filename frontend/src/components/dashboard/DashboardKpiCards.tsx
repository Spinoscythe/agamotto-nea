import { CalendarCheck, CheckCircle2, Clock, XCircle } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'

interface DashboardKpiCardsProps {
  loading: boolean
  scheduled: number
  completed: number
  delayed: number
  excluded: number
}

export function DashboardKpiCards({
  loading,
  scheduled,
  completed,
  delayed,
  excluded,
}: DashboardKpiCardsProps) {
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
      <Card className="p-4 transition-shadow hover:shadow-xs">
        <div className="flex items-center justify-between text-muted-foreground">
          <span className="text-xs font-medium uppercase tracking-wider">Scheduled</span>
          <CalendarCheck className="size-4 text-blue-500" />
        </div>
        <p className="mt-2 text-3xl font-bold tabular-nums">
          {loading ? <Skeleton className="h-8 w-12" /> : scheduled}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">Active in plans</p>
      </Card>

      <Card className="p-4 transition-shadow hover:shadow-xs">
        <div className="flex items-center justify-between text-muted-foreground">
          <span className="text-xs font-medium uppercase tracking-wider">Completed</span>
          <CheckCircle2 className="size-4 text-emerald-500" />
        </div>
        <p className="mt-2 text-3xl font-bold tabular-nums text-emerald-600 dark:text-emerald-400">
          {loading ? <Skeleton className="h-8 w-12" /> : completed}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">Finished tasks</p>
      </Card>

      <Card className="p-4 transition-shadow hover:shadow-xs">
        <div className="flex items-center justify-between text-muted-foreground">
          <span className="text-xs font-medium uppercase tracking-wider">Delayed</span>
          <Clock className="size-4 text-amber-500" />
        </div>
        <p className="mt-2 text-3xl font-bold tabular-nums text-amber-600 dark:text-amber-400">
          {loading ? <Skeleton className="h-8 w-12" /> : delayed}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">Pushed back</p>
      </Card>

      <Card className="p-4 transition-shadow hover:shadow-xs">
        <div className="flex items-center justify-between text-muted-foreground">
          <span className="text-xs font-medium uppercase tracking-wider">Excluded</span>
          <XCircle className="size-4 text-rose-500" />
        </div>
        <p className="mt-2 text-3xl font-bold tabular-nums text-rose-600 dark:text-rose-400">
          {loading ? <Skeleton className="h-8 w-12" /> : excluded}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">Over deadline</p>
      </Card>
    </div>
  )
}
