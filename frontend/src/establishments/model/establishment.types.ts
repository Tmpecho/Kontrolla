export type EstablishmentStatus = 'ACTIVE' | 'INACTIVE'

export type EstablishmentType =
  | 'RESTAURANT'
  | 'BAR'
  | 'CAFE'
  | 'OTHER'

export type Establishment = {
  id: string
  organizationId: string
  name: string
  type: EstablishmentType
  status: EstablishmentStatus
  createdAt: string
  updatedAt: string
}
