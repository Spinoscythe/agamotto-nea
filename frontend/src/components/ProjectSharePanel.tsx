import { type FormEvent, useEffect, useState } from 'react'
import {
  ApiError,
  collaborationApi,
  type ProjectInviteResponse,
  type ProjectMemberResponse,
  type ProjectResponse,
  type ProjectRole,
} from '@/api'
import { useAuth } from '@/auth/AuthContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

export function ProjectSharePanel({
  project,
  onLeft,
}: {
  project: ProjectResponse
  onLeft?: () => void
}) {
  const { user } = useAuth()
  const [members, setMembers] = useState<ProjectMemberResponse[]>([])
  const [invites, setInvites] = useState<ProjectInviteResponse[]>([])
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<ProjectRole>('EDITOR')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const isOwner = user?.id === project.ownerId

  useEffect(() => {
    let cancelled = false
    setError(null)
    void (async () => {
      try {
        const listed = await collaborationApi.listMembers(project.id)
        if (cancelled) return
        setMembers(listed)
        if (user?.id === project.ownerId) {
          const pending = await collaborationApi.listInvites(project.id)
          if (!cancelled) setInvites(pending)
        } else {
          setInvites([])
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : 'Could not load members')
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [project.id, project.ownerId, user?.id])

  async function invite(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!email.trim()) return
    setBusy(true)
    setError(null)
    try {
      const created = await collaborationApi.invite(project.id, {
        email: email.trim(),
        role,
      })
      setInvites((prev) => [created, ...prev])
      setEmail('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not send invite')
    } finally {
      setBusy(false)
    }
  }

  async function cancelInvite(inviteId: string) {
    setBusy(true)
    setError(null)
    try {
      await collaborationApi.cancelInvite(project.id, inviteId)
      setInvites((prev) => prev.filter((invite) => invite.id !== inviteId))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not cancel invite')
    } finally {
      setBusy(false)
    }
  }

  async function removeMember(userId: string) {
    setBusy(true)
    setError(null)
    try {
      await collaborationApi.removeMember(project.id, userId)
      setMembers((prev) => prev.filter((member) => member.userId !== userId))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not remove member')
    } finally {
      setBusy(false)
    }
  }

  async function leave() {
    setBusy(true)
    setError(null)
    try {
      await collaborationApi.leave(project.id)
      onLeft?.()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not leave project')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Share project</CardTitle>
        <CardDescription>
          Invite registered users. Editors can change tasks and schedules; viewers can only look.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        <ul className="flex flex-col gap-2">
          {members.map((member) => (
            <li key={member.id} className="flex flex-wrap items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{member.fullName}</p>
                <p className="truncate text-xs text-muted-foreground">{member.email}</p>
              </div>
              <div className="flex items-center gap-2">
                <Badge variant="secondary">{member.role}</Badge>
                {isOwner && member.role !== 'OWNER' ? (
                  <Button
                    type="button"
                    size="sm"
                    variant="ghost"
                    disabled={busy}
                    onClick={() => void removeMember(member.userId)}
                  >
                    Remove
                  </Button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>

        {isOwner ? (
          <form className="grid gap-2 sm:grid-cols-[1fr_8rem_auto]" onSubmit={invite}>
            <Field>
              <FieldLabel htmlFor={`invite-email-${project.id}`}>Invite email</FieldLabel>
              <Input
                id={`invite-email-${project.id}`}
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="classmate@example.com"
                required
              />
            </Field>
            <Field>
              <FieldLabel>Role</FieldLabel>
              <Select
                value={role}
                onValueChange={(value) => {
                  if (value === 'EDITOR' || value === 'VIEWER') setRole(value)
                }}
              >
                <SelectTrigger className="w-full">
                  <SelectValue>{role}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="EDITOR">EDITOR</SelectItem>
                  <SelectItem value="VIEWER">VIEWER</SelectItem>
                </SelectContent>
              </Select>
            </Field>
            <div className="flex items-end">
              <Button type="submit" disabled={busy}>
                Invite
              </Button>
            </div>
          </form>
        ) : (
          <Button type="button" variant="outline" disabled={busy} onClick={() => void leave()}>
            Leave project
          </Button>
        )}

        {isOwner && invites.length > 0 ? (
          <div className="flex flex-col gap-2">
            <p className="text-sm font-medium">Pending invites</p>
            {invites.map((invite) => (
              <div key={invite.id} className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm">
                  {invite.inviteeEmail}{' '}
                  <span className="text-muted-foreground">as {invite.role}</span>
                </p>
                <Button
                  type="button"
                  size="sm"
                  variant="ghost"
                  disabled={busy}
                  onClick={() => void cancelInvite(invite.id)}
                >
                  Cancel
                </Button>
              </div>
            ))}
          </div>
        ) : null}
      </CardContent>
    </Card>
  )
}
