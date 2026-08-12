import type { SchedulePlanResponse } from '@/api'

/** Stored generate summary, or a short fallback from block decisions for older plans. */
export function planExplanation(plan: SchedulePlanResponse): string | null {
  const stored = plan.explanationSummary?.trim()
  if (stored) return stored
  const blocks = plan.blocks ?? []
  if (blocks.length === 0) return null
  const scheduled = blocks.filter((b) => b.decision === 'SCHEDULED').length
  const delayed = blocks.filter((b) => b.decision === 'DELAYED').length
  const excluded = blocks.filter((b) => b.decision === 'EXCLUDED').length
  const parts = [`${scheduled} scheduled`]
  if (delayed > 0) parts.push(`${delayed} delayed`)
  if (excluded > 0) parts.push(`${excluded} excluded`)
  return `${plan.mode}: ${parts.join(', ')}.`
}
