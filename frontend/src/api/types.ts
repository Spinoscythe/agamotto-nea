/** Types mirroring Spring Boot DTOs. */

export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type ScheduleMode = 'SERENITY' | 'CRUNCH'
export type PlanStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
export type BlockDecision = 'SCHEDULED' | 'DELAYED' | 'EXCLUDED'
export type ReportPeriod = 'DAILY' | 'WEEKLY' | 'MONTHLY'
export type ChangeType = 'CREATED' | 'EDITED' | 'DELETED' | 'STATUS_CHANGED'

export interface UserProfileResponse {
  id: string
  preferredStart: string
  preferredEnd: string
  includeWeekends: boolean
  weightPriority: number
  weightUrgency: number
  weightDuration: number
  updatedAt: string
}

export interface UserResponse {
  id: string
  fullName: string
  email: string
  createdAt: string
  profile: UserProfileResponse | null
}

export interface AuthResponse {
  token: string
  tokenType: string
  user: UserResponse
}

export interface ProjectResponse {
  id: string
  ownerId: string
  name: string
  description: string | null
  startDate: string
  endDate: string
  estimatedEffortHours: number
  createdAt: string
}

export interface TaskResponse {
  id: string
  projectId: string
  title: string
  description: string | null
  category: string
  priority: number
  deadline: string
  estimatedDurationHours: number
  correctedDurationHours: number | null
  complexity: number
  status: TaskStatus
  createdAt: string
  updatedAt: string
}

export interface ScheduleBlockResponse {
  id: string
  scheduleId: string
  taskId: string
  startTime: string | null
  endTime: string | null
  decision: BlockDecision
  reason: string | null
  manuallyOverridden: boolean
}

export interface SchedulePlanResponse {
  id: string
  projectId: string
  mode: ScheduleMode
  status: PlanStatus
  startDate: string
  endDate: string
  generatedAt: string
  explanationSummary: string | null
  blocks: ScheduleBlockResponse[]
}

export interface DashboardReportResponse {
  id: string
  userId: string
  period: ReportPeriod
  periodStart: string
  periodEnd: string
  scheduledCount: number
  delayedCount: number
  excludedCount: number
  completedCount: number
  generatedAt: string
}

export interface NotificationResponse {
  id: string
  userId: string
  taskId: string | null
  message: string
  createdAt: string
  sentAt: string | null
  read: boolean
}

export interface RescheduleResponse {
  outcome: 'MOVED' | 'REGENERATED'
  movedBlock: ScheduleBlockResponse | null
  regeneratedPlan: SchedulePlanResponse | null
}

export interface ErrorResponse {
  status: number
  error: string
  message: string
  path?: string
  timestamp?: string
}

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UpdateUserRequest {
  fullName?: string
  preferredStart?: string
  preferredEnd?: string
  includeWeekends?: boolean
  weightPriority?: number
  weightUrgency?: number
  weightDuration?: number
}

export interface CreateProjectRequest {
  ownerId: string
  name: string
  description?: string
  startDate: string
  endDate: string
  estimatedEffortHours: number
}

export interface CreateTaskRequest {
  actorUserId: string
  title: string
  description?: string
  category: string
  priority: number
  deadline: string
  estimatedDurationHours: number
  complexity: number
}

export interface GenerateScheduleRequest {
  startDate: string
  endDate: string
}

export interface OverrideBlockRequest {
  startTime?: string | null
  endTime?: string | null
  decision?: BlockDecision | null
  reason?: string | null
}

export interface RescheduleBlockRequest {
  startTime: string
  endTime: string
  reason?: string | null
}
