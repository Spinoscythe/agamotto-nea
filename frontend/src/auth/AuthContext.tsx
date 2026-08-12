import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import { authApi, type UserResponse } from '@/api'

const STORAGE_KEY = 'agamotto.session'

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

const AuthContext = createContext<AuthContextValue | null>(null)

// save user + token in localStorage so they stay logged in after refresh
function saveSession(user: SessionUser) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

function clearSession() {
  localStorage.removeItem(STORAGE_KEY)
}

function loadSession(): SessionUser | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw == null || raw === '') {
    return null
  }
  try {
    const data = JSON.parse(raw)
    if (data.id && data.email && data.token) {
      return {
        id: data.id,
        email: data.email,
        fullName: data.fullName || data.email,
        displayName: data.fullName || data.email,
        token: data.token,
      }
    }
    return null
  } catch (e) {
    // bad json in storage
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
    token: token,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null)
  const [loading, setLoading] = useState(true)

  // on page load, check if we already have a saved session
  useEffect(() => {
    const saved = loadSession()
    if (saved != null) {
      setUser(saved)
    }
    setLoading(false)
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
    } catch (e) {
      // if it fails just keep what we have
      return user
    }
  }

  const value: AuthContextValue = {
    user: user,
    isAuthenticated: user != null,
    loading: loading,
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

// used by other files if they need the token
export function getSavedToken(): string | null {
  const saved = loadSession()
  if (saved == null) {
    return null
  }
  return saved.token
}
