# Unimplemented design features

These controls are drawn from the designs in `docs/designs/project` (the Screens artifact), but the app has no
behavior for them yet. In code they call `UnimplementedAction`
(`shared/src/commonMain/kotlin/net/neednot/renttracker/core/designsystem/components/UnimplementedAction.kt`),
or are shown disabled where a dead tap would mislead. Search for `UnimplementedAction` to find every one.

Added on 2026-11-16 by the UI design sync (branch `feat/ui-design-sync`).

| Screen (board) | Control | What it should do | Backend / data needs |
| --- | --- | --- | --- |
| Home (HOME-01) | Search bar "Search groups and tenants" | Open the Search screen (HOME-02): tenants and groups whose name, unit or contact matches, archived tenants marked Archived, "No matches" empty state. | None on the server: search runs on data already on the phone (canvas note). Needs a Search screen, route and a Room query over cached groups and tenants. Search is in no spec area or milestone yet. |
| Group (GROUP-01) | Compact search bar "Search tenants" | Filter this group's tenant list. | Client only, over the cached tenants. Not in the spec yet. |
| Home, Archived groups, Group, Group settings, Archived tenants, Members, Invite member, Member permissions, Transfer ownership, Tenant, Ledger, Move tenant, Account (all Error boards) | "Try again" button | Retry loading the screen. | The screens read live Convex subscriptions that recover on their own, so the ViewModels have no retry. Needs a refresh/retry entry point on each repository (re-subscribe or a one-shot query). |
| Group settings (GROUP-02 Offline) | Offline state | Offline banner, Back arrow instead of Close, disabled fields, no Save. | `GroupSettingsViewModel` does not watch connectivity/freshness; needs the freshness source wired in through `GroupModule` (Koin). Today saving offline shows an error banner. |
| Members (MEMBER-01 Offline) | Offline banner and hidden write actions | "Offline · last updated …", hide Invite, row chevrons and Revoke. | UI is built and driven by `offlineBanner`, but the ViewModel always passes null: needs a members freshness repository over `FreshnessSource` with `CacheScope.members(groupId)`. |
| Members, Invite, Permissions (MEMBER-01/02/03 Error, Loading) | Group name as top-bar subtitle | Show the group name while loading or after an error. | The name is not known in those states; pass it through the route or read it from the cached group. |
| Account (AUTH-04 Offline) | Offline banner and hidden Edit name / deletion row | "Offline · last updated …" and hide the editing actions. | UI is built and driven by `AccountUiState.Content.offlineBanner`, which the ViewModel leaves null: needs a freshness source for `CacheScope.ACCOUNT` wired through `AccountModule`. |
| Sign in, Enter code, Display name (AUTH-01/02/03 Offline) | Offline banner, "Connect to the internet to sign in." banner, disabled actions | Show the offline state instead of letting a request fail. | UI is built and driven by `offlineBanner` in each auth UiState, which stays null: inject `core.data.Connectivity` into the three auth ViewModels through `AuthModule`. |
| Invite member, Member permissions (MEMBER-02/03) | "Custom" preset chip when not selected | Undefined: Custom is a derived state (any non-preset mix). | None. Needs a product decision on what tapping it does. |
| Group (GROUP-01 Loading/Error) | Group name in the top bar | Show the group name while loading or after an error. | Same as above: the name is not loaded yet. |
| Account (AUTH-04) | "App lock" switch | Require device unlock (biometric or PIN) to open the app. | Phone-only setting (BiometricPrompt on Android, LocalAuthentication on iOS) plus a stored preference; no server. Not in the current milestones (area 12 still lists app lock). |
| Account (AUTH-04) | "Late-rent notifications" section (per-group switches) | Turn late-rent push notifications on or off per group. The account screen has no group list, so it shows one disabled "All groups" row instead of a row per group. | Depends on milestone 10 (notifications, PR #11, not merged): FCM token registration, per-member notification preferences in Convex, and the planner honoring them. |
| Sign-out dialog (AUTH-04 Sign-out, unsynced payment) | Copy says an unsynced payment "stays on this phone and syncs the next time you sign in here" | Keep the pending-payment queue across sign-out and resume syncing when the same account signs in again on this phone. | Spec and code discard the queue on sign-out (M9). Needs the queue keyed by account and kept on sign-out, plus a spec change. The dialog uses the design's copy; the behavior still discards. |
