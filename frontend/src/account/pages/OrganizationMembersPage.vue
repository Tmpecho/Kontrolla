<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import {
  createManagedOrganizationMember,
  createOrganizationMember,
  listOrganizationMembers,
  updateOrganizationMember,
} from '@/account/api/organization-members.api'
import type {
  ManagedOrganizationMemberProvision,
  OrganizationMembership,
  OrganizationRole,
} from '@/account/model/organization-members.types'
import { useAuthStore } from '@/auth/model/auth.store'
import { appEnv } from '@/shared/config/env'
import { ApiError } from '@/shared/api/http'

type EditableMembership = OrganizationMembership & {
  draftRole: OrganizationRole
  draftActive: boolean
}

type MemberProvisionMode = 'existing_user' | 'new_member'

type MemberProvisionDraft = {
  mode: MemberProvisionMode
  existingUserId: string
  firstName: string
  lastName: string
  email: string
  role: OrganizationRole
  active: boolean
}

const authStore = useAuthStore()

const organizationRoles: Array<{ value: OrganizationRole; label: string; description: string }> = [
  {
    value: 'ORG_OWNER',
    label: 'Owner',
    description: 'Full organization control, including member management.',
  },
  {
    value: 'ORG_ADMIN',
    label: 'Admin',
    description: 'Can manage members and operational setup.',
  },
  {
    value: 'ORG_MANAGER',
    label: 'Manager',
    description: 'Can run operations and manage establishments, but not members.',
  },
  {
    value: 'ORG_EMPLOYEE',
    label: 'Employee',
    description: 'Operational access for assigned work.',
  },
]

const members = ref<EditableMembership[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const isCreatingMember = ref(false)
const savingMembershipId = ref<string | null>(null)
const isCreateComposerOpen = ref(false)
const latestInvite = ref<ManagedOrganizationMemberProvision | null>(null)
const showInactiveMembers = ref(false)
const attemptedCreateSubmit = ref(false)
const createDraft = ref<MemberProvisionDraft>({
  mode: 'existing_user',
  existingUserId: '',
  firstName: '',
  lastName: '',
  email: '',
  role: 'ORG_EMPLOYEE',
  active: true,
})

const resolvedOrganizationId = computed(() => {
  return authStore.appContext?.organizationId ?? appEnv.defaultOrganizationId ?? null
})

const resolvedOrganizationName = computed(() => {
  return authStore.appContext?.organizationName ?? 'Current organization'
})
const resolvedEstablishmentId = computed(() => authStore.appContext?.establishmentId ?? null)

const currentUserId = computed(() => authStore.user?.id ?? null)
const canManageMembers = computed(() => {
  if (authStore.user?.globalRoles.includes('PLATFORM_ADMIN')) {
    return true
  }

  return (
    authStore.appContext?.organizationRole === 'ORG_OWNER' ||
    authStore.appContext?.organizationRole === 'ORG_ADMIN'
  )
})
const totalMembers = computed(() => members.value.length)
const activeMembers = computed(() => members.value.filter((member) => member.active).length)
const inactiveMembers = computed(() => totalMembers.value - activeMembers.value)
const memberSummary = computed(() => {
  if (showInactiveMembers.value) {
    return `${activeMembers.value} active, ${inactiveMembers.value} inactive`
  }

  return `${activeMembers.value} active members`
})

const canSubmitNewMember = computed(() => {
  if (createDraft.value.mode === 'existing_user') {
    return createDraft.value.existingUserId.trim().length > 0
  }

  return (
    createDraft.value.firstName.trim().length > 0 &&
    createDraft.value.lastName.trim().length > 0 &&
    createDraft.value.email.trim().length > 0
  )
})

const existingUserIdError = computed(() => {
  if (!attemptedCreateSubmit.value || createDraft.value.mode !== 'existing_user') {
    return null
  }

  return createDraft.value.existingUserId.trim() ? null : 'Enter an existing user ID.'
})

const inviteFirstNameError = computed(() => {
  if (!attemptedCreateSubmit.value || createDraft.value.mode !== 'new_member') {
    return null
  }

  return createDraft.value.firstName.trim() ? null : 'Enter the first name.'
})

const inviteLastNameError = computed(() => {
  if (!attemptedCreateSubmit.value || createDraft.value.mode !== 'new_member') {
    return null
  }

  return createDraft.value.lastName.trim() ? null : 'Enter the last name.'
})

const inviteEmailError = computed(() => {
  if (!attemptedCreateSubmit.value || createDraft.value.mode !== 'new_member') {
    return null
  }

  return createDraft.value.email.trim() ? null : 'Enter the email address.'
})

function toEditableMembership(member: OrganizationMembership): EditableMembership {
  return {
    ...member,
    draftRole: member.role,
    draftActive: member.active,
  }
}

function getFullName(member: OrganizationMembership): string {
  return `${member.userFirstName} ${member.userLastName}`.trim()
}

function hasDraftChanges(member: EditableMembership): boolean {
  return member.role !== member.draftRole || member.active !== member.draftActive
}

function isCurrentUserMembership(member: OrganizationMembership): boolean {
  return currentUserId.value === member.userId
}

async function loadMembers(): Promise<void> {
  const organizationId = resolvedOrganizationId.value

  if (!organizationId) {
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    if (!canManageMembers.value) {
      members.value = []
      errorMessage.value = 'Only organization owners and admins can manage members.'
      return
    }

    const page = await listOrganizationMembers({
      organizationId,
      establishmentId: resolvedEstablishmentId.value ?? undefined,
      includeInactive: showInactiveMembers.value,
      size: 100,
    })

    members.value = page.items
      .sort((a, b) => a.userFirstName.localeCompare(b.userFirstName))
      .map(toEditableMembership)
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load organization members.'
  } finally {
    isLoading.value = false
  }
}

function resetCreateDraft(): void {
  attemptedCreateSubmit.value = false
  createDraft.value = {
    mode: 'existing_user',
    existingUserId: '',
    firstName: '',
    lastName: '',
    email: '',
    role: 'ORG_EMPLOYEE',
    active: true,
  }
}

function openCreateComposer(): void {
  isCreateComposerOpen.value = true
}

function closeCreateComposer(): void {
  isCreateComposerOpen.value = false
  resetCreateDraft()
}

async function handleCreateMember(): Promise<void> {
  const organizationId = resolvedOrganizationId.value

  if (!organizationId || !canSubmitNewMember.value) {
    attemptedCreateSubmit.value = true
    return
  }

  isCreatingMember.value = true
  errorMessage.value = null
  successMessage.value = null
  latestInvite.value = null

  try {
    if (createDraft.value.mode === 'existing_user') {
      const createdMember = await createOrganizationMember(
        { organizationId },
        {
          userId: createDraft.value.existingUserId.trim(),
          role: createDraft.value.role,
          active: createDraft.value.active,
        },
      )

      members.value = [toEditableMembership(createdMember), ...members.value]
      successMessage.value = 'Member added to the organization.'
    } else {
      const provision = await createManagedOrganizationMember(
        { organizationId },
        {
          email: createDraft.value.email.trim(),
          firstName: createDraft.value.firstName.trim(),
          lastName: createDraft.value.lastName.trim(),
          role: createDraft.value.role,
          active: createDraft.value.active,
        },
      )

      latestInvite.value = provision
      members.value = [toEditableMembership(provision.membership), ...members.value]
      successMessage.value = 'Invitation created for the new member.'
    }

    await loadMembers()
    closeCreateComposer()
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : 'Failed to create the member.'
  } finally {
    isCreatingMember.value = false
  }
}

async function handleSaveMember(member: EditableMembership): Promise<void> {
  const organizationId = resolvedOrganizationId.value

  if (!organizationId || !hasDraftChanges(member)) {
    return
  }

  if (isCurrentUserMembership(member)) {
    member.draftRole = member.role
    member.draftActive = member.active
    errorMessage.value = 'You cannot change your own role or access from the member directory.'
    return
  }

  savingMembershipId.value = member.id
  errorMessage.value = null
  successMessage.value = null

  try {
    const updatedMember = await updateOrganizationMember(
      {
        organizationId,
        membershipId: member.id,
      },
      {
        role: member.draftRole,
        active: member.draftActive,
      },
    )

    const index = members.value.findIndex((candidate) => candidate.id === member.id)

    if (index !== -1) {
      members.value[index] = toEditableMembership(updatedMember)
    }

    successMessage.value = `Updated ${getFullName(updatedMember)}.`
    await loadMembers()
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : 'Failed to update the member.'
  } finally {
    savingMembershipId.value = null
  }
}

onMounted(() => {
  void loadMembers()
})

watch(showInactiveMembers, () => {
  void loadMembers()
})

watch(resolvedEstablishmentId, () => {
  void loadMembers()
})

watch(
  () => createDraft.value.mode,
  () => {
    attemptedCreateSubmit.value = false
  },
)
</script>

<template>
  <div class="directory-page">
    <section v-if="!resolvedOrganizationId" class="notice-panel">
      <h2>Organization context required</h2>
      <p>
        This page needs an organization context. Sign in with an organization membership or set
        `VITE_DEFAULT_ORGANIZATION_ID` in development.
      </p>
    </section>

    <section v-else-if="!canManageMembers" class="notice-panel">
      <h2>Member management restricted</h2>
      <p>Only organization owners and admins can view and change member roles.</p>
    </section>

    <template v-else>
      <header class="page-header">
        <h1>Organization members</h1>
        <p class="page-copy">
          Manage staff access, member roles, and organization membership for
          {{ resolvedOrganizationName }}.
        </p>
      </header>

      <p v-if="successMessage" class="feedback-message feedback-message-success">
        {{ successMessage }}
      </p>
      <p v-if="errorMessage" class="feedback-message feedback-message-error">
        {{ errorMessage }}
      </p>
      <div v-if="latestInvite?.inviteUrl" class="invite-link-panel">
        <strong>Invitation link available in this environment</strong>
        <span>
          Open or share this link for {{ latestInvite.membership.userEmail }}. It expires at
          {{ latestInvite.inviteExpiresAt }}.
        </span>
        <a :href="latestInvite.inviteUrl" target="_blank" rel="noopener noreferrer">
          {{ latestInvite.inviteUrl }}
        </a>
      </div>

      <div class="content-grid">
        <section class="directory-panel">
          <header class="directory-header">
            <div>
              <h2>Members</h2>
              <p>{{ memberSummary }}</p>
            </div>

            <div class="directory-actions">
              <button
                type="button"
                class="secondary-button"
                @click="showInactiveMembers = !showInactiveMembers"
              >
                {{ showInactiveMembers ? 'Hide inactive' : 'Show inactive' }}
              </button>
              <button
                type="button"
                class="primary-button"
                @click="isCreateComposerOpen ? closeCreateComposer() : openCreateComposer()"
              >
                {{ isCreateComposerOpen ? 'Cancel' : 'Add member' }}
              </button>
              <button
                type="button"
                class="icon-button"
                @click="loadMembers"
                aria-label="Refresh members"
              >
                ↻
              </button>
            </div>
          </header>

          <p v-if="isLoading" class="state-message">Loading members...</p>
          <p v-else-if="members.length === 0 && !isCreateComposerOpen" class="state-message">
            {{
              showInactiveMembers
                ? 'No members found for this organization yet.'
                : 'No active members found for this organization.'
            }}
          </p>

          <div v-else class="directory-table-shell">
            <div class="directory-table directory-table-head" role="row">
              <span role="columnheader">Member details</span>
              <span role="columnheader">Role</span>
              <span role="columnheader">Status</span>
            </div>

            <form
              v-if="isCreateComposerOpen"
              class="directory-table directory-table-row directory-table-row-create"
              role="row"
              @submit.prevent="handleCreateMember"
            >
              <div class="cell member-cell composer-cell" role="cell">
                <div class="member-copy">
                  <strong>New member</strong>
                  <span
                    >Create from an existing platform user now, or invite a brand-new member to set
                    their own password.</span
                  >
                </div>
              </div>

              <div class="cell composer-fields" role="cell">
                <label class="form-field">
                  <span class="field-label">Provision mode</span>
                  <select
                    v-model="createDraft.mode"
                    class="field-input field-input-table"
                    @change="attemptedCreateSubmit = false"
                  >
                    <option value="existing_user">Existing user</option>
                    <option value="new_member">Invite new member</option>
                  </select>
                </label>

                <label v-if="createDraft.mode === 'existing_user'" class="form-field">
                  <span class="field-label">Existing user ID</span>
                  <input
                    v-model="createDraft.existingUserId"
                    class="field-input field-input-table"
                    :class="{ 'field-input-error': Boolean(existingUserIdError) }"
                    :aria-invalid="Boolean(existingUserIdError)"
                    type="text"
                    placeholder="Paste the user's UUID"
                    @input="attemptedCreateSubmit = false"
                  />
                  <span v-if="existingUserIdError" class="field-error">{{ existingUserIdError }}</span>
                </label>

                <template v-else>
                  <label class="form-field">
                    <span class="field-label">First name</span>
                    <input
                      v-model="createDraft.firstName"
                      class="field-input field-input-table"
                      :class="{ 'field-input-error': Boolean(inviteFirstNameError) }"
                      :aria-invalid="Boolean(inviteFirstNameError)"
                      type="text"
                      placeholder="First name"
                      @input="attemptedCreateSubmit = false"
                    />
                    <span v-if="inviteFirstNameError" class="field-error">{{ inviteFirstNameError }}</span>
                  </label>
                  <label class="form-field">
                    <span class="field-label">Last name</span>
                    <input
                      v-model="createDraft.lastName"
                      class="field-input field-input-table"
                      :class="{ 'field-input-error': Boolean(inviteLastNameError) }"
                      :aria-invalid="Boolean(inviteLastNameError)"
                      type="text"
                      placeholder="Last name"
                      @input="attemptedCreateSubmit = false"
                    />
                    <span v-if="inviteLastNameError" class="field-error">{{ inviteLastNameError }}</span>
                  </label>
                  <label class="form-field">
                    <span class="field-label">Email</span>
                    <input
                      v-model="createDraft.email"
                      class="field-input field-input-table"
                      :class="{ 'field-input-error': Boolean(inviteEmailError) }"
                      :aria-invalid="Boolean(inviteEmailError)"
                      type="email"
                      placeholder="name@company.com"
                      @input="attemptedCreateSubmit = false"
                    />
                    <span v-if="inviteEmailError" class="field-error">{{ inviteEmailError }}</span>
                  </label>
                </template>
              </div>

              <div class="cell composer-fields" role="cell">
                <label class="form-field">
                  <span class="field-label">Role</span>
                  <select v-model="createDraft.role" class="field-input field-input-table">
                    <option
                      v-for="roleOption in organizationRoles"
                      :key="roleOption.value"
                      :value="roleOption.value"
                    >
                      {{ roleOption.label }}
                    </option>
                  </select>
                </label>
              </div>

              <div class="cell status-cell composer-fields" role="cell">
                <label class="status-toggle">
                  <input v-model="createDraft.active" type="checkbox" />
                  <span>{{
                    createDraft.active ? 'Enabled immediately' : 'Create as inactive'
                  }}</span>
                </label>
                <span v-if="createDraft.mode === 'new_member'" class="pending-note">
                  An invitation link will be generated so the new member can choose their own
                  password.
                </span>
              </div>

              <div class="cell actions-cell composer-actions" role="cell">
                <button
                  type="submit"
                  class="primary-button"
                  :disabled="isCreatingMember"
                >
                  {{ isCreatingMember ? 'Provisioning...' : 'Create member' }}
                </button>
                <button type="button" class="secondary-button" @click="closeCreateComposer">
                  Cancel
                </button>
              </div>
            </form>

            <article
              v-for="member in members"
              :key="member.id"
              class="directory-table directory-table-row"
              :data-self-member="isCurrentUserMembership(member)"
              role="row"
            >
              <div class="cell member-cell" role="cell">
                <div class="member-copy">
                  <div class="member-title-row">
                    <strong>{{ getFullName(member) || member.userEmail }}</strong>
                    <span v-if="isCurrentUserMembership(member)" class="self-badge">You</span>
                  </div>
                  <span>{{ member.userEmail }}</span>
                  <span v-if="isCurrentUserMembership(member)" class="self-warning">
                    Self-edit is blocked here to prevent removing your own admin access.
                  </span>
                </div>
              </div>

              <div class="cell" role="cell">
                <div v-if="isCurrentUserMembership(member)" class="blocked-field">
                  <span class="blocked-field-value">
                    {{
                      organizationRoles.find((roleOption) => roleOption.value === member.role)
                        ?.label ?? member.role
                    }}
                  </span>
                </div>
                <select
                  v-else
                  v-model="member.draftRole"
                  class="field-input field-input-table"
                  :disabled="savingMembershipId === member.id"
                  @change="handleSaveMember(member)"
                >
                  <option
                    v-for="roleOption in organizationRoles"
                    :key="roleOption.value"
                    :value="roleOption.value"
                  >
                    {{ roleOption.label }}
                  </option>
                </select>
              </div>

              <div class="cell status-cell" role="cell">
                <div v-if="isCurrentUserMembership(member)" class="blocked-field">
                  <span class="status-pill" :data-active="member.active">
                    {{ member.active ? 'Active' : 'Inactive' }}
                  </span>
                </div>
                <label v-else class="switch-field">
                  <span
                    class="status-pill"
                    :data-active="member.draftActive"
                    :data-saving="savingMembershipId === member.id"
                  >
                    {{
                      savingMembershipId === member.id
                        ? 'Saving...'
                        : member.draftActive
                          ? 'Active'
                          : 'Inactive'
                    }}
                  </span>
                  <span class="switch-control">
                    <input
                      v-model="member.draftActive"
                      type="checkbox"
                      :disabled="savingMembershipId === member.id"
                      @change="handleSaveMember(member)"
                    />
                    <span class="switch-track"></span>
                  </span>
                </label>
              </div>
            </article>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.directory-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  color: var(--color-text-primary);
}

.page-header,
.directory-panel,
.notice-panel {
  display: flex;
  flex-direction: column;
}

.page-header {
  gap: 8px;
  max-width: 72ch;
}

.panel-kicker,
.field-label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-label);
  font-weight: 600;
  letter-spacing: var(--field-label-letter-spacing);
  text-transform: uppercase;
}

.page-header h1,
.page-header p,
.directory-header h2,
.directory-header p,
.notice-panel h2,
.notice-panel p,
.feedback-message {
  margin: 0;
}

.page-copy {
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: 1.45;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
}

.directory-panel,
.notice-panel {
  gap: 18px;
  padding: 20px;
  border: 1px solid #e6e8ef;
  border-radius: 6px;
  background-color: #fbfbfd;
}

.directory-header h2 {
  font-size: var(--font-size-heading-md);
  line-height: var(--line-height-tight);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-input {
  min-height: 44px;
  padding: 0.8rem 0.9rem;
  border: 1px solid #cfd5e3;
  border-radius: 2px;
  background-color: #fff;
  color: var(--color-text-primary);
  font: inherit;
  box-sizing: border-box;
}

.field-input:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}

.field-input-error {
  border-color: var(--color-critical);
}

.field-input-error:focus {
  outline: none;
  border-color: var(--color-critical);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-critical) 18%, transparent);
}

.field-error {
  color: var(--color-critical);
  font-size: var(--font-size-body-sm);
}

.field-hint,
.directory-header p,
.state-message {
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.status-toggle,
.switch-field {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--color-text-primary);
}

.primary-button,
.secondary-button,
.icon-button {
  min-height: 42px;
  border-radius: 2px;
  font: inherit;
  cursor: pointer;
}

.primary-button {
  border: 0;
  padding: 0.85rem 1rem;
  background-color: #1557b0;
  color: #fff;
  font-weight: 600;
}

.secondary-button {
  border: 1px solid #d5dae6;
  padding: 0.72rem 0.95rem;
  background-color: #fff;
  color: var(--color-text-primary);
}

.icon-button {
  width: 42px;
  border: 1px solid #d5dae6;
  background-color: #fff;
  color: var(--color-text-primary);
}

.primary-button:disabled,
.secondary-button:disabled,
.icon-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.directory-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.directory-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.directory-table-shell {
  overflow: hidden;
  border: 1px solid #e1e5ef;
  border-radius: 4px;
  background-color: #fff;
}

.directory-table {
  display: grid;
  grid-template-columns: minmax(280px, 1.8fr) minmax(180px, 0.95fr) minmax(170px, 0.85fr);
}

.directory-table-head {
  background-color: #f4f5f8;
  border-bottom: 1px solid #e1e5ef;
}

.directory-table-head span {
  padding: 14px 16px;
  color: #6a7488;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.directory-table-row {
  border-bottom: 1px solid #edf0f6;
}

.directory-table-row[data-self-member='true'] {
  background: linear-gradient(90deg, #fff4dd 0%, #fff9ef 100%);
}

.directory-table-row-create {
  background-color: #f8f9fc;
}

.directory-table-row:last-child {
  border-bottom: 0;
}

.cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 12px 14px;
}

.member-cell {
  align-items: start;
}

.member-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.member-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.member-copy strong {
  overflow-wrap: anywhere;
  font-size: 0.98rem;
}

.member-copy span,
.pending-note {
  /* color: #6a7488; */
  font-size: 0.84rem;
  overflow-wrap: anywhere;
}

.self-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.22rem 0.52rem;
  border: 1px solid #e9f1f7;
  border-radius: 999px;
  background-color: #2274a5;
  color: #ffffff;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.self-warning {
  color: #8a5a00;
  font-weight: 600;
}

.field-input-table {
  min-height: 38px;
  padding: 0.62rem 0.72rem;
}

.blocked-field {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 10px;
  min-height: 3em;
  width: 100%;
  padding: 0.72rem 0.8rem;
  border: 1px dashed #d4ae3d;
  border-radius: 4px;
  background-color: #fff8de;
  box-sizing: border-box;
}

.blocked-field-value {
  color: var(--color-text-primary);
  font-weight: 600;
}

.composer-cell {
  align-items: start;
}

.composer-fields {
  gap: 12px;
}

.status-cell,
.actions-cell {
  align-items: start;
}

.composer-actions {
  gap: 10px;
}

.switch-field {
  justify-content: space-between;
  min-width: 120px;
}

.switch-control {
  position: relative;
  display: inline-flex;
  width: 48px;
  height: 28px;
  flex-shrink: 0;
}

.switch-control input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
  margin: 0;
}

.switch-track {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 999px;
  background-color: #d4d9e4;
  transition: background-color 0.2s ease;
}

.switch-track::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(28, 36, 52, 0.22);
  transition:
    transform 0.2s ease,
    background-color 0.2s ease;
}

.switch-control input:checked + .switch-track {
  background-color: #3b82f6;
}

.switch-control input:checked + .switch-track::after {
  transform: translateX(20px);
}

.switch-control input:focus-visible + .switch-track {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background-color: #fde9e8;
  color: #b33c36;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.status-pill[data-active='true'] {
  background-color: #e3f6e7;
  color: #287d3c;
}

.status-pill[data-saving='true'] {
  background-color: #e8eefc;
  color: #284c93;
}

.pending-note {
  min-height: 1rem;
}

.feedback-message {
  padding: 12px 14px;
  border-radius: 4px;
}

.feedback-message-success {
  background-color: color-mix(in srgb, var(--color-primary) 10%, white);
}

.feedback-message-error {
  background-color: color-mix(in srgb, var(--color-critical) 10%, white);
  color: var(--color-critical);
}

.invite-link-panel {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  border: 1px solid #d8deea;
  border-radius: 4px;
  background-color: #f7f9fc;
}

.invite-link-panel strong {
  font-size: 0.95rem;
}

.invite-link-panel span,
.invite-link-panel a {
  overflow-wrap: anywhere;
}

.invite-link-panel a {
  color: var(--color-primary);
  text-decoration: none;
}

.invite-link-panel a:hover {
  text-decoration: underline;
}

@media (max-width: 1100px) {
  .directory-header {
    flex-direction: column;
    align-items: start;
  }
}

@media (max-width: 980px) {
  .directory-table-shell {
    overflow-x: auto;
  }

  .directory-table {
    min-width: 760px;
  }
}
</style>
