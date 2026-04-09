export type OrganizationStatus = 'ACTIVE' | 'INACTIVE'

export type OrganizationSummary = {
  id: string
  name: string
  status: OrganizationStatus
  createdAt: string
  updatedAt: string
}
