import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

const STATUS_CLASS: Record<string, string> = {
  PENDING: 'border-amber-200 bg-amber-100 text-amber-900',
  IN_PROGRESS: 'border-sky-200 bg-sky-100 text-sky-900',
  COMPLETED: 'border-emerald-200 bg-emerald-100 text-emerald-900',
  CANCELLED: 'border-rose-200 bg-rose-100 text-rose-900',
  DRAFT: 'border-slate-200 bg-slate-100 text-slate-700',
  ACTIVE: 'border-emerald-200 bg-emerald-100 text-emerald-900',
  ARCHIVED: 'border-zinc-200 bg-zinc-100 text-zinc-700',
  ACCEPTED: 'border-emerald-200 bg-emerald-100 text-emerald-900',
  DECLINED: 'border-rose-200 bg-rose-100 text-rose-900',
  SCHEDULED: 'border-emerald-200 bg-emerald-100 text-emerald-900',
  DELAYED: 'border-orange-200 bg-orange-100 text-orange-900',
  EXCLUDED: 'border-zinc-200 bg-zinc-100 text-zinc-700',
}

function labelFor(status: string) {
  return status
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export function StatusBadge({
  status,
  className,
}: {
  status: string
  className?: string
}) {
  return (
    <Badge
      variant="outline"
      className={cn(
        STATUS_CLASS[status] ?? 'border-border bg-secondary text-secondary-foreground',
        className,
      )}
    >
      {labelFor(status)}
    </Badge>
  )
}
