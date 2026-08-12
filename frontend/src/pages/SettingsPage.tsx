import { useEffect, useState } from 'react'
import { ApiError, authApi, type UserProfileResponse } from '@/api'
import { useAuth } from '@/auth/AuthContext'
import { PageHeader } from '@/components/PageHeader'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import * as React from "react";

export function SettingsPage() {
  const { user, refreshUser } = useAuth()
  const [, setProfile] = useState<UserProfileResponse | null>(null)
  const [fullName, setFullName] = useState('')
  const [preferredStart, setPreferredStart] = useState('09:00')
  const [preferredEnd, setPreferredEnd] = useState('17:00')
  const [includeWeekends, setIncludeWeekends] = useState(true)
  const [weightPriority, setWeightPriority] = useState(1)
  const [weightUrgency, setWeightUrgency] = useState(1)
  const [weightDuration, setWeightDuration] = useState(1)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [pending, setPending] = useState(false)

  useEffect(() => {
    if (!user) return
    let cancelled = false
    void authApi
      .getUser(user.id)
      .then((u) => {
        if (cancelled) return
        const p = u.profile
        setProfile(p)
        setFullName(u.fullName || user.fullName || user.displayName)
        setPreferredStart((p?.preferredStart ?? '09:00').slice(0, 5))
        setPreferredEnd((p?.preferredEnd ?? '17:00').slice(0, 5))
        setIncludeWeekends(p?.includeWeekends ?? true)
        setWeightPriority(p?.weightPriority ?? 1)
        setWeightUrgency(p?.weightUrgency ?? 1)
        setWeightDuration(p?.weightDuration ?? 1)
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : 'Failed to load preferences')
        }
      })
    return () => {
      cancelled = true
    }
  }, [user])

  async function onSubmit(e: React.SubmitEvent) {
    e.preventDefault()
    if (!user) return
    setError(null)
    setSaved(false)
    setPending(true)
    try {
      const updated = await authApi.updateProfile(user.id, {
        fullName: fullName.trim(),
        preferredStart,
        preferredEnd,
        includeWeekends,
        weightPriority,
        weightUrgency,
        weightDuration,
      })
      setProfile(updated.profile)
      await refreshUser()
      setSaved(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save preferences')
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Settings"
        description="Working hours and weighting used when generating Serenity or Crunch schedules."
      />

      {error ? (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      {saved ? (
        <Alert>
          <AlertDescription>Preferences saved.</AlertDescription>
        </Alert>
      ) : null}

      <Card className="max-w-lg">
        <CardHeader>
          <CardTitle>User preferences</CardTitle>
          <CardDescription>
            These weights influence how tasks are ordered during schedule generation.
          </CardDescription>
        </CardHeader>
        <form onSubmit={onSubmit}>
          <CardContent>
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="fullName">Full name</FieldLabel>
                <Input
                  id="fullName"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                />
              </Field>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field>
                  <FieldLabel htmlFor="dayStart">Working day start</FieldLabel>
                  <Input
                    id="dayStart"
                    type="time"
                    value={preferredStart}
                    onChange={(e) => setPreferredStart(e.target.value)}
                    required
                  />
                </Field>
                <Field>
                  <FieldLabel htmlFor="dayEnd">Working day end</FieldLabel>
                  <Input
                    id="dayEnd"
                    type="time"
                    value={preferredEnd}
                    onChange={(e) => setPreferredEnd(e.target.value)}
                    required
                  />
                </Field>
              </div>
              <Field>
                <label className="flex items-center gap-2 text-sm">
                  <input
                    id="includeWeekends"
                    type="checkbox"
                    checked={includeWeekends}
                    onChange={(e) => setIncludeWeekends(e.target.checked)}
                    className="size-4 accent-[var(--color-primary,#0f766e)]"
                  />
                  Include weekends when scheduling
                </label>
              </Field>
              <div className="grid gap-4 sm:grid-cols-3">
                <Field>
                  <FieldLabel htmlFor="wPriority">Priority weight</FieldLabel>
                  <Input
                    id="wPriority"
                    type="number"
                    min={0}
                    step={0.1}
                    value={weightPriority}
                    onChange={(e) => setWeightPriority(Number(e.target.value))}
                  />
                </Field>
                <Field>
                  <FieldLabel htmlFor="wUrgency">Urgency weight</FieldLabel>
                  <Input
                    id="wUrgency"
                    type="number"
                    min={0}
                    step={0.1}
                    value={weightUrgency}
                    onChange={(e) => setWeightUrgency(Number(e.target.value))}
                  />
                </Field>
                <Field>
                  <FieldLabel htmlFor="wDuration">Duration weight</FieldLabel>
                  <Input
                    id="wDuration"
                    type="number"
                    min={0}
                    step={0.1}
                    value={weightDuration}
                    onChange={(e) => setWeightDuration(Number(e.target.value))}
                  />
                </Field>
              </div>
            </FieldGroup>
          </CardContent>
          <CardFooter className="border-t">
            <Button type="submit" disabled={pending}>
              {pending ? 'Saving…' : 'Save preferences'}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}
