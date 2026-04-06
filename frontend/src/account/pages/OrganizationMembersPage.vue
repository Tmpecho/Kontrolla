<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import {
  createOrganizationMember,
  listOrganizationMembers,
  updateOrganizationMember,
} from '@/account/api/organization-members.api'
import type {
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

const totalMembers = computed(() => members.value.length)
const activeMembers = computed(() => members.value.filter((member) => member.active).length)

const supportsDirectMemberCreation = false

const canSubmitNewMember = computed(() => {
  if (createDraft.value.mode === 'existing_user') {
    return createDraft.value.existingUserId.trim().length > 0
  }

  return supportsDirectMemberCreation
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

async function loadMembers(): Promise<void> {
  const organizationId = resolvedOrganizationId.value

  if (!organizationId) {
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    const page = await listOrganizationMembers({
      organizationId,
      size: 100,
    })

    members.value = page.items.map(toEditableMembership)
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load organization members.'
  } finally {
    isLoading.value = false
  }
}

function resetCreateDraft(): void {
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
    return
  }

  isCreatingMember.value = true
  errorMessage.value = null
  successMessage.value = null

  try {
    const createdMember = await createOrganizationMember(
      { organizationId },
      {
        userId: createDraft.value.existingUserId.trim(),
        role: createDraft.value.role,
        active: createDraft.value.active,
      },
    )

    members.value = [toEditableMembership(createdMember), ...members.value]
    closeCreateComposer()
    successMessage.value = 'Member added to the organization.'
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to add the member to the organization.'
  } finally {
    isCreatingMember.value = false
  }
}

async function handleSaveMember(member: EditableMembership): Promise<void> {
  const organizationId = resolvedOrganizationId.value

  if (!organizationId || !hasDraftChanges(member)) {
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
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to update the member.'
  } finally {
    savingMembershipId.value = null
  }
}

onMounted(() => {
  void loadMembers()
})
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

    <template v-else>
      <header class="hero">
        <p class="eyebrow">Entity Management / {{ resolvedOrganizationName }}</p>
        <h1>Member Directory</h1>
        <p class="hero-copy">
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

      <div class="content-grid">
        <section class="directory-panel">
          <header class="directory-header">
            <div>
              <h2>Active Personnel</h2>
              <p>{{ activeMembers }} active of {{ totalMembers }} total members</p>
            </div>

            <div class="directory-actions">
              <button
                type="button"
                class="primary-button"
                @click="isCreateComposerOpen ? closeCreateComposer() : openCreateComposer()"
              >
                {{ isCreateComposerOpen ? 'Cancel' : 'Add member' }}
              </button>
              <button type="button" class="icon-button" @click="loadMembers" aria-label="Refresh members">
                ↻
              </button>
            </div>
          </header>

          <p v-if="isLoading" class="state-message">Loading members...</p>
          <p v-else-if="members.length === 0" class="state-message">
            No members found for this organization yet.
          </p>

          <div v-else class="directory-table-shell">
            <div class="directory-table directory-table-head" role="row">
              <span role="columnheader">Member details</span>
              <span role="columnheader">Identifiers</span>
              <span role="columnheader">Role</span>
              <span role="columnheader">Status</span>
              <span role="columnheader">Actions</span>
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
                  <span>Create from an existing platform user now, or prepare a direct member record for later backend support.</span>
                </div>
              </div>

              <div class="cell identifiers-cell composer-fields" role="cell">
                <label class="form-field">
                  <span class="field-label">Provision mode</span>
                  <select v-model="createDraft.mode" class="field-input field-input-table">
                    <option value="existing_user">Existing user</option>
                    <option value="new_member">New member</option>
                  </select>
                </label>

                <label v-if="createDraft.mode === 'existing_user'" class="form-field">
                  <span class="field-label">Existing user ID</span>
                  <input
                    v-model="createDraft.existingUserId"
                    class="field-input field-input-table"
                    type="text"
                    placeholder="Paste the user's UUID"
                  />
                </label>

                <template v-else>
                  <label class="form-field">
                    <span class="field-label">First name</span>
                    <input
                      v-model="createDraft.firstName"
                      class="field-input field-input-table"
                      type="text"
                      placeholder="First name"
                    />
                  </label>
                  <label class="form-field">
                    <span class="field-label">Last name</span>
                    <input
                      v-model="createDraft.lastName"
                      class="field-input field-input-table"
                      type="text"
                      placeholder="Last name"
                    />
                  </label>
                  <label class="form-field">
                    <span class="field-label">Email</span>
                    <input
                      v-model="createDraft.email"
                      class="field-input field-input-table"
                      type="email"
                      placeholder="name@company.com"
                    />
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
                  <span>{{ createDraft.active ? 'Enabled immediately' : 'Create as inactive' }}</span>
                </label>
                <span v-if="createDraft.mode === 'new_member'" class="pending-note">
                  Direct member creation UI is prepared, but backend support is not enabled yet.
                </span>
              </div>

              <div class="cell actions-cell composer-actions" role="cell">
                <button
                  type="submit"
                  class="primary-button"
                  :disabled="!canSubmitNewMember || isCreatingMember"
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
              role="row"
            >
              <div class="cell member-cell" role="cell">
                <div class="member-copy">
                  <strong>{{ getFullName(member) || member.userEmail }}</strong>
                  <span>{{ member.userEmail }}</span>
                </div>
              </div>

              <div class="cell identifiers-cell" role="cell">
                <span><strong>UID</strong> {{ member.userId }}</span>
                <span><strong>MID</strong> {{ member.id }}</span>
              </div>

              <div class="cell" role="cell">
                <select v-model="member.draftRole" class="field-input field-input-table">
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
                <span class="status-pill" :data-active="member.active">
                  {{ member.active ? 'Compliant' : 'Inactive' }}
                </span>
                <label class="status-toggle">
                  <input v-model="member.draftActive" type="checkbox" />
                  <span>Enabled</span>
                </label>
              </div>

              <div class="cell actions-cell" role="cell">
                <span v-if="hasDraftChanges(member)" class="pending-note">Unsaved</span>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="savingMembershipId === member.id || !hasDraftChanges(member)"
                  @click="handleSaveMember(member)"
                >
                  {{ savingMembershipId === member.id ? 'Saving...' : 'Save' }}
                </button>
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

.hero,
.directory-panel,
.notice-panel {
  display: flex;
  flex-direction: column;
}

.hero {
  gap: 10px;
  max-width: 72ch;
}

.eyebrow,
.panel-kicker,
.field-label {
  color: var(--color-text-secondary);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero h1,
.hero p,
.provision-header h2,
.directory-header h2,
.directory-header p,
.notice-panel h2,
.notice-panel p,
.feedback-message {
  margin: 0;
}

.hero h1 {
  font-size: clamp(2.5rem, 5vw, 4rem);
  line-height: 0.94;
  letter-spacing: -0.05em;
}

.hero-copy {
  color: var(--color-text-secondary);
  font-size: 1.05rem;
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
  font-size: 1.75rem;
  line-height: 1;
  letter-spacing: -0.03em;
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

.field-hint,
.directory-header p,
.state-message {
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.status-toggle {
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
  grid-template-columns: minmax(240px, 1.35fr) minmax(230px, 1fr) minmax(160px, 0.8fr) minmax(170px, 0.8fr) minmax(120px, 0.55fr);
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
  gap: 10px;
  padding: 16px;
}

.member-cell {
  flex-direction: row;
  align-items: start;
  gap: 12px;
}

.member-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.member-copy strong {
  overflow-wrap: anywhere;
  font-size: 1rem;
}

.member-copy span,
.identifiers-cell span,
.pending-note {
  color: #6a7488;
  font-size: 0.875rem;
  overflow-wrap: anywhere;
}

.identifiers-cell strong {
  margin-right: 6px;
  color: #8892a6;
  font-size: 0.72rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.field-input-table {
  min-height: 40px;
  padding: 0.7rem 0.8rem;
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

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background-color: #fde9e8;
  color: #b33c36;
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.status-pill[data-active='true'] {
  background-color: #e3f6e7;
  color: #287d3c;
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
    min-width: 980px;
  }
}
</style>
