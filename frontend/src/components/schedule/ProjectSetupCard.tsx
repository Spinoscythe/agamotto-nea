import type { ProjectResponse } from '@/api'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

interface ProjectSetupCardProps {
  projects: ProjectResponse[]
  projectId: string
  selectedProject: ProjectResponse | null
  startDate: string
  endDate: string
  onProjectChange: (projectId: string) => void
  onStartDateChange: (date: string) => void
  onEndDateChange: (date: string) => void
}

export function ProjectSetupCard({
  projects,
  projectId,
  selectedProject,
  startDate,
  endDate,
  onProjectChange,
  onStartDateChange,
  onEndDateChange,
}: ProjectSetupCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Project & dates</CardTitle>
        <CardDescription>
          Choose which project to schedule and the date range to fill.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <FieldGroup className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Field>
            <FieldLabel htmlFor="project">Project</FieldLabel>
            <Select
              value={projectId}
              onValueChange={(value) => {
                if (value != null) onProjectChange(String(value))
              }}
            >
              <SelectTrigger id="project" className="w-full">
                <SelectValue>
                  {selectedProject?.name ?? 'Select a project'}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  {projects.map((project) => (
                    <SelectItem key={project.id} value={project.id}>
                      {project.name}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          </Field>
          <Field>
            <FieldLabel htmlFor="schedStart">Start Date</FieldLabel>
            <Input
              id="schedStart"
              type="date"
              value={startDate}
              onChange={(event) => onStartDateChange(event.target.value)}
              required
            />
          </Field>
          <Field>
            <FieldLabel htmlFor="schedEnd">End Date</FieldLabel>
            <Input
              id="schedEnd"
              type="date"
              value={endDate}
              onChange={(event) => onEndDateChange(event.target.value)}
              required
            />
          </Field>
        </FieldGroup>
      </CardContent>
    </Card>
  )
}
