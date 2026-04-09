<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

type OverlayCloseReason = 'backdrop' | 'escape' | 'outside'
type OverlayVariant = 'dialog' | 'drawer-left' | 'drawer-right' | 'popover' | 'sheet-bottom'

const props = withDefaults(
  defineProps<{
    open: boolean
    variant: OverlayVariant
    modal?: boolean
    anchorEl?: HTMLElement | null
    closeOnEscape?: boolean
    closeOnOutside?: boolean
    closeOnBackdrop?: boolean
    ariaLabel?: string
    ariaLabelledby?: string
  }>(),
  {
    modal: undefined,
    anchorEl: null,
    closeOnEscape: true,
    closeOnOutside: undefined,
    closeOnBackdrop: undefined,
    ariaLabel: undefined,
    ariaLabelledby: undefined,
  },
)

const emit = defineEmits<{
  (e: 'close', reason: OverlayCloseReason): void
}>()

const panelRef = ref<HTMLElement | null>(null)
const popoverStyle = ref<Record<string, string>>({})

const isModal = computed(() => props.modal ?? props.variant !== 'popover')
const shouldCloseOnOutside = computed(() => props.closeOnOutside ?? props.variant === 'popover')
const shouldCloseOnBackdrop = computed(() => props.closeOnBackdrop ?? isModal.value)
const showBackdrop = computed(() => isModal.value)
const panelStyle = computed(() => (props.variant === 'popover' ? popoverStyle.value : undefined))

let previousActiveElement: HTMLElement | null = null
let previousBodyOverflow = ''
let panelResizeObserver: ResizeObserver | null = null
let isOverlayActive = false

function emitClose(reason: OverlayCloseReason) {
  emit('close', reason)
}

function isTargetWithinAnchor(target: Node | null) {
  return Boolean(target && props.anchorEl?.contains(target))
}

function getFocusableElements() {
  return (
    panelRef.value?.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
    ) ?? []
  )
}

function focusInitialElement() {
  const focusableElements = getFocusableElements()
  if (focusableElements.length > 0) {
    focusableElements.item(0)?.focus()
    return
  }

  panelRef.value?.focus()
}

function restoreFocus() {
  const nextFocusTarget =
    props.anchorEl && props.anchorEl.isConnected ? props.anchorEl : previousActiveElement

  if (nextFocusTarget?.isConnected) {
    nextFocusTarget.focus()
  }
}

function lockBodyScroll() {
  previousBodyOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
}

function unlockBodyScroll() {
  document.body.style.overflow = previousBodyOverflow
  previousBodyOverflow = ''
}

function trapFocus(event: KeyboardEvent) {
  const overlayElement = panelRef.value

  if (!overlayElement || event.key !== 'Tab') {
    return
  }

  const focusableElements = getFocusableElements()

  if (focusableElements.length === 0) {
    event.preventDefault()
    overlayElement.focus()
    return
  }

  const firstFocusableElement = focusableElements.item(0)
  const lastFocusableElement = focusableElements.item(focusableElements.length - 1)
  const activeElement = document.activeElement
  const focusIsInsideOverlay = activeElement instanceof Node && overlayElement.contains(activeElement)

  if (!firstFocusableElement || !lastFocusableElement) {
    event.preventDefault()
    overlayElement.focus()
    return
  }

  if (!focusIsInsideOverlay) {
    event.preventDefault()
    ;(event.shiftKey ? lastFocusableElement : firstFocusableElement).focus()
    return
  }

  if (event.shiftKey && (activeElement === firstFocusableElement || activeElement === overlayElement)) {
    event.preventDefault()
    lastFocusableElement.focus()
    return
  }

  if (!event.shiftKey && activeElement === lastFocusableElement) {
    event.preventDefault()
    firstFocusableElement.focus()
  }
}

function updatePopoverPosition() {
  if (!props.open || props.variant !== 'popover' || !panelRef.value) {
    return
  }

  const viewportPadding = 8
  const gap = 8
  const anchorRect =
    props.anchorEl?.getBoundingClientRect() ?? {
      top: viewportPadding,
      right: window.innerWidth - viewportPadding,
      bottom: viewportPadding,
      left: viewportPadding,
      width: 0,
      height: 0,
    }
  const panelRect = panelRef.value.getBoundingClientRect()
  const panelWidth = panelRect.width || panelRef.value.offsetWidth || 0
  const panelHeight = panelRect.height || panelRef.value.offsetHeight || 0
  const availableWidth = Math.max(0, window.innerWidth - viewportPadding * 2)
  const availableHeight = Math.max(0, window.innerHeight - viewportPadding * 2)
  const clampedWidth = panelWidth > 0 ? Math.min(panelWidth, availableWidth) : availableWidth

  let left = anchorRect.right - clampedWidth
  left = Math.max(viewportPadding, left)
  left = Math.min(left, window.innerWidth - clampedWidth - viewportPadding)

  let top = anchorRect.bottom + gap
  const popoverBottom = top + panelHeight
  const hasRoomAbove = anchorRect.top - gap - panelHeight >= viewportPadding

  if (popoverBottom > window.innerHeight - viewportPadding && hasRoomAbove) {
    top = anchorRect.top - gap - panelHeight
  }

  top = Math.max(viewportPadding, top)
  top = Math.min(top, window.innerHeight - Math.min(panelHeight, availableHeight) - viewportPadding)

  popoverStyle.value = {
    top: `${Math.round(top)}px`,
    left: `${Math.round(left)}px`,
    maxWidth: `${Math.round(availableWidth)}px`,
    maxHeight: `${Math.round(availableHeight)}px`,
  }
}

function stopObservingPanel() {
  panelResizeObserver?.disconnect()
  panelResizeObserver = null
}

function startObservingPanel() {
  if (props.variant !== 'popover' || typeof ResizeObserver === 'undefined' || !panelRef.value) {
    return
  }

  panelResizeObserver = new ResizeObserver(() => updatePopoverPosition())
  panelResizeObserver.observe(panelRef.value)

  if (props.anchorEl) {
    panelResizeObserver.observe(props.anchorEl)
  }
}

function handleDocumentMouseDown(event: MouseEvent) {
  if (!props.open || !shouldCloseOnOutside.value) {
    return
  }

  const target = event.target as Node | null

  if (panelRef.value?.contains(target) || isTargetWithinAnchor(target)) {
    return
  }

  emitClose('outside')
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (!props.open) {
    return
  }

  if (event.key === 'Escape' && props.closeOnEscape) {
    emitClose('escape')
    return
  }

  if (isModal.value) {
    trapFocus(event)
  }
}

function handleWindowLayoutChange() {
  updatePopoverPosition()
}

function addOverlayListeners() {
  document.addEventListener('keydown', handleDocumentKeydown)
  document.addEventListener('mousedown', handleDocumentMouseDown)
  window.addEventListener('resize', handleWindowLayoutChange)
  window.addEventListener('scroll', handleWindowLayoutChange, true)
}

function removeOverlayListeners() {
  document.removeEventListener('keydown', handleDocumentKeydown)
  document.removeEventListener('mousedown', handleDocumentMouseDown)
  window.removeEventListener('resize', handleWindowLayoutChange)
  window.removeEventListener('scroll', handleWindowLayoutChange, true)
}

function handleBackdropClick() {
  if (shouldCloseOnBackdrop.value) {
    emitClose('backdrop')
  }
}

watch(
  () => props.open,
  async (isOpen) => {
    if (isOpen) {
      isOverlayActive = true
      previousActiveElement =
        document.activeElement instanceof HTMLElement ? document.activeElement : null

      if (isModal.value) {
        lockBodyScroll()
      }

      addOverlayListeners()
      await nextTick()
      updatePopoverPosition()
      startObservingPanel()
      focusInitialElement()
      return
    }

    if (!isOverlayActive) {
      return
    }

    isOverlayActive = false
    stopObservingPanel()
    removeOverlayListeners()

    if (isModal.value) {
      unlockBodyScroll()
    }

    restoreFocus()
  },
  { immediate: true },
)

watch(
  () => props.anchorEl,
  async () => {
    if (!props.open || props.variant !== 'popover') {
      return
    }

    await nextTick()
    updatePopoverPosition()
  },
)

onBeforeUnmount(() => {
  stopObservingPanel()
  removeOverlayListeners()

  if (props.open && isModal.value) {
    unlockBodyScroll()
  }
})
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="app-overlay" :data-modal="isModal" :data-variant="variant">
      <div
        v-if="showBackdrop"
        class="app-overlay-backdrop"
        aria-hidden="true"
        @click="handleBackdropClick"
      />

      <div
        ref="panelRef"
        class="app-overlay-panel"
        :data-variant="variant"
        :style="panelStyle"
        role="dialog"
        :aria-modal="isModal || undefined"
        :aria-label="ariaLabel"
        :aria-labelledby="ariaLabelledby"
        tabindex="-1"
        @click.stop
      >
        <slot />
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.app-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  pointer-events: none;
}

.app-overlay-backdrop {
  position: absolute;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.32);
  pointer-events: auto;
}

.app-overlay-panel {
  position: fixed;
  pointer-events: auto;
  outline: none;
}

.app-overlay-panel[data-variant='popover'] {
  min-width: 240px;
  overflow: hidden auto;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
  box-shadow: var(--shadow-elevated);
}

.app-overlay-panel[data-variant='dialog'] {
  top: 50%;
  left: 50%;
  width: min(100vw - 32px, 560px);
  max-height: calc(100vh - 32px);
  overflow: auto;
  transform: translate(-50%, -50%);
}

.app-overlay-panel[data-variant='drawer-left'] {
  top: 0;
  left: 0;
  bottom: 0;
  width: min(86vw, 320px);
  overflow-y: auto;
}

.app-overlay-panel[data-variant='drawer-right'] {
  top: 0;
  right: 0;
  bottom: 0;
  width: min(100vw, 420px);
  overflow-y: auto;
}

.app-overlay-panel[data-variant='sheet-bottom'] {
  right: 0;
  bottom: 0;
  left: 0;
  max-height: calc(100vh - 16px);
  overflow-y: auto;
}
</style>
