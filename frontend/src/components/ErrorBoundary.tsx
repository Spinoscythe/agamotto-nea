import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button, buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'

export function ErrorBoundary({ children }: { children: ReactNode }) {
  const location = useLocation()
  return <ErrorBoundaryInner resetKey={location.key}>{children}</ErrorBoundaryInner>
}

class ErrorBoundaryInner extends Component<
  { children: ReactNode; resetKey: string },
  { error: Error | null; resetKey: string }
> {
  state = { error: null as Error | null, resetKey: this.props.resetKey }

  static getDerivedStateFromError(error: Error) {
    return { error }
  }

  static getDerivedStateFromProps(
    props: { resetKey: string },
    state: { error: Error | null; resetKey: string },
  ) {
    if (props.resetKey !== state.resetKey) {
      return { error: null, resetKey: props.resetKey }
    }
    return null
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('UI crash', error, info.componentStack)
  }

  render() {
    if (this.state.error) {
      const message = this.state.error.message?.trim()
      return (
        <div className="mx-auto flex min-h-[40vh] max-w-lg flex-col justify-center gap-4 p-6">
          <Alert variant="destructive">
            <AlertTitle>Something went wrong</AlertTitle>
            <AlertDescription>
              {message && !message.startsWith('Minified')
                ? message
                : 'This page hit an unexpected error. You can try again or go back to Overview.'}
            </AlertDescription>
          </Alert>
          <div className="flex flex-wrap gap-2">
            <Button type="button" onClick={() => this.setState({ error: null })}>
              Try again
            </Button>
            <Link to="/dashboard" className={cn(buttonVariants({ variant: 'outline' }))}>
              Back to Overview
            </Link>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
