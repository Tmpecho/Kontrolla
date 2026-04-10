# Backend API Documentation

This document supplements Swagger and explains what the main endpoints do and what their important attributes mean.

## 1. Swagger / OpenAPI

Swagger is enabled through `springdoc-openapi` in [`backend/build.gradle`](../../backend/build.gradle).

Available endpoints:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Use Swagger for exact schemas and try-it-out requests. Use this document for developer-oriented explanations and payload meaning.

## 2. Authentication And Security Model

Public endpoints:

- `GET /api/v1/auth/csrf`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/invitations/{token}`
- `POST /api/v1/auth/invitations/{token}/accept`
- Swagger and actuator health endpoints

Protected endpoints:

- all remaining `/api/v1/**` endpoints require authentication

Security behavior:

- access token: bearer JWT
- refresh token: HTTP-only cookie
- CSRF: required for state-changing requests from the SPA
- CORS: configured in [`backend/src/main/java/org/kontrolla/iam/security/SecurityConfig.java`](../../backend/src/main/java/org/kontrolla/iam/security/SecurityConfig.java)

## 3. Common Response Shapes

### 3.1 Pagination

Many list endpoints return:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Attribute meaning:

- `items`: current page data
- `page`: zero-based page index
- `size`: requested page size
- `totalElements`: total number of matching rows
- `totalPages`: total page count

### 3.2 Error Response

Errors use RFC 7807 problem details with extra fields from [`RestExceptionHandler`](../../backend/src/main/java/org/kontrolla/common/api/RestExceptionHandler.java):

- `code`: application-specific error code
- `message`: readable error message
- `path`: request path
- `timestamp`: response timestamp

## 4. Endpoint Groups

## 4.1 System

### `GET /api/v1/system/startup-status`

Purpose:

- tells the frontend whether backend startup is complete

Response attributes:

- `status`: current startup status enum
- `ready`: `true` when the backend is ready to serve requests

## 4.2 Authentication (`/api/v1/auth`)

### `POST /login`

Purpose:

- authenticates a user and returns the active app session

Request attributes:

- `email`: login identity
- `password`: plaintext password entered by the user

Response highlights:

- `user`: authenticated user details
- `accessToken`: bearer JWT for API calls
- `tokenType`: normally `Bearer`
- `expiresIn`: access-token lifetime in seconds
- `appContext`: selected organization and establishment context

### `POST /refresh`

Purpose:

- creates a fresh access token from the refresh-token cookie

Response highlights:

- same structure as `/login`

### `POST /logout`

Purpose:

- revokes the refresh token and clears the cookie

Response:

- `204 No Content`

### `GET /invitations/{token}`

Purpose:

- loads public metadata for an invitation before acceptance

Response attributes:

- `email`: invited email
- `firstName`: invited first name
- `lastName`: invited last name
- `organizationName`: target organization
- `expiresAt`: invitation expiry timestamp

### `POST /invitations/{token}/accept`

Purpose:

- finalizes invite onboarding by setting the user password

Request attributes:

- `password`: new password for the invited account

### `GET /csrf`

Purpose:

- creates or returns the CSRF token used by the SPA

Response attributes:

- `token`: CSRF token value
- `headerName`: header the SPA must send
- `parameterName`: form parameter name if needed

### `GET /me`

Purpose:

- returns the current authenticated user

### `PUT /me`

Purpose:

- updates current user profile data

Request attributes:

- `firstName`: new first name
- `lastName`: new last name

### `PUT /me/password`

Purpose:

- changes the current user password

Request attributes:

- `currentPassword`: current password for verification
- `newPassword`: replacement password

## 4.3 Platform Admin

### `POST /api/v1/admin/users`

Purpose:

- creates a platform-level user

Request attributes:

- `email`: user email
- `firstName`: first name
- `lastName`: last name
- `password`: initial password
- `active`: whether account is enabled
- `globalRoles`: platform roles such as `PLATFORM_ADMIN`

### `GET /api/v1/admin/users`

Purpose:

- lists users for platform admins

### `POST /api/v1/admin/organizations`

Purpose:

- creates a new organization

Request attributes:

- `name`: organization name
- `status`: initial status, usually `ACTIVE`

### `GET /api/v1/admin/organizations`

Purpose:

- lists organizations for platform admins

## 4.4 Organizations And Memberships

### `GET /api/v1/organizations/{organizationId}`

Purpose:

- returns a single organization visible to the current user

### `GET /api/v1/organizations/{organizationId}/members`

Purpose:

- lists memberships for an organization

Query attributes:

- `establishmentId`: optional filter by establishment scope
- `includeInactive`: include inactive memberships when `true`
- `page`, `size`: pagination

Response highlights:

- `role`: organization role like `ORG_OWNER` or `ORG_EMPLOYEE`
- `allEstablishments`: whether the member can access every establishment
- `establishments`: explicit establishment scope when not global

### `POST /api/v1/organizations/{organizationId}/members`

Purpose:

- creates a membership for an existing user

Request attributes:

- `userId`: existing user to add
- `role`: organization role
- `active`: membership status
- `allEstablishments`: full establishment access flag
- `establishmentIds`: explicit allowed establishments

### `POST /api/v1/organizations/{organizationId}/members/managed-users`

Purpose:

- creates a new user and membership in one request

Request attributes:

- `email`, `firstName`, `lastName`: new user identity data
- `role`: organization role
- `active`: membership status
- `allEstablishments`: full establishment access flag
- `establishmentIds`: explicit allowed establishments

Response highlights:

- `membership`: created membership
- `inviteExpiresAt`: invite expiry timestamp
- `inviteUrl`: invite URL when exposure is enabled in the current profile

### `PATCH /api/v1/organizations/{organizationId}/members/{membershipId}`

Purpose:

- updates membership role, active state, and establishment scope

Request attributes:

- `role`: new organization role
- `active`: whether the membership stays active
- `allEstablishments`: full establishment access flag
- `establishmentIds`: explicit allowed establishments

## 4.5 Establishments And Serving Hours

### `GET /api/v1/organizations/{organizationId}/establishments`

Purpose:

- lists establishments the current user can access

### `POST /api/v1/organizations/{organizationId}/establishments`

Purpose:

- creates an establishment

Request attributes:

- `name`: establishment name
- `type`: establishment type such as `RESTAURANT` or `BAR`
- `status`: initial establishment status

### `GET /api/v1/organizations/{organizationId}/establishments/{establishmentId}`

Purpose:

- returns one establishment

### `GET /api/v1/organizations/{organizationId}/establishments/{establishmentId}/serving-hours`

Purpose:

- returns the weekly serving-hours configuration

Response attributes per day:

- `dayOfWeek`: weekday
- `closed`: whether the establishment is closed
- `opensAt`: opening time when open
- `closesAt`: closing time when open

### `PUT /api/v1/organizations/{organizationId}/establishments/{establishmentId}/serving-hours`

Purpose:

- replaces the full weekly serving-hours configuration

Request attributes per day:

- `dayOfWeek`: weekday being updated
- `closed`: closed/open flag
- `opensAt`: opening time
- `closesAt`: closing time

## 4.6 Checklists

Base paths:

- definitions: `/api/v1/organizations/{organizationId}/establishments/{establishmentId}/checklists/definitions`
- runs: `/api/v1/organizations/{organizationId}/establishments/{establishmentId}/checklists/runs`

### Definitions

#### `GET /checklists/definitions`

Purpose:

- lists checklist definitions for one service area

Query attributes:

- `serviceArea`: required, for example `IK_MAT` or `IK_ALKOHOL`
- `page`, `size`: pagination

#### `GET /checklists/definitions/{checklistDefinitionId}`

Purpose:

- returns one checklist definition including tasks and schedules

#### `POST /checklists/definitions`

Purpose:

- creates a new checklist definition

Request attributes:

- `title`: checklist title
- `description`: optional long-form description
- `serviceArea`: owning service area
- `tasks`: ordered task definitions
- `schedules`: optional run-generation schedules

Task attributes:

- `title`: task title
- `details`: optional extra instructions
- `taskKind`: task type
- `required`: required/optional flag
- `sortOrder`: UI ordering index
- `measurementUnit`: used by measurement tasks
- `minimumAllowedValue`: lower threshold for measurement tasks
- `maximumAllowedValue`: upper threshold for measurement tasks

Schedule attributes:

- `scheduleType`: recurrence rule type
- `startDate`: first active date
- `endDate`: optional end date
- `dueTime`: time of day the run becomes due
- `weekdayMask`: weekday bitmask for weekly schedules
- `dayOfMonth`: day number for monthly schedules
- `timezone`: timezone used to evaluate the schedule
- `active`: whether schedule is enabled

#### `PUT /checklists/definitions/{checklistDefinitionId}`

Purpose:

- creates a new version of an existing checklist definition

Extra request attribute:

- `status`: desired status for the new version

### Runs

#### `GET /checklists/runs`

Purpose:

- lists generated checklist runs

Query attributes:

- `serviceArea`: required service-area filter
- `statuses`: optional run status filter
- `assignedUserId`: optional user assignment filter
- `assignedToMe`: restrict to runs assigned to current user
- `dueFrom`, `dueTo`: due-date range
- `page`, `size`: pagination

#### `GET /checklists/runs/{checklistRunId}`

Purpose:

- returns one run with assignments, task snapshots, and event history

#### `POST /checklists/runs/{checklistRunId}/assignments`

Purpose:

- assigns one or more users to a run

Request attributes:

- `assignedUserIds`: list of user IDs to assign

#### `DELETE /checklists/runs/{checklistRunId}/assignments/{assignmentId}`

Purpose:

- removes one assignment from a run

#### `POST /checklists/runs/{checklistRunId}/start`

Purpose:

- marks the run as started

#### `POST /checklists/runs/{checklistRunId}/submit`

Purpose:

- submits results for all relevant tasks in the run

Request attributes:

- `tasks`: submitted task execution list

Per-task attributes:

- `checklistTaskExecutionId`: task execution being updated
- `executionStatus`: current execution state
- `comment`: optional comment
- `verificationResult`: pass/fail style result for verification tasks
- `measuredValue`: numeric result for measurement tasks
- `enteredText`: free-text result for text tasks

#### `PUT /checklists/runs/{checklistRunId}/tasks/{taskId}`

Purpose:

- updates a single task without submitting the whole run

Request attributes:

- same task-level fields as in run submission, except scoped to one task

#### `POST /checklists/runs/{checklistRunId}/reopen`

Purpose:

- reopens a completed or cancelled run

#### `POST /checklists/runs/{checklistRunId}/cancel`

Purpose:

- cancels a run

#### `POST /checklists/runs/{checklistRunId}/reset`

Purpose:

- resets the run to its initial state

## 4.7 Deviations

### `GET /api/v1/organizations/{organizationId}/deviations`

Purpose:

- lists deviations across the whole organization

### `GET /api/v1/organizations/{organizationId}/establishments/{establishmentId}/deviations`

Purpose:

- lists deviations for one establishment

### `GET /api/v1/organizations/{organizationId}/establishments/{establishmentId}/deviations/{deviationId}`

Purpose:

- returns a full deviation including timeline

### `POST /api/v1/organizations/{organizationId}/establishments/{establishmentId}/deviations`

Purpose:

- creates a new deviation

Request attributes:

- `title`: short issue summary
- `description`: detailed description of what happened
- `category`: business category of the deviation
- `severity`: seriousness of the deviation

### `PUT /.../deviations/{deviationId}/assignment`

Purpose:

- assigns the deviation to a user

Request attributes:

- `assignedUserId`: user responsible for follow-up

### `PUT /.../deviations/{deviationId}/status`

Purpose:

- changes deviation workflow status

Request attributes:

- `status`: new deviation status

### `PUT /.../deviations/{deviationId}`

Purpose:

- updates the editable deviation details

Request attributes:

- `title`
- `description`
- `category`
- `severity`

### `POST /.../deviations/{deviationId}/timeline`

Purpose:

- appends a note to the deviation timeline

Request attributes:

- `note`: timeline comment

## 4.8 Documents

Base path:

- `/api/v1/organizations/{organizationId}/establishments/{establishmentId}/documents`

### `GET /documents`

Purpose:

- lists documents by service area

Query attributes:

- `serviceArea`: required document area
- `page`, `size`: pagination

Response highlights:

- `title`, `holderName`, `issueDate`, `renewalDate`
- `fileName`, `contentType`, `fileSizeBytes`
- `status`: derived document status
- `auditAssignments`: users assigned to acknowledge reading

### `GET /documents/{documentId}`

Purpose:

- returns one document with metadata and audit assignments

### `GET /documents/{documentId}/file`

Purpose:

- downloads the stored document file

Response behavior:

- binary body
- `Content-Type` from stored file metadata
- `Content-Disposition` attachment filename header

### `POST /documents`

Purpose:

- uploads a new document

Content type:

- `multipart/form-data`

Parts:

- `metadata`: JSON object
- `file`: uploaded PDF or other stored document file

Metadata attributes:

- `serviceArea`: document business area
- `title`: document title
- `holderName`: document owner/holder
- `issueDate`: issue date
- `renewalDate`: renewal date
- `auditUserIds`: users who must acknowledge the document

### `PUT /documents/{documentId}`

Purpose:

- updates document metadata and audit assignments

Request attributes:

- same metadata fields as document creation

### `PUT /documents/{documentId}/file`

Purpose:

- replaces the stored file while keeping the document record

Content type:

- `multipart/form-data`

Parts:

- `file`: replacement file

### `DELETE /documents/{documentId}`

Purpose:

- deletes document metadata and stored file

### `POST /documents/{documentId}/acknowledge-read`

Purpose:

- records that the current user has acknowledged a document audit assignment

## 4.9 Notifications

### `GET /api/v1/notifications`

Purpose:

- lists notifications for the current user

Query attributes:

- `status`: notification filter, default `ALL`
- `page`, `size`: pagination

### `GET /api/v1/notifications/unread-count`

Purpose:

- returns unread notification count

Response attributes:

- `unreadCount`: count of unread notifications

### `POST /api/v1/notifications/{notificationId}/read`

Purpose:

- marks one notification as read

### `POST /api/v1/notifications/read-all`

Purpose:

- marks all notifications as read

## 4.10 Temperature Logging

Base path:

- `/api/v1/organizations/{organizationId}/establishments/{establishmentId}/temperature-units`

### `GET /temperature-units`

Purpose:

- lists all temperature units and their recent logs for one establishment

Response highlights:

- `name`: fridge/freezer/unit name
- `location`: physical location
- `type`: unit type
- `dueByTime`: expected daily logging deadline
- `minimumTemperature`, `maximumTemperature`: accepted range
- `logs`: recent readings

### `POST /temperature-units`

Purpose:

- creates a temperature unit

Request attributes:

- `name`: display name
- `location`: physical placement
- `type`: unit type
- `dueByTime`: logging deadline
- `minimumTemperature`: lower allowed temperature
- `maximumTemperature`: upper allowed temperature

### `POST /temperature-units/{temperatureUnitId}/logs`

Purpose:

- records one measured temperature value

Request attributes:

- `temperatureCelsius`: measured temperature
- `measuredAt`: measurement timestamp
- `note`: optional operator note

### `DELETE /temperature-units/{temperatureUnitId}`

Purpose:

- deletes a temperature unit

## 5. Javadoc Requirement

API controllers and DTOs already contain Javadoc comments in source, for example:

- [`backend/src/main/java/org/kontrolla/iam/api/AuthController.java`](../../backend/src/main/java/org/kontrolla/iam/api/AuthController.java)
- [`backend/src/main/java/org/kontrolla/checklists/api/ChecklistDefinitionController.java`](../../backend/src/main/java/org/kontrolla/checklists/api/ChecklistDefinitionController.java)
- [`backend/src/main/java/org/kontrolla/documents/api/DocumentController.java`](../../backend/src/main/java/org/kontrolla/documents/api/DocumentController.java)
