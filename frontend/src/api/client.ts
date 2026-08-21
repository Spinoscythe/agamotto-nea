import type { ErrorResponse } from './types'

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '')
  ?? 'http://localhost:8080'

export const SESSION_STORAGE_KEY = 'agamotto.session'

export class ApiError extends Error {
  readonly status: number
  readonly body: ErrorResponse | null

  constructor(status: number, message: string, body: ErrorResponse | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown
  query?: Record<string, string | number | boolean | undefined | null>
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  let url = BASE_URL + path
  if (query) {
    const parts: string[] = []
    for (const key in query) {
      const value = query[key]
      if (value !== undefined && value !== null && value !== '') {
        parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(String(value)))
      }
    }
    if (parts.length > 0) {
      url = url + '?' + parts.join('&')
    }
  }
  return url
}

export function getAccessToken(): string | null {
  const raw = localStorage.getItem(SESSION_STORAGE_KEY)
  if (raw == null || raw === '') {
    return null
  }
  try {
    const data = JSON.parse(raw)
    if (data.token) {
      return data.token
    }
    return null
  } catch {
    return null
  }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, query, headers, ...rest } = options

  const token = getAccessToken()

  const requestHeaders: Record<string, string> = {
    Accept: 'application/json',
  }
  if (body !== undefined) {
    requestHeaders['Content-Type'] = 'application/json'
  }
  if (token != null && token !== '') {
    requestHeaders['Authorization'] = 'Bearer ' + token
  }

  let response: Response
  try {
    response = await fetch(buildUrl(path, query), {
      ...rest,
      headers: {
        ...requestHeaders,
        ...headers,
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Cannot reach API at ' + BASE_URL + '. Is the Spring Boot server running?')
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  let parsed: any = null
  if (text !== '') {
    try {
      parsed = JSON.parse(text)
    } catch {
      throw new ApiError(response.status, 'Bad response from server')
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, messageFromErrorBody(parsed, response.status), parsed)
  }

  return parsed as T
}

export const api = {
  get: <T>(path: string, query?: RequestOptions['query']) =>
    apiRequest<T>(path, { method: 'GET', query }),
  post: <T>(path: string, body?: unknown, query?: RequestOptions['query']) =>
    apiRequest<T>(path, { method: 'POST', body, query }),
  put: <T>(path: string, body?: unknown) =>
    apiRequest<T>(path, { method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown) =>
    apiRequest<T>(path, { method: 'PATCH', body }),
  delete: <T>(path: string, query?: RequestOptions['query']) =>
    apiRequest<T>(path, { method: 'DELETE', query }),
}

function messageFromErrorBody(parsed: any, status: number): string {
  const fallback = 'Request failed (' + status + ')'
  if (parsed == null || typeof parsed !== 'object') {
    return fallback
  }

  let message = ''
  if (typeof parsed.message === 'string' && parsed.message.trim() !== '') {
    message = parsed.message.trim()
  } else if (typeof parsed.detail === 'string' && parsed.detail.trim() !== '') {
    message = parsed.detail.trim()
  } else if (typeof parsed.error === 'string' && parsed.error.trim() !== '') {
    message = parsed.error.trim()
  } else if (Array.isArray(parsed.errors) && parsed.errors.length > 0) {
    message = parsed.errors
      .map((item: unknown) => {
        if (typeof item === 'string') return item
        if (item && typeof item === 'object' && 'message' in item) {
          return String((item as { message: unknown }).message)
        }
        return ''
      })
      .filter(Boolean)
      .join('; ')
  }

  if (message === '') {
    return fallback
  }
  return simplifyJacksonMessage(message)
}

/** Jackson/Spring unreadable-body text is long; keep the useful clause. */
function simplifyJacksonMessage(message: string): string {
  if (!message.startsWith('JSON parse error:') && !message.includes('Cannot deserialize')
      && !message.includes('Cannot map `null`')) {
    return message
  }
  if (message.includes('Cannot map `null`')) {
    return 'A required number was empty or invalid. Check hours, priority, and complexity.'
  }
  if (message.includes('LocalDate') || message.includes('LocalDateTime') || message.includes('LocalTime')) {
    return 'A date or time was missing or invalid. Check the start, end, and deadline fields.'
  }
  const firstLine = message.split('\n')[0]
  return firstLine.length > 220 ? firstLine.slice(0, 217) + '…' : firstLine
}

export function apiBaseUrl(): string {
  return BASE_URL
}
