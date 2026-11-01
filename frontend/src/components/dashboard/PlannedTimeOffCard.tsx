import { Link } from 'react-router-dom'
import type { LeaveResponse } from '@/api'
import { Badge } from '@/components/ui/badge'
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
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'

interface PlannedTimeOffCardProps {
  loading: boolean
  leaves: LeaveResponse[]
}

export function PlannedTimeOffCard({ loading, leaves }: PlannedTimeOffCardProps) {
  return (
    <Card className="p-5">
      <CardHeader className="p-0">
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-base font-semibold">Planned Time Off</CardTitle>
            <CardDescription className="text-xs">Protected vacations and study breaks</CardDescription>
          </div>
          <CardAction>
            <Button
              variant="link"
              size="sm"
              render={<Link to="/leaves" />}
              nativeButton={false}
            >
              Manage
            </Button>
          </CardAction>
        </div>
      </CardHeader>
      <CardContent className="mt-4 p-0">
        {loading ? (
          <Skeleton className="h-14 w-full" />
        ) : leaves.length === 0 ? (
          <Empty className="py-4">
            <EmptyHeader>
              <EmptyTitle className="text-xs">No active leaves</EmptyTitle>
              <EmptyDescription className="text-[0.7rem]">Book time off so the engine won't schedule tasks.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <div className="divide-y rounded-md border overflow-hidden">
            {leaves.slice(0, 3).map((leave) => (
              <div key={leave.id} className="flex items-center justify-between p-2.5 text-xs">
                <div className="flex items-center gap-2 min-w-0">
                  <span className="text-sm">
                    {leave.type === 'VACATION' ? '🌴' : leave.type === 'HOLIDAY' ? '🎉' : '☕'}
                  </span>
                  <div className="truncate">
                    <p className="font-medium truncate">{leave.title}</p>
                    <p className="text-[0.65rem] text-muted-foreground">
                      {new Date(leave.startTime).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                      {' – '}
                      {new Date(leave.endTime).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                    </p>
                  </div>
                </div>
                <Badge variant="outline" className="text-[0.65rem] px-1.5 py-0 shrink-0">
                  {leave.type.replace('_', ' ')}
                </Badge>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
