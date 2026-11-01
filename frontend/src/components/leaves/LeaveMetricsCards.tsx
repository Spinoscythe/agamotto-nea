import { Calendar, CalendarX2, Clock } from 'lucide-react'
import type { LeaveResponse } from '@/api'
import { Card } from '@/components/ui/card'

interface LeaveMetricsCardsProps {
  leavesCount: number
  totalLeaveHours: number
  nextUpcomingLeave: LeaveResponse | null
}

export function LeaveMetricsCards({
  leavesCount,
  totalLeaveHours,
  nextUpcomingLeave,
}: LeaveMetricsCardsProps) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <Card className="p-4">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-lg border bg-muted/60 text-primary">
            <CalendarX2 className="size-5" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Booked Absences</p>
            <p className="text-xl font-semibold">{leavesCount}</p>
          </div>
        </div>
      </Card>

      <Card className="p-4">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-lg border bg-muted/60 text-primary">
            <Clock className="size-5" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Protected Hours</p>
            <p className="text-xl font-semibold">{Math.round(totalLeaveHours)}h</p>
          </div>
        </div>
      </Card>

      <Card className="p-4">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-lg border bg-muted/60 text-primary">
            <Calendar className="size-5" />
          </div>
          <div className="min-w-0">
            <p className="text-xs text-muted-foreground">Next Scheduled Break</p>
            <p className="truncate text-sm font-semibold">
              {nextUpcomingLeave ? nextUpcomingLeave.title : 'None scheduled'}
            </p>
          </div>
        </div>
      </Card>
    </div>
  )
}
