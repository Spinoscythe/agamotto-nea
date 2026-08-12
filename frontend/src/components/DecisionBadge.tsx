export function DecisionBadge({
  decision,
  overridden,
}: {
  decision: string
  overridden?: boolean
}) {
  return (
    <span className="text-sm">
      {decision}
      {overridden ? ' (override)' : null}
    </span>
  )
}
