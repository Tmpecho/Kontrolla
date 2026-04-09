import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DeviationDetailPanel from '@/deviations/components/DeviationDetailPanel.vue'
import type { DeviationListItem } from '@/deviations/model/deviation.types'

function createDeviation(overrides: Partial<DeviationListItem> = {}): DeviationListItem {
  return {
    id: 'dev-1',
    establishmentId: 'est-1',
    serviceArea: 'IK_MAT',
    title: 'Walk-in fridge too warm',
    reportedAt: '2026-04-06T08:00:00Z',
    category: 'Temperature',
    severity: 'HIGH',
    status: 'OPEN',
    assignedToUserId: null,
    assignedTo: [],
    description: 'Opening check measured 10C.',
    timeline: [
      {
        id: 'evt-1',
        createdAt: '2026-04-06T08:00:00Z',
        authorName: 'Reporter User',
        note: 'Deviation reported.',
      },
    ],
    ...overrides,
  }
}

describe('DeviationDetailPanel', () => {
  it('emits a trimmed add-note event', async () => {
    const wrapper = mount(DeviationDetailPanel, {
      props: {
        deviation: createDeviation(),
        memberOptions: [],
      },
    })

    await wrapper.get('#deviation-timeline-note').setValue('   Follow-up completed.   ')
    const addNoteButton = wrapper.findAll('button').find((candidate) => candidate.text() === 'Add note')
    await addNoteButton?.trigger('click')

    expect(wrapper.emitted('add-note')).toEqual([['Follow-up completed.']])
  })

  it('disables note submission when the textarea is blank', async () => {
    const wrapper = mount(DeviationDetailPanel, {
      props: {
        deviation: createDeviation(),
        memberOptions: [],
      },
    })

    const addNoteButton = wrapper.findAll('button').find((candidate) => candidate.text() === 'Add note')

    expect(addNoteButton?.attributes('disabled')).toBeDefined()
    expect(wrapper.emitted('add-note')).toBeUndefined()
  })
})
