import { api } from './client'
import type {
  AuthResponse,
  CreateProjectRequest,
  CreateTaskRequest,
  DashboardReportResponse,
  GenerateScheduleRequest,
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
  UpdateUserRequest,
  UserResponse,
} from './types'

export * from './types'
export {
  ApiError,
  api,
  apiBaseUrl,
  getAccessToken,
  setAccessToken,
  SESSION_STORAGE_KEY,
  TOKEN_STORAGE_KEY,
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

export const usersApi = {
  get: authApi.getUser,
  update: authApi.updateProfile,
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
