import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

export function AuthLayout({
  title,
  children,
  footer,
}: {
  title: string
  children: ReactNode
  footer?: ReactNode
}) {
  return (
    <div
      className={cn(
        'dark grid min-h-[100dvh] bg-background text-foreground',
        'grid-rows-[auto_1fr]',
        'lg:grid-cols-[minmax(14rem,1fr)_minmax(0,2fr)] lg:grid-rows-none',
      )}
    >
      <aside
        aria-label="Agamotto"
        className={cn(
          'relative flex flex-col items-center justify-center overflow-hidden border-b border-sidebar-border bg-sidebar',
          'px-6 py-10',
          'lg:border-r lg:border-b-0 lg:px-10 lg:py-10',
        )}
      >
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_30%_20%,oklch(1_0_0_/_0.06),transparent_55%)]"
        />
        <p className="auth-brand-mark relative text-center text-4xl font-semibold tracking-tight text-sidebar-foreground sm:text-5xl lg:text-6xl">
          Agamotto
        </p>
      </aside>

      <main className="flex items-center justify-center bg-background px-6 py-12 sm:px-10 lg:px-16">
        <div className="auth-form-panel w-full max-w-md">
          <h1 className="mb-8 text-3xl font-semibold tracking-tight sm:text-4xl">
            {title}
          </h1>

          <div>{children}</div>

          {footer ? (
            <div className="mt-6 text-sm text-muted-foreground [&_a]:font-medium [&_a]:text-foreground [&_a]:underline [&_a]:underline-offset-4">
              {footer}
            </div>
          ) : null}
        </div>
      </main>
    </div>
  )
}
