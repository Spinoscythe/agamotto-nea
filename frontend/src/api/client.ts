import type { ErrorResponse } from './types'

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '')
  ?? 'http://localhost:8080'

export const SESSION_STORAGE_KEY = 'agamotto.session'
export const TOKEN_STORAGE_KEY = 'agamotto.session'

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

// read the jwt from the saved session in localStorage
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
  } catch (e) {
    return null
  }
}

export function setAccessToken(_token: string | null): void {
  // not used anymore - token is saved with the whole session
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
  } catch (e) {
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
    } catch (e) {
      throw new ApiError(response.status, 'Bad response from server')
    }
  }

  if (!response.ok) {
    let message = 'Request failed (' + response.status + ')'
    if (parsed != null && parsed.message) {
      message = parsed.message
    }
    throw new ApiError(response.status, message, parsed)
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

export function apiBaseUrl(): string {
  return BASE_URL
}
