import { Link } from 'react-router-dom'
import { ArrowRight, FolderKanban, Plus, Sparkles } from 'lucide-react'
import type { ProjectResponse } from '@/api'
import { Button, buttonVariants } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

interface ProjectsOverviewCardProps {
  loading: boolean
  projects: ProjectResponse[]
}

export function ProjectsOverviewCard({ loading, projects }: ProjectsOverviewCardProps) {
  return (
    <Card className="p-5">
      <CardHeader className="p-0">
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-base font-semibold">Your Projects</CardTitle>
            <CardDescription className="text-xs">
              Quickly check progress or generate fresh timetables.
            </CardDescription>
          </div>
          <Button variant="link" size="sm" render={<Link to="/projects" />} nativeButton={false}>
            All projects <ArrowRight className="size-3" />
          </Button>
        </div>
      </CardHeader>
      <CardContent className="mt-4 p-0">
        {loading ? (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </div>
        ) : projects.length === 0 ? (
          <div className="rounded-lg border border-dashed p-6 text-center">
            <FolderKanban className="mx-auto size-8 text-muted-foreground/60" />
            <p className="mt-2 text-sm font-medium">No projects created yet</p>
            <p className="mt-1 text-xs text-muted-foreground">
              Create a project to start organizing tasks and generating smart schedules.
            </p>
            <div className="mt-3">
              <Link to="/projects" className={cn(buttonVariants({ size: 'sm', variant: 'outline' }))}>
                <Plus className="size-3" /> Create first project
              </Link>
            </div>
          </div>
        ) : (
          <div className="divide-y rounded-md border">
            {projects.slice(0, 3).map((project) => (
              <div
                key={project.id}
                className="flex flex-wrap items-center justify-between gap-3 p-3 transition-colors hover:bg-muted/40"
              >
                <div className="min-w-0">
                  <Link
                    to={`/projects?projectId=${project.id}`}
                    className="font-medium text-sm hover:underline"
                  >
                    {project.name}
                  </Link>
                  <p className="text-xs text-muted-foreground">
                    {project.startDate} → {project.endDate}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Link
                    to={`/generate?projectId=${project.id}`}
                    className={cn(buttonVariants({ size: 'sm', variant: 'ghost' }))}
                  >
                    <Sparkles className="size-3 text-primary" /> Generate
                  </Link>
                  <Link
                    to={`/projects?projectId=${project.id}`}
                    className={cn(buttonVariants({ size: 'sm', variant: 'outline' }))}
                  >
                    Open
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
