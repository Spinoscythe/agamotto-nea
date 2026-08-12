import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ApiError,
  projectsApi,
  schedulesApi,
  type ProjectResponse,
  type SchedulePlanResponse,
} from '@/api'
import { useAuth } from '@/auth/AuthContext'
import { PageHeader } from '@/components/PageHeader'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { buttonVariants } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { cn } from '@/lib/utils'
import { planExplanation } from '@/lib/schedule'

interface HistoryRow {
  plan: SchedulePlanResponse
  projectName: string
}

export function HistoryPage() {
  const { user } = useAuth()
  const [rows, setRows] = useState<HistoryRow[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    let cancelled = false
    setLoading(true)
    ;(async () => {
      try {
        const projects: ProjectResponse[] = await projectsApi.list(user.id)
        const nested = await Promise.all(
          projects.map(async (project) => {
            const plans = await schedulesApi.listByProject(project.id)
            return plans.map((plan) => ({ plan, projectName: project.name }))
          }),
        )
        if (cancelled) return
        const flat = nested.flat().sort((a, b) =>
          (b.plan.generatedAt ?? '').localeCompare(a.plan.generatedAt ?? ''),
        )
        setRows(flat)
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : 'Failed to load history')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [user])

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="History"
        description="Previously generated schedule plans across your projects."
      />

      {error ? (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      {loading ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : rows.length === 0 ? (
        <Empty className="border border-dashed">
          <EmptyHeader>
            <EmptyTitle>No generated schedules yet</EmptyTitle>
            <EmptyDescription>
              Create a project, add tasks, and generate a schedule to see it here.
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Link to="/projects" className={cn(buttonVariants())}>
              Go to projects
            </Link>
          </EmptyContent>
        </Empty>
      ) : (
        <Card size="sm" className="py-0">
          <CardContent className="p-0">
            <ul className="flex flex-col divide-y">
              {rows.map(({ plan, projectName }) => {
                const explanation = planExplanation(plan)
                return (
                  <li
                    key={plan.id}
                    className="flex flex-wrap items-center justify-between gap-3 px-4 py-3"
                  >
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="text-sm font-medium">{projectName}</p>
                        <Badge variant="secondary">{plan.mode}</Badge>
                        <Badge variant="outline">{plan.status}</Badge>
                      </div>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {plan.startDate} to {plan.endDate} · {plan.blocks?.length ?? 0}{' '}
                        blocks ·{' '}
                        {plan.generatedAt
                          ? new Date(plan.generatedAt).toLocaleString()
                          : '—'}
                      </p>
                      {explanation ? (
                        <p className="mt-1 text-xs text-muted-foreground">{explanation}</p>
                      ) : null}
                    </div>
                    <Link
                      to={`/projects?projectId=${plan.projectId}`}
                      className={cn(buttonVariants({ variant: 'outline', size: 'sm' }))}
                    >
                      Open
                    </Link>
                  </li>
                )
              })}
            </ul>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
