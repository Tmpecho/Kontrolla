import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'

const { authStoreMock, notificationsStoreMock } = vi.hoisted(() => ({
  authStoreMock: {
    isAuthenticated: true,
    isStartupPending: false,
    startupStatus: 'ready' as 'idle' | 'waiting-for-backend' | 'bootstrapping-workspace' | 'ready' | 'error',
    startupError: null as string | null,
    startupStartedAt: null as number | null,
    retryWorkspaceStartup: vi.fn(),
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
        <button :data-testid="\`\${variant}-first-nav-action\`" type="button">First action</button>
        <button :data-testid="\`\${variant}-last-nav-action\`" type="button" @click="$emit('navigate')">Navigate</button>
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
    authStoreMock.isStartupPending = false
    authStoreMock.startupStatus = 'ready'
    authStoreMock.startupError = null
    authStoreMock.startupStartedAt = null
    authStoreMock.retryWorkspaceStartup.mockReset()
  })

  it('traps focus inside the mobile navigation drawer and closes on escape', async () => {
    const wrapper = await mountLayout()

    const trigger = wrapper.get('#mobile-nav-trigger')
    ;(trigger.element as HTMLButtonElement).focus()
    await trigger.trigger('click')
    await flushPromises()

    const firstAction = document.body.querySelector(
      '[data-testid="mobile-first-nav-action"]',
    ) as HTMLButtonElement
    const lastAction = document.body.querySelector(
      '[data-testid="mobile-last-nav-action"]',
    ) as HTMLButtonElement

    expect(firstAction).not.toBeNull()
    expect(document.activeElement).toBe(firstAction)

    lastAction.focus()
    document.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'Tab' }))
    expect(document.activeElement).toBe(firstAction)

    document.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'Escape' }))
    await flushPromises()

    expect(document.body.querySelector('[data-testid="mobile-first-nav-action"]')).toBeNull()
    expect(document.body.querySelector('[data-testid="desktop-first-nav-action"]')).not.toBeNull()
    expect(document.activeElement).toBe(trigger.element)
  })

  it('renders the startup shell instead of route content while startup is pending', async () => {
    authStoreMock.isStartupPending = true
    authStoreMock.startupStatus = 'waiting-for-backend'
    authStoreMock.startupStartedAt = Date.now() - 91_000

    const wrapper = await mountLayout()
    await flushPromises()

    expect(wrapper.text()).toContain('Starting workspace...')
    expect(wrapper.text()).toContain('Retry now')
    expect(wrapper.text()).not.toContain('Page content')
    expect(notificationsStoreMock.startPolling).not.toHaveBeenCalled()
  })

  it('renders the startup error state and forwards the retry action', async () => {
    authStoreMock.startupStatus = 'error'
    authStoreMock.startupError = 'Backend unavailable'

    const wrapper = await mountLayout()
    await flushPromises()

    expect(wrapper.text()).toContain('Unable to start workspace')
    expect(wrapper.text()).toContain('Backend unavailable')

    await wrapper.get('.startup-state__retry').trigger('click')

    expect(authStoreMock.retryWorkspaceStartup).toHaveBeenCalledTimes(1)
  })
})
