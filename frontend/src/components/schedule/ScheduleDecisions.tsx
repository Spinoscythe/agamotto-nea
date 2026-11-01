import { XCircleIcon } from '@phosphor-icons/react'
import type { ScheduleBlockResponse, TaskResponse } from '@/api'
import { DecisionBadge } from '@/components/DecisionBadge'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { complexityLabel } from '@/lib/complexity'
import { formatSittingDateTime } from '@/lib/schedule-format'
import { parseReason, type TaskDecision } from './preview-utils'

interface ScheduleDecisionsProps {
  explainedBlocks: ScheduleBlockResponse[]
  taskDecisions: TaskDecision[]
  decisionTab: 'byTask' | 'bySitting'
  onDecisionTabChange: (tab: 'byTask' | 'bySitting') => void
  taskById: Map<string, TaskResponse>
  onMarkUnattended?: (blockId: string) => void | Promise<void>
}

export function ScheduleDecisions({
  explainedBlocks,
  taskDecisions,
  decisionTab,
  onDecisionTabChange,
  taskById,
  onMarkUnattended,
}: ScheduleDecisionsProps) {
  return (
    <Card size="sm">
      <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <CardTitle>Schedule decisions</CardTitle>
          <p className="text-xs text-muted-foreground mt-0.5">
            Rationales, priority scores, and pacing policies for scheduled tasks
          </p>
        </div>
        <div className="flex items-center gap-1">
          <Button
            type="button"
            variant={decisionTab === 'byTask' ? 'secondary' : 'ghost'}
            size="sm"
            className="h-7 px-2.5 text-xs font-medium"
            onClick={() => onDecisionTabChange('byTask')}
          >
            Grouped by task ({taskDecisions.length})
          </Button>
          <Button
            type="button"
            variant={decisionTab === 'bySitting' ? 'secondary' : 'ghost'}
            size="sm"
            className="h-7 px-2.5 text-xs font-medium"
            onClick={() => onDecisionTabChange('bySitting')}
          >
            All sittings ({explainedBlocks.length})
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {decisionTab === 'byTask' ? (
          <div className="flex flex-col gap-3">
            {taskDecisions.map((item) => (
              <div
                key={item.taskId}
                className="rounded-lg border bg-card p-3.5 transition-colors hover:border-primary/30"
              >
                <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                  <div className="flex flex-col gap-1.5">
                    <div className="flex flex-wrap items-center gap-2">
                      <DecisionBadge decision={item.overallDecision} />
                      <span className="text-sm font-bold text-foreground">
                        {item.task?.title ?? item.taskId}
                      </span>
                      {item.task?.category ? (
                        <Badge variant="outline" className="text-xs px-1.5 py-0">
                          {item.task.category}
                        </Badge>
                      ) : null}
                    </div>

                    <div className="flex flex-wrap items-center gap-1.5 text-xs">
                      {item.task?.priority ? (
                        <Badge variant="secondary" className="font-normal text-[0.7rem] px-1.5 py-0">
                          Priority {item.task.priority}
                        </Badge>
                      ) : null}
                      {item.task?.complexity ? (
                        <Badge variant="secondary" className="font-normal text-[0.7rem] px-1.5 py-0">
                          {complexityLabel(item.task.complexity)}
                        </Badge>
                      ) : null}
                      {item.sittingsCount > 0 ? (
                        <Badge variant="secondary" className="font-normal text-[0.7rem] px-1.5 py-0">
                          {item.sittingsCount} {item.sittingsCount === 1 ? 'sitting' : 'sittings'} ({item.totalHours.toFixed(1)}h total)
                        </Badge>
                      ) : null}
                      {item.parsed.deadlineStr ? (
                        <span className="text-muted-foreground text-[0.75rem]">
                          Due {item.parsed.deadlineStr}
                        </span>
                      ) : null}
                    </div>
                  </div>

                  {item.parsed.score ? (
                    <div className="flex items-center gap-1.5 rounded-md border bg-muted/40 px-2.5 py-1 text-xs shrink-0">
                      <span className="text-muted-foreground font-medium">Priority score:</span>
                      <span className="font-bold text-foreground">{item.parsed.score}</span>
                    </div>
                  ) : null}
                </div>

                {/* Policy / Explanation Notes */}
                <div className="mt-3 rounded-md bg-muted/30 p-2.5 text-xs text-muted-foreground leading-relaxed flex flex-col gap-1">
                  {item.parsed.sessionCap ? (
                    <div className="flex items-center gap-2 text-foreground/90 font-medium">
                      <span className="size-1.5 rounded-full bg-primary shrink-0" />
                      <span>Pacing policy: {item.parsed.sessionCap}</span>
                    </div>
                  ) : null}
                  {item.parsed.decomposedNote ? (
                    <div className="flex items-center gap-2 text-foreground/90">
                      <span className="size-1.5 rounded-full bg-primary shrink-0" />
                      <span>{item.parsed.decomposedNote}</span>
                    </div>
                  ) : null}
                  {item.delayedBlocks.length > 0 ? (
                    item.delayedBlocks[0].reason?.toLowerCase().includes("you can't do anything anymore") ? (
                      <div className="mt-1 rounded-md border border-destructive/40 bg-destructive/15 p-2.5 text-xs text-destructive flex items-start gap-2">
                        <XCircleIcon size={16} className="shrink-0 mt-0.5" />
                        <div className="flex flex-col gap-0.5">
                          <span className="font-bold">Deadline Approached — Cannot Complete Task:</span>
                          <span className="text-destructive/90">{item.delayedBlocks[0].reason}</span>
                        </div>
                      </div>
                    ) : (
                      <div className="text-destructive font-medium flex items-center gap-2">
                        <span className="size-1.5 rounded-full bg-destructive shrink-0" />
                        <span>{item.delayedBlocks[0].reason}</span>
                      </div>
                    )
                  ) : null}
                </div>

                {/* Sittings breakdown chips */}
                {item.sittings.length > 0 ? (
                  <div className="mt-3 border-t border-border/50 pt-2.5">
                    <p className="mb-2 text-xs font-medium text-muted-foreground">
                      Scheduled sittings:
                    </p>
                    <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                      {item.sittings.map((sitting, idx) => (
                        <div
                          key={sitting.id}
                          className="flex flex-col gap-0.5 rounded-md border bg-background/80 p-2 text-xs"
                        >
                          <span className="font-semibold text-foreground">
                            Sitting {idx + 1} of {item.sittings.length}
                          </span>
                          <span className="text-muted-foreground">
                            {formatSittingDateTime(sitting.startTime, sitting.endTime)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        ) : (
          <ul className="flex flex-col gap-3">
            {explainedBlocks.map((b) => {
              const task = taskById.get(b.taskId)
              const parsed = parseReason(b.reason)
              return (
                <li
                  key={b.id}
                  className="flex flex-col gap-1.5 border-b border-border/60 pb-3 last:border-b-0 last:pb-0"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <DecisionBadge decision={b.decision} />
                      <span className="text-sm font-semibold text-foreground">
                        {task?.title ?? b.taskId}
                      </span>
                      {b.startTime && b.endTime ? (
                        <span className="text-xs text-muted-foreground font-medium">
                          {formatSittingDateTime(b.startTime, b.endTime)}
                        </span>
                      ) : null}
                      {b.reason?.toLowerCase().startsWith('unattended session') ? (
                        <Badge variant="outline" className="text-xs px-1.5 py-0 border-amber-500/40 text-amber-600 dark:text-amber-400 font-semibold">
                          Missed Session
                        </Badge>
                      ) : null}
                    </div>
                    <div className="flex items-center gap-2">
                      {onMarkUnattended && b.decision === 'SCHEDULED' && !b.reason?.toLowerCase().startsWith('unattended session') ? (
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          className="h-6 px-2 text-[0.68rem] text-amber-600 dark:text-amber-400 hover:text-amber-700 hover:bg-amber-500/10 border-amber-500/30"
                          onClick={() => void onMarkUnattended(b.id)}
                          title="Mark session as unattended and rearrange remaining schedule"
                        >
                          Mark Unattended
                        </Button>
                      ) : null}
                      {parsed.score ? (
                        <Badge variant="outline" className="text-xs">
                          Score {parsed.score}
                        </Badge>
                      ) : null}
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2 text-xs">
                    {parsed.sessionCap ? (
                      <Badge variant="secondary" className="font-normal text-[0.7rem]">
                        {parsed.sessionCap}
                      </Badge>
                    ) : null}
                    {parsed.decomposedNote ? (
                      <span className="text-muted-foreground">
                        {parsed.decomposedNote}
                      </span>
                    ) : null}
                    {parsed.deadlineStr ? (
                      <span className="text-muted-foreground">
                        Due {parsed.deadlineStr}
                      </span>
                    ) : null}
                    {!parsed.sessionCap && !parsed.decomposedNote ? (
                      <span className="text-muted-foreground">
                        {b.reason?.trim() || 'No explanation provided.'}
                      </span>
                    ) : null}
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
