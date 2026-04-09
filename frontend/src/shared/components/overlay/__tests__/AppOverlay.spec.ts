import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick, ref } from 'vue'

import AppOverlay from '@/shared/components/overlay/AppOverlay.vue'

function dispatchKeyboardEvent(key: string, options: KeyboardEventInit = {}) {
  document.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key, ...options }))
}

describe('AppOverlay', () => {
  const originalBodyOverflow = document.body.style.overflow
  const originalInnerWidth = window.innerWidth
  const originalInnerHeight = window.innerHeight

  beforeEach(() => {
    document.body.innerHTML = ''
    document.body.style.overflow = originalBodyOverflow
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: originalInnerWidth })
    Object.defineProperty(window, 'innerHeight', { configurable: true, value: originalInnerHeight })
  })

  afterEach(() => {
    document.body.innerHTML = ''
    document.body.style.overflow = originalBodyOverflow
  })

  it('emits close when escape is pressed', async () => {
    const wrapper = mount(AppOverlay, {
      attachTo: document.body,
      props: {
        open: true,
        variant: 'dialog',
        ariaLabel: 'Example dialog',
      },
      slots: {
        default: '<button type="button">Action</button>',
      },
    })

    await nextTick()
    dispatchKeyboardEvent('Escape')

    expect(wrapper.emitted('close')).toEqual([['escape']])
  })

  it('emits close when the backdrop is clicked', async () => {
    const wrapper = mount(AppOverlay, {
      attachTo: document.body,
      props: {
        open: true,
        variant: 'dialog',
        ariaLabel: 'Example dialog',
      },
      slots: {
        default: '<div>Dialog body</div>',
      },
    })

    await nextTick()
    const backdrop = document.body.querySelector('.app-overlay-backdrop') as HTMLDivElement
    backdrop.click()

    expect(wrapper.emitted('close')).toEqual([['backdrop']])
  })

  it('emits close when clicking outside a popover', async () => {
    const anchorEl = document.createElement('button')
    document.body.appendChild(anchorEl)

    const wrapper = mount(AppOverlay, {
      attachTo: document.body,
      props: {
        open: true,
        variant: 'popover',
        anchorEl,
        ariaLabel: 'Example popover',
      },
      slots: {
        default: '<button type="button">First action</button>',
      },
    })

    await nextTick()
    document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }))

    expect(wrapper.emitted('close')).toEqual([['outside']])
  })

  it('traps focus for modal variants', async () => {
    const wrapper = mount(AppOverlay, {
      attachTo: document.body,
      props: {
        open: true,
        variant: 'dialog',
        ariaLabel: 'Focusable dialog',
      },
      slots: {
        default: `
          <div>
            <button type="button" id="first-action">First</button>
            <button type="button" id="last-action">Last</button>
          </div>
        `,
      },
    })

    await nextTick()

    const firstAction = document.body.querySelector('#first-action') as HTMLButtonElement
    const lastAction = document.body.querySelector('#last-action') as HTMLButtonElement

    expect(document.activeElement).toBe(firstAction)

    lastAction.focus()
    dispatchKeyboardEvent('Tab')
    expect(document.activeElement).toBe(firstAction)

    firstAction.focus()
    dispatchKeyboardEvent('Tab', { shiftKey: true })
    expect(document.activeElement).toBe(lastAction)

    wrapper.unmount()
  })

  it('restores focus to the previously active element after close', async () => {
    const HostComponent = defineComponent({
      components: { AppOverlay },
      setup() {
        return {
          open: ref(false),
        }
      },
      template: `
        <div>
          <button id="trigger" type="button">Open overlay</button>
          <AppOverlay :open="open" aria-label="Managed dialog" variant="dialog">
            <button type="button">Action</button>
          </AppOverlay>
        </div>
      `,
    })

    const wrapper = mount(HostComponent, {
      attachTo: document.body,
    })

    const trigger = wrapper.get('#trigger').element as HTMLButtonElement
    trigger.focus()

    ;(wrapper.vm as { open: boolean }).open = true
    await nextTick()

    ;(wrapper.vm as { open: boolean }).open = false
    await nextTick()

    expect(document.activeElement).toBe(trigger)
  })

  it('locks body scroll only for modal overlays', async () => {
    const HostComponent = defineComponent({
      components: { AppOverlay },
      setup() {
        return {
          open: ref(false),
        }
      },
      template: `
        <AppOverlay :open="open" aria-label="Managed dialog" variant="dialog">
          <button type="button">Action</button>
        </AppOverlay>
      `,
    })

    document.body.style.overflow = 'scroll'

    const modalWrapper = mount(HostComponent, {
      attachTo: document.body,
    })

    ;(modalWrapper.vm as { open: boolean }).open = true
    await nextTick()
    expect(document.body.style.overflow).toBe('hidden')

    ;(modalWrapper.vm as { open: boolean }).open = false
    await nextTick()
    expect(document.body.style.overflow).toBe('scroll')

    const popoverWrapper = mount(AppOverlay, {
      attachTo: document.body,
      props: {
        open: true,
        variant: 'popover',
        ariaLabel: 'Passive popover',
      },
      slots: {
        default: '<div>Popover</div>',
      },
    })

    await nextTick()
    expect(document.body.style.overflow).toBe('scroll')

    popoverWrapper.unmount()
  })

  it('positions popovers from their anchor element', async () => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 400 })
    Object.defineProperty(window, 'innerHeight', { configurable: true, value: 300 })

    const anchorEl = document.createElement('button')
    anchorEl.getBoundingClientRect = vi.fn(() => ({
      width: 48,
      height: 32,
      top: 16,
      right: 210,
      bottom: 48,
      left: 162,
      x: 162,
      y: 16,
      toJSON: () => ({}),
    }))
    document.body.appendChild(anchorEl)

    const wrapper = mount(AppOverlay, {
      attachTo: document.body,
      props: {
        open: true,
        variant: 'popover',
        anchorEl,
        ariaLabel: 'Anchored popover',
      },
      slots: {
        default: '<div>Popover body</div>',
      },
    })

    await nextTick()

    const panel = document.body.querySelector('.app-overlay-panel') as HTMLDivElement
    panel.getBoundingClientRect = vi.fn(() => ({
      width: 150,
      height: 120,
      top: 0,
      right: 150,
      bottom: 120,
      left: 0,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    }))

    window.dispatchEvent(new Event('resize'))
    await nextTick()

    expect(panel.style.top).toBe('56px')
    expect(panel.style.left).toBe('60px')

    wrapper.unmount()
  })
})
