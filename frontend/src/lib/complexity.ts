export const COMPLEXITY_EASY = 1
export const COMPLEXITY_MEDIUM = 3
export const COMPLEXITY_HARD = 5

export function complexityLabel(value: number): string {
  if (!Number.isFinite(value)) return '—'
  if (value <= 2) return 'Easy'
  if (value === 3) return 'Medium'
  return 'Hard'
}
