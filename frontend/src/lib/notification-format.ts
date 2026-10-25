import type { NotificationResponse } from '@/api'

export interface FormattedNotification {
    category: 'SCHEDULE' | 'INVITE' | 'TASK' | 'GENERAL'
    mode?: 'SERENITY' | 'CRUNCH'
    title: string
    body: string
    stats?: { workload?: string; capacity?: string; excludedCount?: number }
    relativeTime: string
}

export function formatNotification(n: NotificationResponse): FormattedNotification {
    const raw = n.message.trim()
    const relativeTime = getRelativeTime(new Date(n.createdAt))

    // 1. Project Invites
    if (n.type === 'PROJECT_INVITE' || raw.toLowerCase().includes('invited to project')) {
        return {
            category: 'INVITE',
            title: 'Project Invitation',
            body: raw,
            relativeTime,
        }
    }

    // 2. Schedule Alerts
    const isSchedule = n.type === 'SCHEDULE' || /schedule|serenity|crunch/i.test(raw)
    if (isSchedule) {
        const isSerenity = /serenity/i.test(raw)
        const isCrunch = /crunch/i.test(raw)
        const mode = isSerenity ? 'SERENITY' : isCrunch ? 'CRUNCH' : undefined

        // Extract hours from "(14.00h <= 88.00h)" or "14.00h scheduled out of 88.00h"
        const matchHours = raw.match(/(\d+(?:\.\d+)?h)\s*(?:<=|scheduled out of|exceeds(?: available time)?|of|\/)\s*(\d+(?:\.\d+)?h)/i)
        const stats = matchHours
            ? { workload: matchHours[1], capacity: matchHours[2] }
            : undefined

        // Clean up repetitive and robotic jargon from legacy messages
        let cleanBody = raw
            .replace(/^Schedule (?:generated in|ready) (?:SERENITY|CRUNCH) mode[:.]?\s*/i, '')
            .replace(/^(?:Serenity|Crunch) mode:\s*/i, '')
            .replace(/\(\d+(?:\.\d+)?h\s*<=\s*\d+(?:\.\d+)?h\)[.]?\s*/i, '')
            .replace(/Tasks ordered by score \(priority, deadline urgency, duration\) with deadline tie-break[.]?/i, 'Tasks organized by deadline and priority.')
            .replace(/survivors placed by score[.]?/i, 'High priority tasks placed on calendar.')
            .trim()

        if (!cleanBody) {
            cleanBody = isSerenity
                ? 'All tasks fit comfortably into your schedule.'
                : 'Workload exceeds available time. Some tasks were set aside.'
        }

        const isActionRequired = /assign (?:an )?assignee|assign team members/i.test(raw)
        const title = isActionRequired
            ? 'Schedule Generated · Action Required'
            : isSerenity
            ? 'Schedule Ready · Serenity Mode'
            : isCrunch
            ? 'Schedule Ready · Crunch Mode'
            : 'Schedule Updated'

        return {
            category: 'SCHEDULE',
            mode,
            title,
            body: cleanBody,
            stats,
            relativeTime,
        }
    }

    // 3. Task Assignment
    if (/you have been assigned to task/i.test(raw)) {
        return {
            category: 'TASK',
            title: 'Task Assignment',
            body: raw,
            relativeTime,
        }
    }

    // 4. Task Exclusions / Deadlines
    if (raw.startsWith("Task '") || n.taskId) {
        return {
            category: 'TASK',
            title: 'Task Status Alert',
            body: raw,
            relativeTime,
        }
    }

    return {
        category: 'GENERAL',
        title: 'Notification',
        body: raw,
        relativeTime,
    }
}

function getRelativeTime(date: Date): string {
    const now = new Date()
    const diffSec = Math.floor((now.getTime() - date.getTime()) / 1000)
    if (diffSec < 60) return 'Just now'
    const diffMin = Math.floor(diffSec / 60)
    if (diffMin < 60) return `${diffMin}m ago`
    const diffHr = Math.floor(diffMin / 60)
    if (diffHr < 24) return `${diffHr}h ago`
    const diffDays = Math.floor(diffHr / 24)
    if (diffDays < 7) return `${diffDays}d ago`
    return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}
