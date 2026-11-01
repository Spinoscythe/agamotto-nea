import { Link } from 'react-router-dom'
import { DotsThreeIcon, PlusIcon } from '@phosphor-icons/react'
import type { ProjectResponse } from '@/api'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { cn } from '@/lib/utils'

interface ProjectGridProps {
  projects: ProjectResponse[]
  selectedProjectId: string
  currentUserId?: string
  loading: boolean
  onSelectProject: (projectId: string) => void
  onDeleteProject: (projectId: string) => void
  onCreateFirst: () => void
}

export function ProjectGrid({
  projects,
  selectedProjectId,
  currentUserId,
  loading,
  onSelectProject,
  onDeleteProject,
  onCreateFirst,
}: ProjectGridProps) {
  if (projects.length > 0) {
    return (
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {projects.map((project) => (
          <Card
            key={project.id}
            size="sm"
            className={cn(
              'relative cursor-pointer transition-colors hover:bg-muted/40',
              project.id === selectedProjectId && 'ring-2 ring-ring',
            )}
            onClick={() => onSelectProject(project.id)}
          >
            <CardHeader className="pr-12">
              <CardTitle className="text-base">{project.name}</CardTitle>
              <CardDescription>
                {project.startDate} → {project.endDate}
              </CardDescription>
            </CardHeader>
            <div
              className="absolute top-2 right-2"
              onClick={(event) => event.stopPropagation()}
              onKeyDown={(event) => event.stopPropagation()}
            >
              <DropdownMenu>
                <DropdownMenuTrigger
                  render={
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label={`Actions for ${project.name}`}
                    />
                  }
                >
                  <DotsThreeIcon />
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="min-w-44">
                  <DropdownMenuGroup>
                    <DropdownMenuItem
                      render={<Link to={`/generate?projectId=${project.id}`} />}
                    >
                      Add tasks & generate
                    </DropdownMenuItem>
                    {currentUserId === project.ownerId ? (
                      <DropdownMenuItem
                        variant="destructive"
                        onClick={() => onDeleteProject(project.id)}
                      >
                        Delete project
                      </DropdownMenuItem>
                    ) : null}
                    <DropdownMenuItem render={<Link to="/settings" />}>
                      Edit preferences
                    </DropdownMenuItem>
                  </DropdownMenuGroup>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </Card>
        ))}
      </div>
    )
  }

  if (loading) return null

  return (
    <Empty className="border border-dashed">
      <EmptyHeader>
        <EmptyTitle>No projects yet</EmptyTitle>
        <EmptyDescription>
          Create a project first. Then add tasks and generate a schedule.
        </EmptyDescription>
      </EmptyHeader>
      <EmptyContent>
        <Button type="button" onClick={onCreateFirst}>
          <PlusIcon data-icon="inline-start" />
          New project
        </Button>
      </EmptyContent>
    </Empty>
  )
}
