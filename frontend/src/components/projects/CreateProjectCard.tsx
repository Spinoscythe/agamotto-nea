import type { SubmitEvent } from 'react'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'

interface CreateProjectCardProps {
  projectName: string
  projectDesc: string
  startDate: string
  endDate: string
  onNameChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onStartDateChange: (value: string) => void
  onEndDateChange: (value: string) => void
  onSubmit: (event: SubmitEvent) => void
}

export function CreateProjectCard({
  projectName,
  projectDesc,
  startDate,
  endDate,
  onNameChange,
  onDescriptionChange,
  onStartDateChange,
  onEndDateChange,
  onSubmit,
}: CreateProjectCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>New project</CardTitle>
        <CardDescription>
          A project holds your tasks. After you add tasks, you can generate a schedule.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={onSubmit}>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="projectName">Project name</FieldLabel>
              <Input
                id="projectName"
                value={projectName}
                onChange={(event) => onNameChange(event.target.value)}
                required
                maxLength={200}
                placeholder="e.g. A-level coursework"
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="projectDesc">Description</FieldLabel>
              <Textarea
                id="projectDesc"
                value={projectDesc}
                onChange={(event) => onDescriptionChange(event.target.value)}
                rows={2}
              />
            </Field>
            <div className="grid gap-3 sm:grid-cols-2">
              <Field>
                <FieldLabel htmlFor="startDate">Start</FieldLabel>
                <Input
                  id="startDate"
                  type="date"
                  max={endDate || undefined}
                  value={startDate}
                  onChange={(event) => {
                    const value = event.target.value
                    onStartDateChange(value)
                    if (value && endDate && endDate < value) onEndDateChange(value)
                  }}
                  required
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="endDate">End</FieldLabel>
                <Input
                  id="endDate"
                  type="date"
                  min={startDate || undefined}
                  value={endDate}
                  onChange={(event) => onEndDateChange(event.target.value)}
                  required
                />
              </Field>
            </div>
          </FieldGroup>
          <Button type="submit" className="w-fit">
            Create project
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
