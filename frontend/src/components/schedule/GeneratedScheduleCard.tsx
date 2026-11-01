import { Link } from 'react-router-dom'
import { Calendar, SlidersHorizontal } from 'lucide-react'
import type {
  ScheduleBlockResponse,
  SchedulePlanResponse,
  TaskResponse,
} from '@/api'
import { DecisionBadge } from '@/components/DecisionBadge'
import { ScheduleWeekPreview } from '@/components/ScheduleWeekPreview'
import { StatusBadge } from '@/components/StatusBadge'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  formatBlockDate,
  formatBlockTime,
  formatDurationHours,
  formatShortDate,
} from '@/lib/schedule-format'
import { cn } from '@/lib/utils'

interface GeneratedScheduleCardProps {
  result: SchedulePlanResponse
  tasks: TaskResponse[]
  taskById: Map<string, TaskResponse>
  scheduledBlocks: ScheduleBlockResponse[]
  totalScheduledHours: number
  tasksScheduledCount: number
  sittingByBlockId: Map<string, { n: number; m: number }>
  selectedBlockId: string
  projectId: string
  busy: boolean
  onSelectBlock: (blockId: string) => void
  onMarkUnattended: (blockId: string) => void | Promise<void>
  onDelayBlock: (blockId: string) => void
  onDeleteTask?: (taskId: string) => void
}

export function GeneratedScheduleCard({
  result,
  tasks,
  taskById,
  scheduledBlocks,
  totalScheduledHours,
  tasksScheduledCount,
  sittingByBlockId,
  selectedBlockId,
  projectId,
  busy,
  onSelectBlock,
  onMarkUnattended,
  onDelayBlock,
  onDeleteTask,
}: GeneratedScheduleCardProps) {
  return (
    <Card className="overflow-hidden border-2 border-primary/20 shadow-md">
      <CardHeader className="border-b bg-muted/20 pb-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <CardTitle className="text-xl font-bold tracking-tight">Generated Timetable</CardTitle>
              <Badge
                variant={result.mode === 'SERENITY' ? 'default' : 'secondary'}
                className="font-semibold uppercase tracking-wider text-xs"
              >
                {result.mode}
              </Badge>
              <StatusBadge status={result.status} />
            </div>
            {result.explanationSummary ? (
              <CardDescription className="mt-1.5 text-sm leading-relaxed text-foreground/80">
                {result.explanationSummary}
              </CardDescription>
            ) : null}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <Link
              to={`/projects?projectId=${projectId}`}
              className={cn(buttonVariants({ variant: 'outline', size: 'sm' }))}
            >
              Back to project
            </Link>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div className="rounded-lg border bg-background/80 p-3 shadow-2xs">
            <p className="text-xs font-medium text-muted-foreground">Total Scheduled</p>
            <p className="mt-1 text-lg font-bold text-foreground">
              {totalScheduledHours.toFixed(1)} hrs
            </p>
          </div>
          <div className="rounded-lg border bg-background/80 p-3 shadow-2xs">
            <p className="text-xs font-medium text-muted-foreground">Sittings</p>
            <p className="mt-1 text-lg font-bold text-foreground">
              {scheduledBlocks.length} blocks
            </p>
          </div>
          <div className="rounded-lg border bg-background/80 p-3 shadow-2xs">
            <p className="text-xs font-medium text-muted-foreground">Tasks Included</p>
            <p className="mt-1 text-lg font-bold text-foreground">
              {tasksScheduledCount} / {tasks.length} tasks
            </p>
          </div>
          <div className="rounded-lg border bg-background/80 p-3 shadow-2xs">
            <p className="text-xs font-medium text-muted-foreground">Date Range</p>
            <p className="mt-1 text-sm font-bold text-foreground truncate">
              {formatShortDate(result.startDate)} → {formatShortDate(result.endDate)}
            </p>
          </div>
        </div>
      </CardHeader>

      <CardContent className="p-4 sm:p-6">
        <Tabs defaultValue="timetable" className="w-full">
          <TabsList className="mb-6 w-full justify-start sm:w-fit">
            <TabsTrigger value="timetable" className="gap-2 px-4 py-1.5 text-sm">
              <Calendar className="size-4" />
              Weekly Timetable
            </TabsTrigger>
            <TabsTrigger value="blocks" className="gap-2 px-4 py-1.5 text-sm">
              <SlidersHorizontal className="size-4" />
              All Sittings & Adjustments ({scheduledBlocks.length})
            </TabsTrigger>
          </TabsList>

          <TabsContent value="timetable" className="focus-visible:outline-none">
            <ScheduleWeekPreview
              blocks={result.blocks}
              tasks={tasks}
              startDate={result.startDate}
              endDate={result.endDate}
              projectId={projectId}
              onDeleteTask={onDeleteTask}
              onMarkUnattended={onMarkUnattended}
              onSelectBlock={onSelectBlock}
            />
          </TabsContent>

          <TabsContent value="blocks" className="focus-visible:outline-none">
            <p className="mb-3 text-xs text-muted-foreground">
              Select a sitting to view its description, edit its time range, or mark it completed.
            </p>

            <div className="overflow-x-auto rounded-lg border">
              <Table>
                <TableHeader className="bg-muted/50">
                  <TableRow>
                    <TableHead className="w-12 text-center font-semibold">#</TableHead>
                    <TableHead className="min-w-[180px] font-semibold">Task</TableHead>
                    <TableHead className="min-w-[130px] font-semibold">Day & Date</TableHead>
                    <TableHead className="min-w-[150px] font-semibold">Time Window</TableHead>
                    <TableHead className="min-w-[90px] text-center font-semibold">Duration</TableHead>
                    <TableHead className="min-w-[110px] text-center font-semibold">Decision</TableHead>
                    <TableHead className="min-w-[110px] text-center font-semibold">Status</TableHead>
                    <TableHead className="w-28 text-right font-semibold">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(result.blocks ?? []).length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={8} className="py-12 text-center text-muted-foreground">
                        No blocks were placed for this range.
                      </TableCell>
                    </TableRow>
                  ) : (
                    (result.blocks ?? []).map((block, index) => {
                      const task = taskById.get(block.taskId)
                      const sitting = sittingByBlockId.get(block.id)
                      return (
                        <TableRow
                          key={block.id}
                          className={cn(
                            'cursor-pointer hover:bg-muted/40',
                            block.id === selectedBlockId && 'bg-primary/5',
                          )}
                          onClick={() => onSelectBlock(block.id)}
                        >
                          <TableCell className="text-center text-xs text-muted-foreground">
                            {index + 1}
                          </TableCell>
                          <TableCell>
                            <div className="flex flex-col gap-0.5">
                              <div className="flex items-center gap-1.5">
                                <span
                                  className={cn(
                                    'font-semibold text-sm',
                                    block.completed
                                      ? 'text-muted-foreground line-through'
                                      : 'text-foreground',
                                  )}
                                >
                                  {task?.title ?? block.taskId}
                                </span>
                                {sitting && sitting.m > 1 ? (
                                  <Badge
                                    variant="outline"
                                    className="text-[0.65rem] px-1.5 py-0"
                                  >
                                    Sitting {sitting.n}/{sitting.m}
                                  </Badge>
                                ) : null}
                              </div>
                              {task?.category ? (
                                <span className="text-xs text-muted-foreground">
                                  {task.category}
                                </span>
                              ) : null}
                            </div>
                          </TableCell>
                          <TableCell className="text-sm font-medium">
                            {formatBlockDate(block.startTime)}
                          </TableCell>
                          <TableCell>
                            {block.startTime && block.endTime ? (
                              <div className="flex items-center gap-1.5 text-sm font-medium font-mono text-foreground/90">
                                <span>{formatBlockTime(block.startTime)}</span>
                                <span className="text-muted-foreground">→</span>
                                <span>{formatBlockTime(block.endTime)}</span>
                              </div>
                            ) : (
                              <span className="text-xs text-muted-foreground">—</span>
                            )}
                          </TableCell>
                          <TableCell className="text-center">
                            <Badge variant="secondary" className="font-normal text-xs">
                              {formatDurationHours(block.startTime, block.endTime)}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-center">
                            <DecisionBadge decision={block.decision} />
                          </TableCell>
                          <TableCell className="text-center">
                            {block.completed ? (
                              <Badge
                                variant="secondary"
                                className="text-[0.65rem] px-1.5 py-0 font-medium text-emerald-700 dark:text-emerald-400"
                              >
                                Completed
                              </Badge>
                            ) : (
                              <span className="text-xs text-muted-foreground">Open</span>
                            )}
                          </TableCell>
                          <TableCell className="text-right" onClick={(event) => event.stopPropagation()}>
                            {block.decision === 'SCHEDULED' ? (
                              <div className="flex items-center justify-end gap-1">
                                <Button
                                  type="button"
                                  size="sm"
                                  variant="outline"
                                  className="h-7 px-2 text-xs"
                                  disabled={busy}
                                  onClick={() => onSelectBlock(block.id)}
                                >
                                  Details
                                </Button>
                                <Button
                                  type="button"
                                  size="sm"
                                  variant="outline"
                                  className="h-7 px-2 text-xs text-amber-600 dark:text-amber-400 hover:text-amber-700 hover:bg-amber-500/10 border-amber-500/30"
                                  disabled={busy}
                                  title="Mark this session as unattended and rearrange the schedule"
                                  onClick={() => onMarkUnattended(block.id)}
                                >
                                  Unattended
                                </Button>
                                <Button
                                  type="button"
                                  size="sm"
                                  variant="ghost"
                                  className="h-7 px-2 text-xs text-muted-foreground hover:text-destructive"
                                  disabled={busy}
                                  onClick={() => onDelayBlock(block.id)}
                                >
                                  Delay
                                </Button>
                              </div>
                            ) : block.reason?.toLowerCase().startsWith('unattended session') ? (
                              <span className="text-xs font-semibold text-amber-600 dark:text-amber-400">
                                Missed
                              </span>
                            ) : (
                              <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                className="h-7 px-2 text-xs"
                                onClick={() => onSelectBlock(block.id)}
                              >
                                Details
                              </Button>
                            )}
                          </TableCell>
                        </TableRow>
                      )
                    })
                  )}
                </TableBody>
              </Table>
            </div>
          </TabsContent>
        </Tabs>
      </CardContent>
      <CardFooter className="justify-between border-t bg-muted/10">
        <span className="text-xs text-muted-foreground">
          Changes to sitting blocks take effect immediately on your project schedule.
        </span>
        <Link
          to={`/projects?projectId=${projectId}`}
          className={cn(buttonVariants({ variant: 'link' }), 'px-0 text-xs')}
        >
          Go to project page →
        </Link>
      </CardFooter>
    </Card>
  )
}
