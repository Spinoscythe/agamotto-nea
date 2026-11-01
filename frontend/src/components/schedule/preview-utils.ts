import type { LeaveType, ScheduleBlockResponse, TaskResponse } from '@/api'

export const DEFAULT_HOURS = [9, 10, 11, 12, 13, 14, 15, 16]

export function dayKey(iso: string | null): string | null {
  if (!iso || typeof iso !== 'string') return null
  return iso.slice(0, 10)
}

export function formatHour(hour: number): string {
  if (hour === 0) return '12 AM'
  if (hour === 12) return '12 PM'
  if (hour > 12) return `${hour - 12} PM`
  return `${hour} AM`
}

export function getLeaveEmoji(type: LeaveType): string {
  switch (type) {
    case 'VACATION':
      return '🌴'
    case 'SHORT_BREAK':
      return '☕'
    case 'SICK_LEAVE':
      return '🤒'
    case 'HOLIDAY':
      return '🎉'
    case 'PERSONAL':
      return '👤'
    default:
      return '🛑'
  }
}

export interface ParsedReason {
  raw: string
  score: string | null
  priority: number | null
  deadlineStr: string | null
  durationHours: string | null
  sessionCap: string | null
  decomposedNote: string | null
  isDelayed: boolean
  delayedMessage: string | null
}

export function parseReason(reason: string | null | undefined): ParsedReason {
  if (!reason) {
    return {
      raw: '',
      score: null,
      priority: null,
      deadlineStr: null,
      durationHours: null,
      sessionCap: null,
      decomposedNote: null,
      isDelayed: false,
      delayedMessage: null,
    }
  }

  const isDelayed = reason.toLowerCase().startsWith('delayed')
  const scoreMatch = reason.match(/score=([0-9.]+)/)
  const priorityMatch = reason.match(/priority=([0-9]+)/)
  const deadlineMatch = reason.match(/deadline=([0-9-T:]+)/)
  const durationMatch = reason.match(/duration=([0-9.]+h?)/)
  const sessionCapMatch = reason.match(/Session split:\s*([^.]+)/) || reason.match(/capped at\s*([0-9.]+h?)/)
  const decomposedMatch = reason.match(/Decomposed across days[^.]*/) || reason.match(/Split across days[^.]*/)

  let deadlineFormatted: string | null = null
  if (deadlineMatch) {
    const d = new Date(deadlineMatch[1])
    if (!Number.isNaN(d.getTime())) {
      deadlineFormatted = d.toLocaleDateString(undefined, {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
      })
    } else {
      deadlineFormatted = deadlineMatch[1].slice(0, 10)
    }
  }

  return {
    raw: reason,
    score: scoreMatch ? scoreMatch[1] : null,
    priority: priorityMatch ? Number(priorityMatch[1]) : null,
    deadlineStr: deadlineFormatted,
    durationHours: durationMatch ? durationMatch[1] : null,
    sessionCap: sessionCapMatch ? sessionCapMatch[1].trim() : null,
    decomposedNote: decomposedMatch ? decomposedMatch[0].trim() : null,
    isDelayed,
    delayedMessage: isDelayed ? reason : null,
  }
}

export const palette = [
  'bg-chart-1/80 text-white',
  'bg-chart-2/80 text-white',
  'bg-chart-3/80 text-white',
  'bg-chart-4/80 text-foreground',
]

export type StatusItem = { id: string; title: string }

export interface StatusGroup {
  label: string
  items: StatusItem[]
}

export interface TaskDecision {
  taskId: string
  task: TaskResponse | undefined
  overallDecision: 'SCHEDULED' | 'DELAYED' | 'EXCLUDED'
  totalHours: number
  sittingsCount: number
  sittings: ScheduleBlockResponse[]
  delayedBlocks: ScheduleBlockResponse[]
  parsed: ParsedReason
}
