export type GlobalRole = 'PLATFORM_ADMIN'

export type AuthAppContext = {
  organizationId: string | null
  organizationName: string | null
  establishmentId: string | null
  establishmentName: string | null
}

export type AuthUser = {
  id: string
  email: string
  firstName: string
  lastName: string
  active: boolean
  globalRoles: GlobalRole[]
  createdAt: string
  updatedAt: string
}

export type AuthSession = {
  user: AuthUser
  accessToken: string
  tokenType: string
  expiresIn: number
  appContext: AuthAppContext | null
}

export type LoginCredentials = {
  email: string
  password: string
}
