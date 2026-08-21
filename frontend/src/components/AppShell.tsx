import type { ReactNode } from 'react'
import { NavLink, Outlet, Navigate } from 'react-router-dom'
import { useAuth } from '@/auth/AuthContext'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { cn } from '@/lib/utils'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated, loading } = useAuth()
  if (loading) {
    return (
      <p className="p-6 text-sm text-muted-foreground" role="status">
        Loading…
      </p>
    )
  }
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return children
}

const primaryLinks = [
  { to: '/dashboard', label: 'Overview' },
  { to: '/projects', label: 'Projects' },
  { to: '/generate', label: 'Generate' },
  { to: '/history', label: 'History' },
  { to: '/notifications', label: 'Notifications' },
]

function NavItem({ to, label }: { to: string; label: string }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cn(
          'rounded-md px-3 py-2 text-sm text-sidebar-foreground/80 transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
          isActive && 'bg-sidebar-accent font-medium text-sidebar-accent-foreground',
        )
      }
    >
      {label}
    </NavLink>
  )
}

export function AppShell() {
  const { user, logout } = useAuth()

  return (
    <div
      className={cn(
        'grid min-h-[100dvh] bg-background text-foreground',
        'grid-rows-[auto_1fr]',
        'lg:grid-cols-[15rem_minmax(0,1fr)] lg:grid-rows-[minmax(0,1fr)]',
      )}
    >
      <aside className="flex h-full flex-col border-b border-sidebar-border bg-sidebar lg:sticky lg:top-0 lg:h-[100dvh] lg:border-r lg:border-b-0">
        <div className="px-4 py-5">
          <p className="text-lg font-semibold tracking-tight text-sidebar-foreground">
            Agamotto
          </p>
          {user ? (
            <div className="mt-1 min-w-0">
              <p className="truncate text-sm font-medium text-sidebar-foreground">
                {user.fullName || user.displayName}
              </p>
              {(user.fullName || user.displayName) !== user.email ? (
                <p className="truncate text-xs text-muted-foreground">{user.email}</p>
              ) : null}
            </div>
          ) : null}
        </div>

        <nav className="flex flex-1 flex-col gap-1 px-2 pb-3" aria-label="Primary">
          {primaryLinks.map((link) => (
            <NavItem key={link.to} {...link} />
          ))}
        </nav>

        <div className="flex flex-col gap-1 px-2 py-3">
          <Separator className="mb-2" />
          <NavItem to="/settings" label="Settings" />
          <Button
            type="button"
            variant="ghost"
            className="h-auto justify-start px-3 py-2 text-sm font-normal text-sidebar-foreground/80"
            onClick={() => void logout()}
          >
            Log out
          </Button>
        </div>
      </aside>

      <main className="min-w-0 overflow-auto bg-background px-5 py-6 sm:px-8 sm:py-8">
        <div className="mx-auto w-full max-w-6xl">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
