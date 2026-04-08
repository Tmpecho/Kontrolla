import { requestJson } from '@/shared/api/http'
import type {
  DeviationCategoryValue,
  DeviationListItem,
  DeviationMemberOption,
  DeviationSeverity,
  DeviationStatus,
} from '@/deviations/model/deviation.types'
import {
  getDeviationServiceAreaForCategory,
  toDeviationCategoryLabel,
} from '@/deviations/model/deviation.types'

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type DeviationResponse = {
  id: string
  organizationId: string
  establishmentId: string
  createdByUserId: string
  assignedToUserId: string | null
  title: string
  description: string
  status: DeviationStatus
  severity: DeviationSeverity
  category: DeviationCategoryValue
  createdAt: string
  updatedAt: string
}

export type DeviationTimelineEntryResponse = {
  id: string
  eventType: 'REPORTED' | 'ASSIGNED' | 'UNASSIGNED' | 'STATUS_CHANGED' | 'DETAILS_UPDATED' | 'NOTE_ADDED'
  actorUserId: string | null
  authorName: string
  note: string
  occurredAt: string
}

export type DeviationDetailsResponse = DeviationResponse & {
  timeline: DeviationTimelineEntryResponse[]
}

export type OrganizationMemberResponse = {
  id: string
  userId: string
  userEmail: string
  userFirstName: string
  userLastName: string
  role: string
  active: boolean
  allEstablishments: boolean
  establishments: Array<{
    id: string
    name: string
  }>
  createdAt: string
  updatedAt: string
}

type EstablishmentDeviationQuery = {
  organizationId: string
  establishmentId: string
  page?: number
  size?: number
}

type DeviationMutationTarget = {
  organizationId: string
  establishmentId: string
  deviationId: string
}

export async function listEstablishmentDeviations(
  params: EstablishmentDeviationQuery,
): Promise<PageResponse<DeviationResponse>> {
  return requestJson<PageResponse<DeviationResponse>>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/deviations`,
    {
      query: {
        page: params.page,
        size: params.size,
      },
    },
  )
}

export async function createDeviation(
  params: Pick<EstablishmentDeviationQuery, 'organizationId' | 'establishmentId'> & {
    title: string
    description: string
    category: DeviationCategoryValue
    severity: DeviationSeverity
  },
): Promise<DeviationDetailsResponse> {
  return requestJson<DeviationDetailsResponse>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/deviations`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        title: params.title,
        description: params.description,
        category: params.category,
        severity: params.severity,
      }),
    },
  )
}

export async function getDeviation(params: DeviationMutationTarget): Promise<DeviationDetailsResponse> {
  return requestJson<DeviationDetailsResponse>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/deviations/${params.deviationId}`,
  )
}

export async function updateDeviationDetails(
  params: DeviationMutationTarget & {
    title: string
    description: string
    category: DeviationCategoryValue
    severity: DeviationSeverity
  },
): Promise<DeviationDetailsResponse> {
  return requestJson<DeviationDetailsResponse>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/deviations/${params.deviationId}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        title: params.title,
        description: params.description,
        category: params.category,
        severity: params.severity,
      }),
    },
  )
}

export async function updateDeviationStatus(
  params: DeviationMutationTarget & {
    status: DeviationStatus
  },
): Promise<DeviationDetailsResponse> {
  return requestJson<DeviationDetailsResponse>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/deviations/${params.deviationId}/status`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        status: params.status,
      }),
    },
  )
}

export async function assignDeviation(
  params: DeviationMutationTarget & {
    assignedUserId: string
  },
): Promise<DeviationDetailsResponse> {
  return requestJson<DeviationDetailsResponse>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/deviations/${params.deviationId}/assignment`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        assignedUserId: params.assignedUserId,
      }),
    },
  )
}

export async function addDeviationTimelineNote(
  params: DeviationMutationTarget & {
    note: string
  },
): Promise<DeviationDetailsResponse> {
  return requestJson<DeviationDetailsResponse>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/deviations/${params.deviationId}/timeline`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        note: params.note,
      }),
    },
  )
}

export async function listOrganizationMembers(params: {
  organizationId: string
  establishmentId?: string
  includeInactive?: boolean
  page?: number
  size?: number
}): Promise<PageResponse<OrganizationMemberResponse>> {
  return requestJson<PageResponse<OrganizationMemberResponse>>(
    `/api/v1/organizations/${params.organizationId}/members`,
    {
      query: {
        establishmentId: params.establishmentId,
        includeInactive: params.includeInactive,
        page: params.page,
        size: params.size,
      },
    },
  )
}

export function buildMemberDisplayName(member: OrganizationMemberResponse): string {
  const fullName = `${member.userFirstName} ${member.userLastName}`.trim()
  return fullName || member.userEmail
}

export function toMemberOptions(
  members: OrganizationMemberResponse[],
): DeviationMemberOption[] {
  return members
    .filter((member) => member.active)
    .map((member) => ({
      userId: member.userId,
      displayName: buildMemberDisplayName(member),
    }))
}

export function toMemberNameLookup(
  members: OrganizationMemberResponse[],
): Record<string, string> {
  return Object.fromEntries(members.map((member) => [member.userId, buildMemberDisplayName(member)]))
}

function fallbackUserLabel(userId: string): string {
  return `User ${userId.slice(0, 8)}`
}

export function mapDeviationResponseToListItem(
  deviation: DeviationResponse | DeviationDetailsResponse,
  memberNamesById: Record<string, string>,
): DeviationListItem {
  const categoryLabel = toDeviationCategoryLabel(deviation.category)
  const assignedToDisplayName = deviation.assignedToUserId
    ? memberNamesById[deviation.assignedToUserId] ?? fallbackUserLabel(deviation.assignedToUserId)
    : null
  const authorName =
    memberNamesById[deviation.createdByUserId] ?? fallbackUserLabel(deviation.createdByUserId)

  return {
    id: deviation.id,
    serviceArea: getDeviationServiceAreaForCategory(categoryLabel),
    title: deviation.title,
    reportedAt: deviation.createdAt,
    category: categoryLabel,
    severity: deviation.severity,
    status: deviation.status,
    assignedToUserId: deviation.assignedToUserId,
    assignedTo: assignedToDisplayName ? [assignedToDisplayName] : [],
    description: deviation.description,
    timeline:
      'timeline' in deviation
        ? deviation.timeline.map((entry) => ({
            id: entry.id,
            createdAt: entry.occurredAt,
            authorName: entry.authorName || (entry.actorUserId ? memberNamesById[entry.actorUserId] ?? fallbackUserLabel(entry.actorUserId) : 'System'),
            note: entry.note,
          }))
        : [
            {
              id: `${deviation.id}-reported`,
              createdAt: deviation.createdAt,
              authorName,
              note: 'Deviation reported.',
            },
          ],
  }
}
