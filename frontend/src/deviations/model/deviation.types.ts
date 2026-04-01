export type DeviationServiceArea = 'IK_MAT' | 'IK_ALKOHOL'

export type DeviationStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED'

export type DeviationSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export type DeviationCategory =
  | 'Temperature'
  | 'Cleaning and hygiene'
  | 'Allergen handling'
  | 'Storage and labeling'
  | 'Age control'
  | 'Intoxicated guest'
  | 'Serving hours'
  | 'Documentation and training'

export type DeviationTimelineEntry = {
  id: string
  createdAt: string
  authorName: string
  note: string
}

export type DeviationListItem = {
  id: string
  serviceArea: DeviationServiceArea
  title: string
  reportedAt: string
  category: DeviationCategory
  severity: DeviationSeverity
  status: DeviationStatus
  assignedTo: string[]
  description: string
  timeline: DeviationTimelineEntry[]
}
