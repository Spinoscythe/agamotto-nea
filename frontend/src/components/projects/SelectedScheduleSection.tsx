import { Link } from 'react-router-dom'
import type { SchedulePlanResponse, TaskResponse } from '@/api'
import { ScheduleWeekPreview } from '@/components/ScheduleWeekPreview'
import { buttonVariants } from '@/components/ui/button'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { planExplanation } from '@/lib/schedule'
import { cn } from '@/lib/utils'

interface SelectedScheduleSectionProps {
  selectedPlan: SchedulePlanResponse | null
  tasks: TaskResponse[]
  selectedProjectId: string
  loading: boolean
  onDeleteTask: (taskId: string) => void
  onMarkUnattended: (blockId: string) => void
}

export function SelectedScheduleSection({
  selectedPlan,
  tasks,
  selectedProjectId,
  loading,
  onDeleteTask,
  onMarkUnattended,
}: SelectedScheduleSectionProps) {
  if (selectedPlan) {
    return (
      <div className="flex flex-col gap-2">
        <p className="text-sm font-medium">Generated schedule</p>
        <p className="text-sm text-muted-foreground">
          {selectedPlan.mode} · {selectedPlan.startDate} to {selectedPlan.endDate}
          {planExplanation(selectedPlan)
            ? ` · ${planExplanation(selectedPlan)}`
            : null}
        </p>
        <ScheduleWeekPreview
          blocks={selectedPlan.blocks}
          tasks={tasks}
          startDate={selectedPlan.startDate}
          endDate={selectedPlan.endDate}
          projectId={selectedProjectId}
          onDeleteTask={onDeleteTask}
          onMarkUnattended={onMarkUnattended}
        />
      </div>
    )
  }

  if (!selectedProjectId || loading) return null

  return (
    <Empty className="border border-dashed">
      <EmptyHeader>
        <EmptyTitle>No schedule for this project yet</EmptyTitle>
        <EmptyDescription>
          Add your tasks, then generate a timetable for the selected project.
        </EmptyDescription>
      </EmptyHeader>
      <EmptyContent>
        <Link
          to={`/generate?projectId=${selectedProjectId}`}
          className={cn(buttonVariants())}
        >
          Add tasks & generate schedule
        </Link>
      </EmptyContent>
    </Empty>
  )
}
