export type DeviationServiceArea = 'IK_MAT' | 'IK_ALKOHOL'

export type DeviationStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED'

export type DeviationSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export type DeviationCategoryValue =
  | 'TEMPERATURE'
  | 'HYGIENE'
  | 'ALLERGEN'
  | 'STORAGE'
  | 'AGE_CONTROL'
  | 'INAPPROPRIATE_BEHAVIOUR'
  | 'SERVING_HOURS'
  | 'DOCUMENTATION_AND_TRAINING'

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

export type DeviationMemberOption = {
  userId: string
  displayName: string
}

export type DeviationSaveInput = {
  title: string
  category: DeviationCategory
  severity: DeviationSeverity
  status: DeviationStatus
  assignedToUserId: string | null
  description: string
}

export type DeviationListItem = {
  id: string
  serviceArea: DeviationServiceArea
  title: string
  reportedAt: string
  category: DeviationCategory
  severity: DeviationSeverity
  status: DeviationStatus
  assignedToUserId: string | null
  assignedTo: string[]
  description: string
  timeline: DeviationTimelineEntry[]
}

export const deviationCategoryValueByLabel: Record<DeviationCategory, DeviationCategoryValue> = {
  Temperature: 'TEMPERATURE',
  'Cleaning and hygiene': 'HYGIENE',
  'Allergen handling': 'ALLERGEN',
  'Storage and labeling': 'STORAGE',
  'Age control': 'AGE_CONTROL',
  'Intoxicated guest': 'INAPPROPRIATE_BEHAVIOUR',
  'Serving hours': 'SERVING_HOURS',
  'Documentation and training': 'DOCUMENTATION_AND_TRAINING',
}

export const deviationCategoryLabelByValue: Record<DeviationCategoryValue, DeviationCategory> = {
  TEMPERATURE: 'Temperature',
  HYGIENE: 'Cleaning and hygiene',
  ALLERGEN: 'Allergen handling',
  STORAGE: 'Storage and labeling',
  AGE_CONTROL: 'Age control',
  INAPPROPRIATE_BEHAVIOUR: 'Intoxicated guest',
  SERVING_HOURS: 'Serving hours',
  DOCUMENTATION_AND_TRAINING: 'Documentation and training',
}

export const deviationCategoriesByServiceArea: Record<DeviationServiceArea, DeviationCategory[]> = {
  IK_MAT: [
    'Temperature',
    'Cleaning and hygiene',
    'Allergen handling',
    'Storage and labeling',
  ],
  IK_ALKOHOL: [
    'Age control',
    'Intoxicated guest',
    'Serving hours',
    'Documentation and training',
  ],
}

export function toDeviationCategoryLabel(value: DeviationCategoryValue): DeviationCategory {
  return deviationCategoryLabelByValue[value]
}

export function toDeviationCategoryValue(label: DeviationCategory): DeviationCategoryValue {
  return deviationCategoryValueByLabel[label]
}

export function getDeviationServiceAreaForCategory(
  category: DeviationCategory,
): DeviationServiceArea {
  return deviationCategoriesByServiceArea.IK_MAT.includes(category) ? 'IK_MAT' : 'IK_ALKOHOL'
}

export function formatDeviationStatus(status: DeviationStatus): string {
  return status.toLowerCase().replace('_', ' ')
}

export function formatDeviationSeverity(severity: DeviationSeverity): string {
  return severity.toLowerCase()
}
