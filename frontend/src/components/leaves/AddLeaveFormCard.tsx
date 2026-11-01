import type { FormEvent } from 'react'
import type { LeaveType } from '@/api'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { LEAVE_TYPES } from './leave-utils'

interface AddLeaveFormCardProps {
  title: string
  type: LeaveType
  startTime: string
  endTime: string
  reason: string
  saving: boolean
  onTitleChange: (value: string) => void
  onTypeChange: (value: LeaveType) => void
  onStartTimeChange: (value: string) => void
  onEndTimeChange: (value: string) => void
  onReasonChange: (value: string) => void
  onSubmit: (event: FormEvent) => void
  onCancel: () => void
}

export function AddLeaveFormCard({
  title,
  type,
  startTime,
  endTime,
  reason,
  saving,
  onTitleChange,
  onTypeChange,
  onStartTimeChange,
  onEndTimeChange,
  onReasonChange,
  onSubmit,
  onCancel,
}: AddLeaveFormCardProps) {
  return (
    <Card className="border-primary/40 shadow-sm animate-in fade-in-50">
      <CardHeader>
        <CardTitle className="text-base font-semibold">Schedule a Leave or Break</CardTitle>
        <CardDescription className="text-xs">
          Any workload during this window will automatically be rescheduled or held in your backlog.
        </CardDescription>
      </CardHeader>
      <form onSubmit={onSubmit}>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field>
              <FieldLabel>Title</FieldLabel>
              <Input
                required
                placeholder="e.g. Reading Week, Spain Trip, Dentist"
                value={title}
                onChange={(e) => onTitleChange(e.target.value)}
              />
            </Field>

            <Field>
              <FieldLabel>Type of Absence</FieldLabel>
              <select
                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-hidden focus-visible:ring-1 focus-visible:ring-ring"
                value={type}
                onChange={(e) => onTypeChange(e.target.value as LeaveType)}
              >
                {LEAVE_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field>
              <FieldLabel>Start Date & Time</FieldLabel>
              <Input
                type="datetime-local"
                required
                value={startTime}
                onChange={(e) => onStartTimeChange(e.target.value)}
              />
            </Field>

            <Field>
              <FieldLabel>End Date & Time</FieldLabel>
              <Input
                type="datetime-local"
                required
                value={endTime}
                onChange={(e) => onEndTimeChange(e.target.value)}
              />
            </Field>
          </div>

          <Field>
            <FieldLabel>Reason or Notes (Optional)</FieldLabel>
            <Textarea
              placeholder="Optional details for this break..."
              rows={2}
              value={reason}
              onChange={(e) => onReasonChange(e.target.value)}
            />
          </Field>
        </CardContent>
        <CardFooter className="flex justify-end gap-2 border-t pt-4">
          <Button type="button" variant="outline" onClick={onCancel} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Protect Hours'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  )
}
