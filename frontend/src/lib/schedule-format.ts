/** Shared date/time formatting for schedule blocks and sittings. */

export function formatBlockDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso.slice(0, 10)
  return date.toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
}

export function formatBlockTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso.slice(11, 16)
  return date.toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

export function formatDurationHours(
  startIso: string | null | undefined,
  endIso: string | null | undefined,
): string {
  if (!startIso || !endIso) return '—'
  const start = new Date(startIso).getTime()
  const end = new Date(endIso).getTime()
  if (Number.isNaN(start) || Number.isNaN(end) || end <= start) return '—'
  const hours = (end - start) / (1000 * 60 * 60)
  return hours === 1 ? '1.0h' : `${hours.toFixed(1)}h`
}

export function formatShortDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
  const date = new Date(`${dateStr}T12:00:00`)
  if (Number.isNaN(date.getTime())) return dateStr
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

export function formatSittingDateTime(
  startIso: string | null | undefined,
  endIso: string | null | undefined,
): string {
  if (!startIso || !endIso) return '—'
  const start = new Date(startIso)
  const end = new Date(endIso)
  const dateStr = start.toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
  const startStr = start.toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
  const endStr = end.toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
  const hours = (end.getTime() - start.getTime()) / (1000 * 60 * 60)
  const durStr = hours === 1 ? '1h' : `${hours.toFixed(1)}h`
  return `${dateStr} · ${startStr} – ${endStr} (${durStr})`
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}
