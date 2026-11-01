import { api } from './client'
import type {
  AuthResponse,
  CreateLeaveRequest,
  CreateProjectRequest,
  CreateTaskRequest,
  DashboardReportResponse,
  GenerateScheduleRequest,
  LeaveResponse,
  LoginRequest,
  NotificationResponse,
  OverrideBlockRequest,
  ProjectResponse,
  RescheduleBlockRequest,
  RegisterRequest,
  ReportPeriod,
  RescheduleResponse,
  ScheduleBlockResponse,
  SchedulePlanResponse,
  TaskResponse,
  UpdateLeaveRequest,
  UpdateTaskRequest,
  UpdateUserRequest,
  UserResponse,
} from './types'

export * from './types'
export {
  ApiError,
  api,
  SESSION_STORAGE_KEY,
  UNAUTHORIZED_EVENT,
  isAccessTokenUsable,
  normalizeAccessToken,
  setSessionToken,
} from './client'

export const authApi = {
  register: (body: RegisterRequest) =>
    api.post<AuthResponse>('/api/auth/register', body),
  login: (body: LoginRequest) =>
    api.post<AuthResponse>('/api/auth/login', body),
  getUser: (userId: string) =>
    api.get<UserResponse>(`/api/users/${userId}`),
  updateProfile: (userId: string, body: UpdateUserRequest) =>
    api.patch<UserResponse>(`/api/users/${userId}`, body),
}

export const projectsApi = {
  list: (ownerId: string) =>
    api.get<ProjectResponse[]>('/api/projects', { ownerId }),
  get: (projectId: string) =>
    api.get<ProjectResponse>(`/api/projects/${projectId}`),
  create: (body: CreateProjectRequest) =>
    api.post<ProjectResponse>('/api/projects', body),
  update: (projectId: string, body: Partial<CreateProjectRequest>) =>
    api.put<ProjectResponse>(`/api/projects/${projectId}`, body),
  remove: (projectId: string) =>
    api.delete<void>(`/api/projects/${projectId}`),
}

export const tasksApi = {
  listByProject: (projectId: string) =>
    api.get<TaskResponse[]>(`/api/projects/${projectId}/tasks`),
  get: (taskId: string) =>
    api.get<TaskResponse>(`/api/tasks/${taskId}`),
  create: (projectId: string, body: CreateTaskRequest) =>
    api.post<TaskResponse>(`/api/projects/${projectId}/tasks`, body),
  update: (taskId: string, body: UpdateTaskRequest) =>
    api.patch<TaskResponse>(`/api/tasks/${taskId}`, body),
  remove: (taskId: string, actorUserId: string) =>
    api.delete<TaskResponse>(`/api/tasks/${taskId}`, { actorUserId }),
}

export const schedulesApi = {
  generate: (projectId: string, body: GenerateScheduleRequest) =>
    api.post<SchedulePlanResponse>(`/api/projects/${projectId}/schedule`, body),
  listByProject: (projectId: string) =>
    api.get<SchedulePlanResponse[]>(`/api/projects/${projectId}/schedules`),
  get: (planId: string) =>
    api.get<SchedulePlanResponse>(`/api/schedules/${planId}`),
  overrideBlock: (blockId: string, body: OverrideBlockRequest) =>
    api.patch<ScheduleBlockResponse>(`/api/schedule-blocks/${blockId}`, body),
  rescheduleBlock: (blockId: string, body: RescheduleBlockRequest) =>
    api.post<RescheduleResponse>(`/api/schedule-blocks/${blockId}/reschedule`, body),
  markUnattended: (blockId: string) =>
    api.post<RescheduleResponse>(`/api/schedule-blocks/${blockId}/unattended`),
}

export const leavesApi = {
  list: (params?: { start?: string; end?: string }) =>
    api.get<LeaveResponse[]>('/api/leaves', params),
  get: (id: string) =>
    api.get<LeaveResponse>(`/api/leaves/${id}`),
  create: (body: CreateLeaveRequest) =>
    api.post<LeaveResponse>('/api/leaves', body),
  update: (id: string, body: UpdateLeaveRequest) =>
    api.patch<LeaveResponse>(`/api/leaves/${id}`, body),
  remove: (id: string) =>
    api.delete<void>(`/api/leaves/${id}`),
}

export const dashboardApi = {
  get: (userId: string, period: ReportPeriod) =>
    api.get<DashboardReportResponse>('/api/dashboard', { userId, period }),
}

export const notificationsApi = {
  listUnread: (userId: string) =>
    api.get<NotificationResponse[]>('/api/notifications', { userId }),
  markRead: (notificationId: string) =>
    api.post<NotificationResponse>(`/api/notifications/${notificationId}/read`),
}
