import type { TaskStatus } from '@/api'
import { addDays, deadlineDatePart, toDateInputValue } from '@/lib/datetime'
import { COMPLEXITY_MEDIUM } from '@/lib/complexity'

export interface TaskDraft {
  title: string
  category: string
  projectType: string
  priority: number
  deadline: string
  estimatedDurationHours: number
  complexity: number
  status: TaskStatus
}

export function emptyTaskDraft(projectStartDate?: string | null): TaskDraft {
  const projectStart = projectStartDate ? deadlineDatePart(projectStartDate) : null
  return {
    title: '',
    category: 'work',
    projectType: 'General',
    priority: 3,
    deadline: addDays(projectStart ?? toDateInputValue(), 7),
    estimatedDurationHours: 2,
    complexity: COMPLEXITY_MEDIUM,
    status: 'PENDING',
  }
}
