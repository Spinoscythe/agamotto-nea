import {
  Coffee,
  HeartPulse,
  Palmtree,
  Sparkles,
  User as UserIcon,
} from 'lucide-react'
import type { LeaveType } from '@/api'

export const LEAVE_TYPES: {
  value: LeaveType
  label: string
  icon: typeof Palmtree
  color: string
}[] = [
  { value: 'VACATION', label: 'Vacation / Trip', icon: Palmtree, color: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' },
  { value: 'SHORT_BREAK', label: 'Short Break / Rest', icon: Coffee, color: 'border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400' },
  { value: 'SICK_LEAVE', label: 'Sick Leave / Recovery', icon: HeartPulse, color: 'border-rose-500/30 bg-rose-500/10 text-rose-600 dark:text-rose-400' },
  { value: 'PERSONAL', label: 'Personal Appointment', icon: UserIcon, color: 'border-sky-500/30 bg-sky-500/10 text-sky-600 dark:text-sky-400' },
  { value: 'HOLIDAY', label: 'Public / Academic Holiday', icon: Sparkles, color: 'border-purple-500/30 bg-purple-500/10 text-purple-600 dark:text-purple-400' },
]

export function formatLeaveRange(startIso: string, endIso: string): {
  formatted: string
  durationHours: number
} {
  const start = new Date(startIso)
  const end = new Date(endIso)
  const durationHours = Math.max(0, (end.getTime() - start.getTime()) / (1000 * 60 * 60))
  const startFormatted = start.toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
  const endFormatted = end.toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
  return {
    formatted: `${startFormatted} – ${endFormatted}`,
    durationHours: Math.round(durationHours * 10) / 10,
  }
}

export function getDefaultDateTimeInput(offsetHours = 24): string {
  const d = new Date()
  d.setHours(d.getHours() + offsetHours, 0, 0, 0)
  return d.toISOString().slice(0, 16)
}
