import { Link } from 'react-router-dom'
import { Clock, Palmtree, Plus, Sparkles } from 'lucide-react'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'

export function QuickActionsCard() {
  return (
    <Card className="p-5">
      <CardHeader className="p-0">
        <CardTitle className="text-base font-semibold">Quick Actions</CardTitle>
        <CardDescription className="text-xs">Common workflows at a glance</CardDescription>
      </CardHeader>
      <CardContent className="mt-4 flex flex-col gap-2 p-0">
        <Link
          to="/generate"
          className={cn(
            buttonVariants({ variant: 'default' }),
            'w-full justify-start gap-2 shadow-xs'
          )}
        >
          <Sparkles className="size-4" />
          Generate Schedule
        </Link>
        <Link
          to="/projects"
          className={cn(buttonVariants({ variant: 'outline' }), 'w-full justify-start gap-2')}
        >
          <Plus className="size-4" />
          Create New Project
        </Link>
        <Link
          to="/leaves"
          className={cn(buttonVariants({ variant: 'outline' }), 'w-full justify-start gap-2')}
        >
          <Palmtree className="size-4" />
          Manage Leaves & Breaks
        </Link>
        <Link
          to="/history"
          className={cn(buttonVariants({ variant: 'ghost' }), 'w-full justify-start gap-2')}
        >
          <Clock className="size-4" />
          Schedule Plan History
        </Link>
      </CardContent>
    </Card>
  )
}
