import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Plus } from 'lucide-react'
import { toast } from 'sonner'
import {
  ApiError,
  leavesApi,
  type CreateLeaveRequest,
  type LeaveResponse,
  type LeaveType,
} from '@/api'
import { AddLeaveFormCard } from '@/components/leaves/AddLeaveFormCard'
import { LeaveMetricsCards } from '@/components/leaves/LeaveMetricsCards'
import { LeavesListCard } from '@/components/leaves/LeavesListCard'
import { getDefaultDateTimeInput } from '@/components/leaves/leave-utils'
import { PageHeader } from '@/components/PageHeader'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'

export function LeavesPage() {
  const [leaves, setLeaves] = useState<LeaveResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showAddForm, setShowAddForm] = useState(false)
  const [saving, setSaving] = useState(false)
  const [selectedFilter, setSelectedFilter] = useState<string>('ALL')

  const [title, setTitle] = useState('')
  const [type, setType] = useState<LeaveType>('VACATION')
  const [startTime, setStartTime] = useState(() => getDefaultDateTimeInput(24))
  const [endTime, setEndTime] = useState(() => getDefaultDateTimeInput(32))
  const [reason, setReason] = useState('')

  async function loadLeaves() {
    setLoading(true)
    setError(null)
    try {
      const data = await leavesApi.list()
      setLeaves(data)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load leaves')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadLeaves()
  }, [])

  async function handleCreate(e: FormEvent) {
    e.preventDefault()
    if (!title.trim()) {
      toast.error('Title is required')
      return
    }
    if (!startTime || !endTime) {
      toast.error('Start and end times are required')
      return
    }
    if (new Date(endTime) <= new Date(startTime)) {
      toast.error('End time must be after start time')
      return
    }

    setSaving(true)
    try {
      const payload: CreateLeaveRequest = {
        title: title.trim(),
        type,
        startTime,
        endTime,
        reason: reason.trim() || null,
      }
      await leavesApi.create(payload)
      toast.success('Leave scheduled successfully! Agamotto will protect these hours.')
      setTitle('')
      setReason('')
      setShowAddForm(false)
      await loadLeaves()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Failed to save leave')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: string, leaveTitle: string) {
    if (!window.confirm(`Delete leave "${leaveTitle}"?`)) return
    try {
      await leavesApi.remove(id)
      toast.success(`Deleted leave "${leaveTitle}"`)
      setLeaves((prev) => prev.filter((l) => l.id !== id))
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Failed to delete leave')
    }
  }

  const filteredLeaves = useMemo(() => {
    if (selectedFilter === 'ALL') return leaves
    return leaves.filter((l) => l.type === selectedFilter)
  }, [leaves, selectedFilter])

  const totalLeaveHours = useMemo(() => {
    return leaves.reduce((sum, l) => {
      const s = new Date(l.startTime).getTime()
      const e = new Date(l.endTime).getTime()
      return sum + Math.max(0, (e - s) / (1000 * 60 * 60))
    }, 0)
  }, [leaves])

  const nextUpcomingLeave = useMemo(() => {
    const now = Date.now()
    const upcoming = leaves
      .filter((l) => new Date(l.endTime).getTime() >= now)
      .sort((a, b) => a.startTime.localeCompare(b.startTime))
    return upcoming[0] ?? null
  }, [leaves])

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Leaves & Breaks"
        description="Book your planned time off, vacations, and study breaks. Agamotto's scheduler strictly reserves these periods."
        actions={
          <Button
            type="button"
            onClick={() => setShowAddForm((prev) => !prev)}
            className="gap-2"
          >
            <Plus className="size-4" />
            {showAddForm ? 'Cancel' : 'Add Leave / Break'}
          </Button>
        }
      />

      {error ? (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      <LeaveMetricsCards
        leavesCount={leaves.length}
        totalLeaveHours={totalLeaveHours}
        nextUpcomingLeave={nextUpcomingLeave}
      />

      {showAddForm ? (
        <AddLeaveFormCard
          title={title}
          type={type}
          startTime={startTime}
          endTime={endTime}
          reason={reason}
          saving={saving}
          onTitleChange={setTitle}
          onTypeChange={setType}
          onStartTimeChange={setStartTime}
          onEndTimeChange={setEndTime}
          onReasonChange={setReason}
          onSubmit={handleCreate}
          onCancel={() => setShowAddForm(false)}
        />
      ) : null}

      <LeavesListCard
        leaves={leaves}
        filteredLeaves={filteredLeaves}
        loading={loading}
        selectedFilter={selectedFilter}
        onFilterChange={setSelectedFilter}
        onAddFirst={() => setShowAddForm(true)}
        onDelete={(id, leaveTitle) => void handleDelete(id, leaveTitle)}
      />
    </div>
  )
}
