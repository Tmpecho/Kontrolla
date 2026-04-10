export type ServingHoursDayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export type ServingHoursDay = {
  dayOfWeek: ServingHoursDayOfWeek
  closed: boolean
  opensAt: string | null
  closesAt: string | null
}
