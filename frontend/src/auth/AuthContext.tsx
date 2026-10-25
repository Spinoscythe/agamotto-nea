import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import {
  authApi,
  isAccessTokenUsable,
  normalizeAccessToken,
  SESSION_STORAGE_KEY,
  setSessionToken,
  UNAUTHORIZED_EVENT,
  type UserResponse,
} from '@/api'

export interface SessionUser {
  id: string
  email: string
  fullName: string
  displayName: string
  token: string
}

interface AuthContextValue {
  user: SessionUser | null
  isAuthenticated: boolean
  loading: boolean
  login: (email: string, password: string) => Promise<SessionUser>
  register: (email: string, password: string, fullName: string) => Promise<SessionUser>
  logout: () => Promise<void>
  refreshUser: () => Promise<SessionUser | null>
}

function createAuthContext() {
  return createContext<AuthContextValue | null>(null)
}

// Reuse the same context object across Vite HMR so useAuth and AuthProvider
// do not briefly disagree and throw "must be used inside AuthProvider".
const AuthContext: ReturnType<typeof createAuthContext> =
  import.meta.hot?.data.authContext ?? createAuthContext()

if (import.meta.hot) {
  import.meta.hot.data.authContext = AuthContext
}

// save user + token in localStorage so they stay logged in after refresh
function saveSession(user: SessionUser) {
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(user))
  setSessionToken(user.token)
}

function clearSession() {
  localStorage.removeItem(SESSION_STORAGE_KEY)
  setSessionToken(null)
}

function loadSession(): SessionUser | null {
  const raw = localStorage.getItem(SESSION_STORAGE_KEY)
  if (raw == null || raw === '') {
    return null
  }
  try {
    const data = JSON.parse(raw)
    const token = normalizeAccessToken(data.token)
    if (data.id && data.email && token && isAccessTokenUsable(token)) {
      const session = {
        id: data.id,
        email: data.email,
        fullName: data.fullName || data.email,
        displayName: data.fullName || data.email,
        token: token,
      }
      setSessionToken(token)
      return session
    }
    localStorage.removeItem(SESSION_STORAGE_KEY)
    setSessionToken(null)
    return null
  } catch {
    return null
  }
}

function userFromResponse(user: UserResponse, token: string): SessionUser {
  const name = user.fullName
  return {
    id: user.id,
    email: user.email,
    fullName: name,
    displayName: name,
    token: normalizeAccessToken(token) ?? token,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(() => loadSession())

  useEffect(() => {
    const onUnauthorized = () => {
      clearSession()
      setUser(null)
    }
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [])

  async function login(email: string, password: string) {
    const response = await authApi.login({ email: email, password: password })
    const session = userFromResponse(response.user, response.token)
    saveSession(session)
    setUser(session)
    return session
  }

  async function register(email: string, password: string, fullName: string) {
    const response = await authApi.register({
      email: email,
      password: password,
      fullName: fullName,
    })
    const session = userFromResponse(response.user, response.token)
    saveSession(session)
    setUser(session)
    return session
  }

  async function logout() {
    clearSession()
    setUser(null)
  }

  // reload the user from the API using the saved id
  async function refreshUser() {
    if (user == null) {
      return null
    }
    try {
      const response = await authApi.getUser(user.id)
      const session = userFromResponse(response, user.token)
      saveSession(session)
      setUser(session)
      return session
    } catch {
      return user
    }
  }

  const value: AuthContextValue = {
    user: user,
    isAuthenticated: user != null,
    loading: false,
    login: login,
    register: register,
    logout: logout,
    refreshUser: refreshUser,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (ctx == null) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return ctx
}
