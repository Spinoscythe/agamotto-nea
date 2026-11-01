import type { SubmitEvent } from 'react'
import { Link } from 'react-router-dom'
import { TrashIcon } from '@phosphor-icons/react'
import type { ProjectResponse, TaskResponse, TaskStatus } from '@/api'
import { StatusBadge } from '@/components/StatusBadge'
import { Button, buttonVariants } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  COMPLEXITY_EASY,
  COMPLEXITY_HARD,
  COMPLEXITY_MEDIUM,
  complexityLabel,
} from '@/lib/complexity'
import { deadlineDatePart } from '@/lib/datetime'
import { cn } from '@/lib/utils'
import type { TaskDraft } from './task-draft'

interface TaskCreationCardProps {
  tasks: TaskResponse[]
  draft: TaskDraft
  onDraftChange: (draft: TaskDraft) => void
  busy: boolean
  projectId: string
  selectedProject: ProjectResponse | null
  schedulableCount: number
  onAddTask: (event: SubmitEvent) => void
  onRemoveTask: (taskId: string) => void
  onGenerate: (event: SubmitEvent) => void
}

export function TaskCreationCard({
  tasks,
  draft,
  onDraftChange,
  busy,
  projectId,
  selectedProject,
  schedulableCount,
  onAddTask,
  onRemoveTask,
  onGenerate,
}: TaskCreationCardProps) {
  const updateDraft = (patch: Partial<TaskDraft>) => onDraftChange({ ...draft, ...patch })

  return (
    <Card>
      <CardHeader>
        <CardTitle>Tasks Creation</CardTitle>
        <CardDescription>
          Tasks are registered when added, then used by Serenity or Crunch generation.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <form onSubmit={onAddTask}>
          <div className="overflow-x-auto rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="min-w-[150px] text-center">Task Name</TableHead>
                  <TableHead className="min-w-[120px] text-center">Category</TableHead>
                  <TableHead className="min-w-[130px] text-center">Description</TableHead>
                  <TableHead className="min-w-[120px] text-center">Complexity</TableHead>
                  <TableHead className="min-w-[130px] text-center">Priority</TableHead>
                  <TableHead className="min-w-[130px] text-center">Estimate hours</TableHead>
                  <TableHead className="min-w-[150px] text-center">Deadline</TableHead>
                  <TableHead className="min-w-[150px] text-center">Status</TableHead>
                  <TableHead className="w-28 text-center">Action</TableHead>
                </TableRow>
              </TableHeader>

              <TableBody>
                {tasks.map((task) => (
                  <TableRow key={task.id}>
                    <TableCell className="font-medium">{task.title}</TableCell>
                    <TableCell>{task.category}</TableCell>
                    <TableCell className="max-w-[10rem] truncate">
                      {task.description ?? '—'}
                    </TableCell>
                    <TableCell className="text-center">{complexityLabel(task.complexity)}</TableCell>
                    <TableCell className="text-center">{task.priority}</TableCell>
                    <TableCell className="text-center">{task.estimatedDurationHours}</TableCell>
                    <TableCell className="text-center">
                      {deadlineDatePart(task.deadline) ?? '—'}
                    </TableCell>
                    <TableCell className="text-center">
                      <StatusBadge status={task.status} />
                    </TableCell>
                    <TableCell className="text-center">
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-sm"
                        aria-label={`Remove ${task.title}`}
                        disabled={busy}
                        onClick={() => onRemoveTask(task.id)}
                      >
                        <TrashIcon />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
                {tasks.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={9} className="py-8 text-center text-muted-foreground">
                      No tasks yet — enter details below and click Add task.
                    </TableCell>
                  </TableRow>
                ) : null}
              </TableBody>

              <TableFooter className="bg-muted/40 font-normal">
                <TableRow>
                  <TableCell>
                    <Input
                      aria-label="Task name"
                      placeholder="Task name"
                      value={draft.title}
                      onChange={(event) => updateDraft({ title: event.target.value })}
                      required
                    />
                  </TableCell>
                  <TableCell>
                    <Input
                      aria-label="Category"
                      placeholder="Category"
                      value={draft.category}
                      onChange={(event) => updateDraft({ category: event.target.value })}
                      required
                    />
                  </TableCell>
                  <TableCell>
                    <Input
                      aria-label="Project type"
                      placeholder="Project type"
                      value={draft.projectType}
                      onChange={(event) => updateDraft({ projectType: event.target.value })}
                    />
                  </TableCell>
                  <TableCell>
                    <Select
                      value={String(draft.complexity)}
                      onValueChange={(value) => {
                        if (value != null) updateDraft({ complexity: Number(value) })
                      }}
                    >
                      <SelectTrigger aria-label="Complexity" className="w-full">
                        <SelectValue>{complexityLabel(draft.complexity)}</SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        <SelectGroup>
                          <SelectItem value={String(COMPLEXITY_EASY)}>Easy</SelectItem>
                          <SelectItem value={String(COMPLEXITY_MEDIUM)}>Medium</SelectItem>
                          <SelectItem value={String(COMPLEXITY_HARD)}>Hard</SelectItem>
                        </SelectGroup>
                      </SelectContent>
                    </Select>
                  </TableCell>
                  <TableCell>
                    <Select
                      value={String(draft.priority)}
                      onValueChange={(value) => {
                        if (value != null) updateDraft({ priority: Number(value) })
                      }}
                    >
                      <SelectTrigger aria-label="Priority" className="w-full">
                        <SelectValue>
                          {draft.priority === 1
                            ? '1 (Highest)'
                            : draft.priority === 5
                              ? '5 (Lowest)'
                              : String(draft.priority)}
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        <SelectGroup>
                          <SelectItem value="1">1 (Highest)</SelectItem>
                          <SelectItem value="2">2 (High)</SelectItem>
                          <SelectItem value="3">3 (Medium)</SelectItem>
                          <SelectItem value="4">4 (Low)</SelectItem>
                          <SelectItem value="5">5 (Lowest)</SelectItem>
                        </SelectGroup>
                      </SelectContent>
                    </Select>
                  </TableCell>
                  <TableCell>
                    <Input
                      aria-label="Estimate hours"
                      type="number"
                      min={0.25}
                      step={0.25}
                      placeholder="Hours"
                      value={
                        Number.isFinite(draft.estimatedDurationHours)
                          ? draft.estimatedDurationHours
                          : ''
                      }
                      onChange={(event) =>
                        updateDraft({
                          estimatedDurationHours:
                            event.target.value === '' ? Number.NaN : Number(event.target.value),
                        })
                      }
                      required
                    />
                  </TableCell>
                  <TableCell>
                    <Input
                      aria-label="Deadline"
                      type="date"
                      min={deadlineDatePart(selectedProject?.startDate) ?? undefined}
                      max={deadlineDatePart(selectedProject?.endDate) ?? undefined}
                      value={draft.deadline}
                      onChange={(event) => updateDraft({ deadline: event.target.value })}
                      required
                    />
                  </TableCell>
                  <TableCell>
                    <Select
                      value={draft.status}
                      onValueChange={(value) => {
                        if (value != null) updateDraft({ status: value as TaskStatus })
                      }}
                    >
                      <SelectTrigger aria-label="Status" className="w-full">
                        <SelectValue>{draft.status}</SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        <SelectGroup>
                          <SelectItem value="PENDING">PENDING</SelectItem>
                          <SelectItem value="IN_PROGRESS">IN_PROGRESS</SelectItem>
                          <SelectItem value="COMPLETED">COMPLETED</SelectItem>
                          <SelectItem value="CANCELLED">CANCELLED</SelectItem>
                        </SelectGroup>
                      </SelectContent>
                    </Select>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      type="submit"
                      size="sm"
                      disabled={busy || !projectId}
                      className="whitespace-nowrap"
                    >
                      Add task
                    </Button>
                  </TableCell>
                </TableRow>
              </TableFooter>
            </Table>
          </div>
        </form>
      </CardContent>
      <CardFooter className="justify-between border-t">
        <Link to="/settings" className={cn(buttonVariants({ variant: 'outline' }))}>
          Edit User preferences
        </Link>
        <form onSubmit={onGenerate}>
          <Button type="submit" disabled={busy || !projectId || schedulableCount === 0}>
            {busy ? 'Generating…' : 'Generate schedule'}
          </Button>
        </form>
      </CardFooter>
    </Card>
  )
}
