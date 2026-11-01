import { TrendingUp } from 'lucide-react'
import type { ReportPeriod } from '@/api'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'

interface ScheduleDistributionCardProps {
  loading: boolean
  period: ReportPeriod
  scheduled: number
  delayed: number
  excluded: number
  completed: number
  totalTasks: number
  completionRate: number
}

export function ScheduleDistributionCard({
  loading,
  period,
  scheduled,
  delayed,
  excluded,
  completed,
  totalTasks,
  completionRate,
}: ScheduleDistributionCardProps) {
  return (
    <Card className="p-5">
      <CardHeader className="p-0">
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-base font-semibold">Schedule Distribution</CardTitle>
            <CardDescription className="text-xs">
              Task status composition across this {period.toLowerCase()} period.
            </CardDescription>
          </div>
          {!loading && totalTasks > 0 ? (
            <Badge variant="outline" className="gap-1 px-2.5 py-1 text-xs font-normal">
              <TrendingUp className="size-3 text-emerald-500" />
              {completionRate}% Completion
            </Badge>
          ) : null}
        </div>
      </CardHeader>

      <CardContent className="mt-4 p-0">
        {loading ? (
          <Skeleton className="h-4 w-full rounded-full" />
        ) : totalTasks === 0 ? (
          <p className="text-xs text-muted-foreground">No schedule activity recorded for this period.</p>
        ) : (
          <div className="flex flex-col gap-3">
            <div className="flex h-3 w-full overflow-hidden rounded-full bg-muted">
              {completed > 0 ? (
                <div
                  style={{ width: `${(completed / totalTasks) * 100}%` }}
                  className="bg-emerald-500 transition-all"
                  title={`Completed: ${completed}`}
                />
              ) : null}
              {scheduled > 0 ? (
                <div
                  style={{ width: `${(scheduled / totalTasks) * 100}%` }}
                  className="bg-blue-500 transition-all"
                  title={`Scheduled: ${scheduled}`}
                />
              ) : null}
              {delayed > 0 ? (
                <div
                  style={{ width: `${(delayed / totalTasks) * 100}%` }}
                  className="bg-amber-500 transition-all"
                  title={`Delayed: ${delayed}`}
                />
              ) : null}
              {excluded > 0 ? (
                <div
                  style={{ width: `${(excluded / totalTasks) * 100}%` }}
                  className="bg-rose-500 transition-all"
                  title={`Excluded: ${excluded}`}
                />
              ) : null}
            </div>

            <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <span className="size-2.5 rounded-full bg-emerald-500" /> Completed ({completed})
              </span>
              <span className="flex items-center gap-1.5">
                <span className="size-2.5 rounded-full bg-blue-500" /> Scheduled ({scheduled})
              </span>
              <span className="flex items-center gap-1.5">
                <span className="size-2.5 rounded-full bg-amber-500" /> Delayed ({delayed})
              </span>
              <span className="flex items-center gap-1.5">
                <span className="size-2.5 rounded-full bg-rose-500" /> Excluded ({excluded})
              </span>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
