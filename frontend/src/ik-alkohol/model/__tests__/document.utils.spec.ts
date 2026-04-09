import { describe, expect, it, vi } from 'vitest'

import {
  expiryWarningDays,
  formatDocumentStatus,
  getDocumentsWithStatus,
  getDocumentStatus,
  parseLocalDate,
} from '@/ik-alkohol/model/document.utils'
import type { ImportantDocumentRecord } from '@/ik-alkohol/model/document.types'

function createDocumentRecord(id: string, renewalDate: string): ImportantDocumentRecord {
  return {
    id,
    title: `${id} title`,
    holderName: `${id} holder`,
    issueDate: '2026-01-01',
    renewalDate,
    auditAssignments: [],
  }
}

describe('document.utils', () => {
  it('parses date-only strings as local dates', () => {
    const parsedDate = parseLocalDate('2026-04-01')

    expect(parsedDate.getFullYear()).toBe(2026)
    expect(parsedDate.getMonth()).toBe(3)
    expect(parsedDate.getDate()).toBe(1)
    expect(parsedDate.getHours()).toBe(0)
  })

  it('classifies expired, expiring, and valid documents at the warning boundary', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 3, 1, 12, 0, 0))

    expect(getDocumentStatus(createDocumentRecord('expired', '2026-03-31'))).toBe('EXPIRED')
    expect(getDocumentStatus(createDocumentRecord('expiring-today', '2026-04-01'))).toBe('EXPIRING')
    expect(
      getDocumentStatus(
        createDocumentRecord('expiring-cutoff', '2026-05-01'),
        expiryWarningDays,
      ),
    ).toBe('EXPIRING')
    expect(
      getDocumentStatus(createDocumentRecord('valid-after-cutoff', '2026-05-02'), expiryWarningDays),
    ).toBe('VALID')

    vi.useRealTimers()
  })

  it('sorts documents by the earliest renewal date first', () => {
    const documents = [
      createDocumentRecord('latest', '2026-06-15'),
      createDocumentRecord('earliest', '2026-04-02'),
      createDocumentRecord('middle', '2026-05-10'),
    ]

    const documentsWithStatus = getDocumentsWithStatus(documents)

    expect(documentsWithStatus.map((documentRecord) => documentRecord.id)).toEqual([
      'earliest',
      'middle',
      'latest',
    ])
  })

  it('formats the status labels used by the UI', () => {
    expect(formatDocumentStatus('VALID')).toBe('Valid')
    expect(formatDocumentStatus('EXPIRING')).toBe('Expiring')
    expect(formatDocumentStatus('EXPIRED')).toBe('Expired')
  })
})
