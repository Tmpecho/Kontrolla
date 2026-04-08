import type { AuthUser } from '@/auth/model/auth.types'
import { requestJson } from '@/shared/api/http'

type UpdateMyProfileInput = {
  firstName: string
  lastName: string
}

type ChangeMyPasswordInput = {
  currentPassword: string
  newPassword: string
}

export async function updateMyProfile(input: UpdateMyProfileInput): Promise<AuthUser> {
  return requestJson<AuthUser>('/api/v1/auth/me', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export async function changeMyPassword(input: ChangeMyPasswordInput): Promise<void> {
  await requestJson<void>('/api/v1/auth/me/password', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}
