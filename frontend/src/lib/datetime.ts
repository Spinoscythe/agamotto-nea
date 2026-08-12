/** Local calendar helpers for `<input type="date">` / `datetime-local` (never UTC ISO). */

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

export function toDateInputValue(date: Date = new Date()): string {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

export function toDateTimeLocalValue(date: Date = new Date()): string {
  return `${toDateInputValue(date)}T${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

export function addDays(isoDate: string, days: number): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  return toDateInputValue(new Date(year, month - 1, day + days))
}

export function eachDateInclusive(startDate: string, endDate: string, maxDays = 7): string[] {
  const list: string[] = []
  let current = startDate
  let guard = 0
  while (current <= endDate && guard < maxDays) {
    list.push(current)
    current = addDays(current, 1)
    guard += 1
  }
  return list.length > 0 ? list : [startDate]
}

/** Pad `datetime-local` to `yyyy-MM-ddTHH:mm:ss` for Jackson LocalDateTime. */
export function toLocalDateTimePayload(value: string): string {
  const trimmed = value.trim()
  if (trimmed.length === 16) return `${trimmed}:00`
  if (trimmed.length === 19) return trimmed
  if (trimmed.length > 19) return trimmed.slice(0, 19)
  return trimmed
}

export function deadlineDatePart(deadline: string | null | undefined): string | null {
  if (deadline == null || deadline === '') return null
  if (typeof deadline !== 'string') return null
  return deadline.length >= 10 ? deadline.slice(0, 10) : deadline
}

export function toTimeInputValue(value: unknown, fallback: string): string {
  if (typeof value === 'string' && value.length >= 5) return value.slice(0, 5)
  return fallback
}

export function timeToMinutes(value: string): number {
  const [hours, minutes] = value.split(':').map(Number)
  if (!Number.isFinite(hours)) return Number.NaN
  return hours * 60 + (Number.isFinite(minutes) ? minutes : 0)
}

/** Hour field from a local `yyyy-MM-ddTHH:mm[:ss]` string (not UTC-converted). */
export function localHourOf(iso: string | null | undefined): number | null {
  if (!iso || typeof iso !== 'string' || iso.length < 13) return null
  const hour = Number(iso.slice(11, 13))
  return Number.isFinite(hour) && hour >= 0 && hour <= 23 ? hour : null
}

export function localMinuteOf(iso: string | null | undefined): number | null {
  if (!iso || typeof iso !== 'string' || iso.length < 16) return null
  const minute = Number(iso.slice(14, 16))
  return Number.isFinite(minute) && minute >= 0 && minute <= 59 ? minute : null
}

/** Inclusive start hour and exclusive end hour a block occupies. */
export function occupiedHourRange(
  startIso: string | null | undefined,
  endIso: string | null | undefined,
): { startHour: number; endHourExclusive: number } | null {
  const startHour = localHourOf(startIso)
  if (startHour == null) return null
  const endHour = localHourOf(endIso)
  const endMinute = localMinuteOf(endIso)
  let endHourExclusive: number
  if (endHour == null) {
    endHourExclusive = startHour + 1
  } else if ((endMinute ?? 0) > 0) {
    endHourExclusive = endHour + 1
  } else {
    endHourExclusive = endHour
  }
  if (endHourExclusive <= startHour) endHourExclusive = startHour + 1
  return { startHour, endHourExclusive: Math.min(24, endHourExclusive) }
}
