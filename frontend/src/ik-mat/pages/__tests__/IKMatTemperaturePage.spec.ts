import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

const authStoreMock = vi.hoisted(() => ({
  user: {
    email: 'demo@example.com',
    firstName: 'Demo',
    lastName: 'User',
  },
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/ik-mat/components/TemperatureSparkline.vue', () => ({
  default: {
    template: '<div class="sparkline-stub">Sparkline</div>',
  },
}))

describe('IKMatTemperaturePage mobile editor', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    window.innerWidth = 1024
  })

  it('opens the shared mobile sheet and closes it on escape', async () => {
    window.innerWidth = 700

    const { default: IKMatTemperaturePage } = await import('@/ik-mat/pages/IKMatTemperaturePage.vue')
    const wrapper = mount(IKMatTemperaturePage, {
      attachTo: document.body,
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    })

    const trigger = wrapper.find('button.row-action')
    ;(trigger.element as HTMLButtonElement).focus()
    await trigger.trigger('click')
    await flushPromises()

    const overlayPanel = document.body.querySelector('.app-overlay-panel')
    expect(overlayPanel).not.toBeNull()
    expect(overlayPanel?.getAttribute('aria-label')).toBe('Log temperature reading')

    document.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'Escape' }))
    await flushPromises()

    expect(document.body.querySelector('.app-overlay-panel')).toBeNull()
    expect(document.activeElement).toBe(trigger.element)
  })
})
