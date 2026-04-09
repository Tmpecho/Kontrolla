import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'

const { authStoreMock, notificationsStoreMock } = vi.hoisted(() => ({
  authStoreMock: {
    isAuthenticated: true,
  },
  notificationsStoreMock: {
    startPolling: vi.fn(),
    stopPolling: vi.fn(),
    reset: vi.fn(),
  },
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/notifications/model/notifications.store', () => ({
  useNotificationsStore: () => notificationsStoreMock,
}))

vi.mock('@/app/components/TopBar.vue', () => ({
  default: defineComponent({
    props: {
      mobileNavOpen: {
        type: Boolean,
        default: false,
      },
    },
    emits: ['toggle-mobile-nav'],
    template: `
      <button
        id="mobile-nav-trigger"
        type="button"
        :data-open="mobileNavOpen"
        @click="$emit('toggle-mobile-nav')"
      >
        Toggle nav
      </button>
    `,
  }),
}))

vi.mock('@/app/components/Sidebar.vue', () => ({
  default: defineComponent({
    props: {
      variant: {
        type: String,
        default: 'desktop',
      },
    },
    emits: ['navigate'],
    template: `
      <div class="sidebar-stub" :data-variant="variant">
        <button id="first-nav-action" type="button">First action</button>
        <button id="last-nav-action" type="button" @click="$emit('navigate')">Navigate</button>
      </div>
    `,
  }),
}))

async function mountLayout() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: { template: '<div>Home</div>' } }],
  })
  router.push('/')
  await router.isReady()

  const { default: AppLayout } = await import('@/app/layouts/AppLayout.vue')
  return mount(AppLayout, {
    attachTo: document.body,
    global: {
      plugins: [router],
      stubs: {
        RouterView: {
          template: '<div class="router-view-stub">Page content</div>',
        },
      },
    },
  })
}

describe('AppLayout mobile navigation overlay', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    notificationsStoreMock.startPolling.mockReset()
    notificationsStoreMock.stopPolling.mockReset()
    notificationsStoreMock.reset.mockReset()
  })

  it('traps focus inside the mobile navigation drawer and closes on escape', async () => {
    const wrapper = await mountLayout()

    await wrapper.get('#mobile-nav-trigger').trigger('click')
    await flushPromises()

    const firstAction = document.body.querySelectorAll('#first-nav-action').item(1) as HTMLButtonElement
    const lastAction = document.body.querySelectorAll('#last-nav-action').item(1) as HTMLButtonElement

    expect(firstAction).not.toBeNull()
    expect(document.activeElement).toBe(firstAction)

    lastAction.focus()
    document.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'Tab' }))
    expect(document.activeElement).toBe(firstAction)

    document.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'Escape' }))
    await flushPromises()

    expect(document.body.querySelectorAll('#first-nav-action')).toHaveLength(1)
  })
})
