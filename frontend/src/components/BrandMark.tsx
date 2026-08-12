import { cn } from '@/lib/utils'

/** Plain square mark — nothing fancy. */
export function BrandMark({ className }: { className?: string }) {
  return (
    <span
      aria-hidden
      className={cn(
        'inline-block size-5 border border-foreground bg-foreground',
        className,
      )}
    />
  )
}
