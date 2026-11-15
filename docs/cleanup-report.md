# Rent Tracker cleanup report

A read-only audit of the whole repository, written to plan later cleanup passes. No app code was changed.

- **Code audited:** `main` at `5ba8e5d` (milestones 1–8). PR #9 (offline payments, still open) was **not** reviewed, so anything it adds or fixes is not covered here.
- **Designs audited:** the three design artifacts on claude.ai (see section 7), the untracked local copy in `docs/designs/project/` of the main checkout, and the tech spec's area 12 (`docs/specs/tech/12-ui-design-system.md`).
- **Not run:** no Gradle, npm or Convex command was run. The findings come from reading code, plus a few scripts that compared files for duplication and counted the "·" separators.

Severity:

- **High:** causes wrong behaviour now, or makes the next milestone noticeably harder or riskier.
- **Medium:** real duplication or drift that will cost time on each change.
- **Low:** tidy-ups.

Paths are relative to the repo root. `…/renttracker/` stands for `shared/src/commonMain/kotlin/net/neednot/renttracker/`.

---

## 1. Top findings (do these first)

| # | Sev | Finding | Section |
|---|-----|---------|---------|
| 1 | High | Three design sources disagree on colors, calendar, chips, icons and dark mode. The app implements a fourth mix of its own. | 7 |
| 2 | High | The calendar and the state chips in the app don't match the canvas: solid green/red fills instead of pale tints, the fee marker is in the wrong place, there is no current-month outline, and Owes/Paid up chips borrow calendar colors. | 6.2 |
| 3 | High | Convex function names and error codes are untyped strings in Kotlin: 46 function-name literals, and 32 error-code constants copied across 9 files. A rename in Convex still compiles and only fails at runtime. | 5.1 |
| 4 | High | Codegen writes one Kotlin enum or class per function argument. `PaymentMethod` exists 5 times, the permissions shape 4 × 7 files, and the month-state enum 3 times. | 2.1 |
| 5 | Medium | Shared UI is copied into each feature because features can't import each other and `core/designsystem` is missing the shared pieces: 4 editor top bars, 3 header cards, 14 copies of the card style, 5 `Loading()` and 3 `Message()` helpers. | 2.2, 6.1 |
| 6 | Medium | The 5 `*BackendCalls` classes and the 8 `*ErrorMapping` files are near-copies, including the `BACKEND_TIMEOUT = 20.seconds` constant. | 2.3 |
| 7 | Medium | The server writes user-facing English sentences that the app shows word for word through `UiText.Plain(message)`. This breaks the "all text in string resources" rule. | 5.2 |
| 8 | Medium | The "·" separator is used 286 times across 57 distinct strings in the designs, and in 49 app strings. About two thirds should be rewritten. | 8 |
| 9 | Medium | Missing tests: `ConvexBackend` (the threading fix for the main-thread deadlock has no test), two repositories, two error mappings, and only 2 screenshot tests for about 30 screens. | 4 |
| 10 | Low | Dead or placeholder code: `members:mine` (Convex), `MembersMineArgs` (generated), `PendingPaymentDao` on `main`, and `backup-worker`, which only answers `/health`. | 2.4 |

---

## 2. Dead and duplicated code

### 2.1 Codegen duplicates the same shape per function (High)

Files: `tools/codegen/src/buildModels.ts`, `tools/codegen/src/naming.ts`, and the output in `…/renttracker/model/`.

| Shape | Generated copies |
|---|---|
| Payment method enum | `model/tenants/PaymentMethod.kt`, `model/shared/TenantLastPaymentMethod.kt`, `model/payments/PaymentsLogMethod.kt`, `model/payments/PaymentsEditMethod.kt`, plus a hand-written fifth copy in `feature/payments/data/PaymentMethodDetails.kt` |
| Month state enum | `model/tenants/CalendarMonthState.kt`, `model/payments/ChangeBefore.kt`, `model/payments/ChangeAfter.kt` |
| Permission set (7 files each) | `model/home/InvitationPermissions*.kt`, `model/invites/InvitesSendPermissions*.kt`, `model/members/InvitePermissions*.kt`, `model/members/MembersUpdatePermissionsPermissions*.kt` (28 files for one shape) |
| Add/edit args | `model/tenants/TenantsAddArgs.kt` and `TenantsEditArgs.kt` are 79% identical (this one is fine and can stay) |

Because each copy is its own type, each feature has to map the copy it receives into its own domain enum. That accounts for a large share of the `*Mapping.kt` files.

**Fix:** give shared validators a name in `convex/validators.ts` (`paymentMethod`, `calendarMonthState`, `permissionSet` and `permissionLevel` already exist or nearly do). Then make codegen emit one Kotlin type per named validator, in `model/shared/`, and reference it everywhere. As a fallback, deduplicate by structural hash, keeping the first name. After that, delete `PaymentMethodDetails.kt` and decode straight into the shared enum.

### 2.2 Copied UI components (Medium)

| Duplicate | Files | Fix |
|---|---|---|
| Editor top bar with a Save button and a spinner (4 copies; two are 97% identical) | `core/designsystem/components/EditorTopBar.kt` (no spinner), `feature/debts/…/ChargeEditorTopBar.kt`, `feature/payments/…/PaymentEditorTopBar.kt`, `feature/members/…/MembersEditorTopBar.kt` | Add `isWorking` and `workingLabel` (defaulting to Save/Saving) to the core `EditorTopBar` and delete the three feature copies. |
| Tenant header card (identical) | `feature/debts/…/ChargeHeaderCard.kt`, `feature/payments/…/PaymentHeaderCard.kt`, plus the near-twin `feature/members/…/MemberHeaderCard.kt`. Their UI states `ChargeHeaderUiState` and `PaymentHeaderUiState` are also twins. | Replace all of them with one `TenantStrip` in core (this is also what the design draws; see 6.2). |
| Outlined card style (`OutlinedCard` + `shapes.large` + `surfaceContainerLowest` + 1dp `outlineVariant`), repeated 14 times | `GroupCard`, `ArchivedGroupCard`, `InvitationCard`, `MembersListCard`, `PermissionLevelsCard`, `TenantListCard`, `LateFeeOfferBox`, `PaymentHeaderCard`, `ChargeHeaderCard`, `AccountCard`, `CalendarCard`, `MoveTenantScreen`, `MonthDetailSheet`, `SectionCard` | Add `RtCard` / `RtListCard` to `core/designsystem/components` and use them everywhere. |
| List card holding rows with dividers | `feature/group/…/TenantListCard.kt` and `feature/members/…/MembersListCard.kt` (78% similar) | `RtListCard(items) { row }` in core. |
| Error text | `feature/auth/…/AuthErrorText.kt`, `feature/group/…/SyncErrorText.kt`, plus inline red `Text` in `HomeScreen.kt` (`syncError`) and `TenantScreen.kt` (`TenantSections`) | Use the core `Notice` (it already exists and is used 20 times) as the single inline error/banner. |
| Loading and empty states | Private `Loading()` in 5 screens, `Message()` in 3, `ErrorMessage()` in 2, plus `feature/group/…/LoadingIndicator.kt` and `CenteredMessage.kt` | Add `LoadingState`, `EmptyState` and `ErrorState` to core. The design already defines `EmptyState`. |
| Duplicate-warning dialog | `feature/payments/…/DuplicateDialog.kt` and `feature/debts/…/LateFeeDuplicateDialog.kt` (61% similar, with separate `DuplicatePolicy`, `DuplicateMatch` and `DuplicateLateFee` types in each feature) | One `DuplicateWarningDialog(matches)` in core, with one domain type in `core/data`. |
| "Archived …" row | `feature/group/…/ArchivedTenantsLink.kt` and `feature/home/…/ArchivedGroupsRow.kt` (61% similar) | One `ArchivedRow(text, onClick)` in core. |
| Confirm dialogs | 16 `AlertDialog(` calls in 12 files, each restyling the destructive button by hand | `ConfirmDialog(title, body, confirmLabel, isDestructive)` in core. |

### 2.3 Copied data-layer plumbing (Medium)

- **Backend call wrappers:** `feature/{debts,group,members,payments,tenants}/data/*BackendCalls.kt` repeat `subscribe`, `queryOnce`, `mutate`, the timeout and telemetry logging. Only the error type differs. `AuthRepositoryImpl.kt` has its own copy of the timeout too. **Fix:** a generic `core/backend/TypedBackend<E>(backend, telemetry, toError: (BackendError) -> E, offline: E)` and one `BackendTimeout` constant.
- **Error mapping:** the 8 `feature/*/data/*ErrorMapping.kt` files each re-declare the codes they need as `private const val` strings: 31 constants in all (`VALIDATION_FAILED` 8×, `NOT_FOUND` 6×, `PERMISSION_DENIED` 6×, `EDIT_CONFLICT` 5×, `TENANT_ARCHIVED` 3×, `NEEDS_CONFIRMATION` 2×, `NOT_SIGNED_IN` 2×). There is one more in `core/data/GroupAccessWatcher.kt`. **Fix:** generate an `AppErrorCode` enum from `convex/appError.ts` (see 5.1) and map from it.
- **Error details DTOs:** `feature/{debts,group,members,payments}/data/EditConflictDetails.kt` are byte-identical. The same goes for `NeedsConfirmationDetails` in debts and payments. **Fix:** generate them from the `details` payloads in `appError`, or move them into `core/backend`.
- **Balance text:** `feature/payments/…/PaymentBalanceText.kt`, `feature/debts/…/DebtBalanceText.kt` and `feature/group/…/TenantBalanceText.kt` hold the same three-way `when`. Their domain types (`PaymentBalance`, `DebtBalance`, `tenants/domain/BalanceTotal`) have the same shape. **Fix:** one `core/data/Balance` with one `inWords()` next to `owesText` in `core/designsystem/format/MoneyFormat.kt`.
- **Month name:** `feature/payments/…/MonthName.kt` and `feature/debts/…/MonthName.kt` are identical one-liners. **Fix:** move to `core/designsystem/format/MonthFormat.kt`.
- **Strings:** "No connection. Check your internet and try again." and "Something went wrong. Please try again." each appear 6 times. "Back" also appears 6 times, and "Save", "View only", "Late fee" and "Payment" 3 times each. **Fix:** `common_error_offline`, `common_error_unexpected`, `common_back` and so on, kept in `strings.xml`.
- **Workers:** `auth-worker/src/sharedSecret.ts` and `email-worker/src/sharedSecret.ts` are identical. **Fix:** a tiny `workers-shared` workspace package, or one file both import through a relative path. The secret header name `X-Rent-Tracker-Secret` should live there too (it is in `convex/workerPost.ts` today).
- **Convex cancel/restore:** `convex/payments.ts` and `convex/debts.ts` each have a local `cancel` and `restore` with the same load, permission, changeable and patch steps. `rentTerms.ts` calls its version `softDelete`. All three are exported as `delete`. **Fix:** one `cancelEntry` helper in `entryRestore.ts` (which already has `clearCancellation`), and one internal name (`cancel`).

### 2.4 Dead or placeholder code (Low)

| Item | Where | Note |
|---|---|---|
| `members:mine` query | `convex/members.ts:51` | No Kotlin caller. It is used only by tests (`members.test.ts`, `permissionMatrix.test.ts`). Delete it, or note why it is kept. |
| `MembersMineArgs` | `…/renttracker/model/members/MembersMineArgs.kt` | Generated for the unused query above. It goes away with the query. |
| `PendingPaymentEntity` / `PendingPaymentDao` | `…/renttracker/core/data/` | Added in M1 and unused on `main`. PR #9 starts using it. Nothing to do if PR #9 merges. |
| `TenantMoved` error code | `convex/appError.ts` | No Kotlin mapping on `main` (`TenantArchived` is mapped in 3 features). It is for PR #9. Check after it merges. |
| `backup-worker` | `backup-worker/src/index.ts` | Only answers `GET /health`, but CI bundles it on every run. It is scheduled for M11, so keep it but label it a stub in its README. |
| `EditorTopBar` without a spinner | `core/designsystem/components/EditorTopBar.kt` | Used by 3 editors, while the others use copies. It merges into the shared bar from 2.2. |

The Kotlin side has very little true dead code: a script that checked every top-level declaration found only entry points (`MainActivity`, `MainViewController`, `initKoinIos`).

---

## 3. Oversized files and functions

Nothing is huge: the largest Kotlin file has 500 lines and the largest non-test TS file 236. The real issues are **parameter lists** and **mixed concerns** in a few screens.

| File | Lines | Problem | Fix |
|---|---|---|---|
| `…/feature/tenants/presentation/screens/TenantScreen.kt` | 500 | `TenantContent` takes 20 lambdas, and `TenantSections` takes 10. The file also holds the menu, the archived label, the status dialog, loading, errors and a preview. | Group the callbacks into `TenantActions` the way `CalendarActions` and `GroupActions` already do. Move `TenantMenu`, `StatusChangeDialog` and `ArchivedLabel` into `components/`. |
| `…/feature/members/presentation/screens/MembersScreen.kt` | 345 | Several dialogs and rows inline | Split into components the way `group/` does. |
| `…/feature/tenants/presentation/screens/MoveTenantScreen.kt` | 343 | Has a private `AlertDialog` and its own card style | Use `ConfirmDialog` and `RtCard`. |
| `…/feature/group/presentation/screens/GroupScreen.kt` | 343 | Fine overall (it uses `GroupActions`). It still has a private `GroupSummary` and preview data with hard-coded strings. | Leave for now. |
| `…/feature/group/presentation/screens/GroupSettingsScreen.kt` | 325 | Form fields built inline | Use the core `FormTextField` and `PickerField`. |
| `InviteMemberScreen.kt` / `MemberPermissionsScreen.kt` | 264 / 225 | 62% similar: both draw the preset chips and the per-category level editor | Add a `PermissionEditor(form, onChange)` component in `members/presentation/components`. |
| `ChargeEditorViewModel.kt` / `PaymentEditorViewModel.kt` | 162 / 159 | 60% similar (snapshot, conflict, duplicate, save flow) | Extract an `EntryEditorController` into `core/ui`, or accept the duplication until a third editor appears. |
| `convex/permissionMatrix.test.ts` | 685 | Large, but table-driven | Fine as it is. |

Inconsistent callback patterns: some screens bundle callbacks in an `*Actions` class (`GroupActions`, `CalendarActions`, `InvitationActions`), while others pass 10 to 20 lambdas. Pick one rule ("more than 5 callbacks means an `*Actions` class") and apply it to every screen.

---

## 4. Missing tests

The Kotlin ViewModels and repositories are well covered (103 test files), and so is Convex: every public module has a `.test.ts` except thin helpers. These are the gaps:

| Sev | What | Where | Suggested test |
|---|---|---|---|
| High | `ConvexBackend`, including the "every call off the main thread" fix from PR #8 | `androidApp/src/main/kotlin/net/neednot/renttracker/backend/ConvexBackend.kt` | A Robolectric or JVM test that starts two subscriptions together on the main dispatcher and checks that both emit. Move the class into `shared/androidMain` so it can be tested next to `BetterAuthSession` (see 5.4). |
| Medium | `LedgerEntryRepositoryImpl`, `HomeAccountRepositoryImpl` | `feature/tenants/data/`, `feature/home/data/` | Repository tests in the style of `TenantRepositoryImplTest`, which use `FakeBackend`. |
| Medium | `MembersErrorMapping`, `AccountErrorMapping` | `feature/members/data/`, `feature/account/data/` | Every other feature has a `*ErrorMappingTest`. Better still, test the single generated mapping once (2.3). |
| Medium | Screen-level screenshot tests | `shared/src/androidHostTest/…/screenshots/` holds only `CalendarGridScreenshotTest` and `TenantStateChipScreenshotTest` | Add Roborazzi snapshots of every `@Preview` (Home, Group, Tenant, editors, sheets) in light and dark. These are what will catch the drift described in section 6. |
| Medium | Permission rules duplicated in Kotlin and Convex | `…/core/access/PermissionRules.kt` vs `convex/permissionRules.ts`; presets in both `PermissionPreset` and `PERMISSION_PRESETS` | Export the cases in `convex/permissionMatrix.fixture.ts` as JSON (codegen can do it) and run the same cases through the Kotlin `isAtOrBelow` and `raisesOnlyWithin`. |
| Low | Codegen output | `tools/codegen/src/generateModelFiles.test.ts` covers only one fixture | Add a fixture with a shared validator used by two functions, to lock in the fix from 2.1. |
| Low | Convex HTTP helpers | `convex/workerPost.ts`, `convex/authWorker.ts` (they are covered only indirectly through `support.test.ts` and `invites.test.ts`) | Unit-test the trailing-slash handling, the missing-env error, and the 409 → `EMAIL_TAKEN` mapping. |
| Low | iOS | `shared/src/iosMain/` has only `NotYetSupported*` stubs | Nothing to test yet. Note it in the spec. |

---

## 5. Architecture, layering, and the Kotlin ↔ Convex contract

### 5.1 The contract is partly untyped (High)

- **Function names:** 46 string literals such as `"payments:log"` and `"tenants:screen"` are spread over `feature/*/data/*RepositoryImpl.kt`. Codegen already reads Convex's function spec. **Fix:** generate `model/ConvexFunctions.kt` (for example `object Payments { const val LOG = "payments:log" }`), or typed wrappers that pair the name with its Args and Result serializers, and forbid raw strings with an architecture test.
- **Error codes and details:** `AppErrorCode` in `convex/appError.ts` is a TS union. Kotlin re-declares it as `private const val` strings in 9 files (31 constants in the `*ErrorMapping.kt` files, plus `GroupAccessWatcher`). **Fix:** generate an `AppErrorCode` enum and the details classes, then delete the copies (2.3).
- **Naming drift between the two sides:**
  - **Debt or charge:** Convex `debts`, Kotlin `feature/debts`, but the screens say `ChargeEditor` and the design says "Add charge". Pick one word for code (suggest `charge`, since that is what users see) or document the mapping in the spec glossary.
  - **Delete or cancel:** the schema fields are `deletedAt`, `deletedBy` and `deleteReason`, the exported function is `delete`, the internal functions are `cancel` and `softDelete`, and the UI says "Cancel" and "Cancelled". Renaming fields means a migration, so first unify the function names and add a glossary line.
  - Kotlin `PermissionCategory` leaves out `settings` and handles it separately through `SettingsAccessLevel` (read/full). Convex treats it as one of six categories in `LEVEL_CATEGORIES`. The behaviour matches, but the models differ, and that is easy to break.

### 5.2 Server-made English text (Medium)

`convex/inputValidation.ts` alone has 45 `validationFailed("…")` sentences, and `convex/duplicateEntries.ts` builds sentences with its own `dateText.ts` month names. The app shows them through `UiText.Plain(message)` (for example `feature/payments/presentation/viewmodel/PaymentErrorText.kt:12`). The wording also drifts from the app's own strings: the server says "Grace period must be 0 to 15 whole days.", while `tenant_editor_error_grace_days` says "Enter a whole number of days from 0 to 15."

**Fix:** keep the server messages for logs and support, but send a machine reason in `details` (`{field, rule: "range", min, max}`) and render it from string resources on the phone. Then `convex/dateText.ts` can go away.

### 5.3 Validation lives in two places (Low, by design)

Client-side form validation (`*FormValidation.kt`, `core/validation`) mirrors `convex/inputValidation.ts`, and the limits (amount ≤ $1,000,000.00, grace days 0–15, due day 1–31, name lengths) are written down twice. **Fix:** have codegen emit `model/shared/InputLimits.kt` from constants exported by `convex/inputValidation.ts`.

### 5.4 Module placement (Low)

- `ConvexBackend` and `SessionJwtProvider` live in `androidApp/`, while the Android auth session (`BetterAuthSession`) lives in `shared/androidMain`. **Fix:** move both into `shared/androidMain/core/backend` so all platform adapters sit together and can be tested (4).
- Layering is otherwise healthy, and `shared/src/jvmTest/…/architecture/ArchitectureTest.kt` enforces it: no feature imports another feature, core never imports a feature, and `model` is imported only by `data`. The duplication in 2.2 and 2.3 is a side effect of that rule. The fix is to grow `core/`, not to relax the rule.

### 5.5 Spec gaps found while auditing (Low)

- `docs/specs/tech/12-ui-design-system.md` still lists Account as "display name, app lock, notifications per group", but M8 chose only Edit name, Sign out and Request deletion. It also says "Owes $300", while the same page decides "always cents".
- **Search** is designed (`Search.dc.html`, the Home search bar and the group search field) but appears in no spec area and no milestone. Either add it to the product spec and build order, or remove it from the canvas.
- The memory notes say the area-12 navigation diagram may still show History and Trash. The old design system (7.1) definitely still does.

---

## 6. UI: components and match with the design

### 6.1 Is the UI split into reusable components with variants?

**Partly.** Each feature has a tidy `components/` folder, but the reusable pieces stop at the feature boundary. `core/designsystem/components` has 16 files, and only some of them are general-purpose: `FormTextField`, `DateField`, `MonthField`, `PickerField`, `OtpField`, `Notice`, `SectionCard`, `EditorTopBar`, `InitialsAvatar`, `CalendarSquare` and `TenantStateChip`.

The design system already names the components the app needs. The table below lists what the app is missing, or has only as per-feature copies or inline code.

| Design component | App today | Make it a core component with these variants |
|---|---|---|
| Button (filled / tonal / outlined / danger / text / small with spinner) | Raw M3 `Button` / `OutlinedButton` / `TextButton`. "Add charge" is `OutlinedButton` but should be tonal. | `RtButton(variant, isWorking)` |
| TopAppBar (back or close, title plus a second line, actions) | Built inline per screen (`TenantTitle`, `GroupTitle`, `ledger_subtitle`), plus 4 editor bars | `RtTopBar(nav = Back/Close, title, subtitle, actions)` and `EditorTopBar(isWorking)` |
| StatusChip (Late / Owes / Paid up / Archived / View only) | `TenantStateChip` (3 states) and `CalendarStateChip`. "View only" is an inline `Text` in `GroupTitle.kt:69`, and Archived is `ArchivedLabel` in `TenantScreen.kt`. | One `StatusChip(kind)` covering all five |
| Tag (Edited / Cancelled / Bounced / May not apply / Current) | `feature/tenants/…/EntryChipLabel.kt` and `MemberLabelChip.kt` | `Tag(kind)` |
| Banner (warn / error / info / note) | `Notice` exists. The sync errors, archived states and offline banner use red `Text`, `ArchivedBanner` or `ArchivedLabel` instead. | Make `Notice` the only banner and delete the others |
| TenantStrip | Three header cards (2.2) that draw a white outlined card on 3 lines | `TenantStrip(name, group, balance)` on a grey `surface-container` strip |
| ListRow (two-line row, trailing amount and chip) | `TenantRow`, `ArchivedTenantRow`, `MemberRow`, `InviteRow`, `TransferCandidateRow`, `AccountActionRow` | `RtListRow(title, subtitle, leading, trailing)`, with the feature rows as thin wrappers |
| Card / list card | 14 copies (2.2) | `RtCard`, `RtListCard` |
| Dialog | 16 hand-made `AlertDialog`s | `ConfirmDialog` |
| BottomSheet with label–value rows and action rows | `LedgerEntrySheet` and `MonthDetailSheet`, each with its own row style | `DetailSheet` + `DetailRow` + `SheetAction(isDestructive)` |
| EmptyState | 3 `Message()` helpers and `CenteredMessage` | `EmptyState(icon, title, body, action)` |
| SegmentedButton, FilterChips | `ChargeKindToggle`, `PaymentOrderToggle`, `PaymentMethodChips` and `PresetChips` each wrap M3 their own way | Keep the M3 components, but give them one shared style |
| Snackbar | `AppSnackbarHost` plus a custom `LateFeeSnackbar` in the bottom bar | Fine. Keep `LateFeeSnackbar` as a variant of one `RtSnackbar`. |

**Screens that should become (or be built from) components:**

- **BalanceCard** (`feature/tenants/…/BalanceCard.kt`) should own its actions. The design puts Log payment and Add charge inside the card, while the app draws them as a separate `EntryActionButtons` row.
- **The ledger row** in `LedgerPreviewCard` and `LedgerScreen` already shares `LedgerRow.kt`. Keep it that way, and make the canvas's Pending, Needs confirmation and Rejected rows (PR #9) variants of it, not new rows.
- **The permission editor** shared by `InviteMemberScreen` and `MemberPermissionsScreen` (3).
- **The duplicate-warning dialog** shared by payments and charges (2.2).
- **The Archived row** shared by Home and Group (2.2).
- **The offline banner** (PR #9) belongs in core next to `Notice`, not in each screen.

**Design tokens in code:** the Compose theme defines only colors. Spacing, radii and type are literals: the feature code has 87 × `16.dp`, 65 × `8.dp` and 61 × `12.dp`, and the theme passes no custom `Typography` or `Shapes`. **Fix:** add `RentTrackerTheme.spacing` (`space1`…`space8`), set `Shapes` (extraSmall 6, small 8, medium 12, large 16, extraLarge 28) and name the type roles the design uses (`amountL`, `headline` and so on) as a `Typography` extension. Then replace the literals screen by screen.

### 6.2 Where the app doesn't match the design (the canvas is the reference)

| Sev | Area | Canvas (Rent Tracker — Screens) | App | Files |
|---|---|---|---|---|
| High | Calendar fills | Pale containers: Paid `#CDEBD0` / `#1E5128`, Partial `#FFE0A3` / `#5A3B00`, Upcoming `#E9EFEE` | Solid fills: Paid `#2E7D32` with white text, Partial `#FFC233`, Owed `#C62828`. These match neither design system. | `core/designsystem/theme/CalendarColors.kt` |
| High | Owes / Paid up chips | Owes `#FFE0A3` / `#5A3B00`, Paid up `#CDEBD0` / `#1E5128` | Owes reuses the calendar `partial` (`#FFC233`) and Paid up reuses `prepaid` (`#C8EBC4`), so chip colors change whenever the calendar's do | `core/designsystem/components/TenantStateChip.kt:54-67` |
| Medium | Fee marker | Absolute top-right "Fee $50", white with an error border, radius 6 | Stacked under the month name | `core/designsystem/components/CalendarSquare.kt:86-89` |
| Medium | Current month | 2px `#8A5A00` outline | Not shown | `CalendarSquare.kt`, `feature/tenants/…/CalendarGrid.kt` |
| Medium | Month square | Radius 10, height 60 | `shapes.small` (8), min height 60 | `CalendarSquare.kt` |
| Medium | Balance card actions | Inside the card: Log payment (filled) and Add charge (tonal) | Separate row: `Button` and `OutlinedButton` | `feature/tenants/…/BalanceCard.kt`, `EntryActionButtons.kt` |
| Medium | Tenant balance size | 30px (`amount-l`) | `headlineMedium` (28sp) | `BalanceCard.kt:44` |
| Medium | Editor header | Grey `TenantStrip` on one row: "Apt 3B · Owes $550.00" | White outlined card on 3 lines (name, group, balance) | `ChargeHeaderCard.kt`, `PaymentHeaderCard.kt` |
| Medium | Errors and sync state | `Banner` (error or warn container, with an icon) | Plain red `Text` | `HomeScreen.kt` (syncError), `TenantScreen.kt` (TenantSections), `SyncErrorText.kt` |
| Medium | Search | Search bar on Home, search field on Group, a Search results screen | Not built, and not in the spec | (spec gap, 5.5) |
| Low | Card border | `#DAE4E3` (`border-card`) | `outlineVariant` `#BEC9C8`, so the hairlines are darker | every card (2.2) |
| Low | Icons | 2px-stroke outline SVGs with round caps | Material Icons (`ErrorOutline`, `Schedule`, `Check`) | all screens |
| Low | Home avatar | 32px solid `primary` with white initial | `AccountButton` / `InitialsAvatar` defaults to `secondaryContainer` | `feature/home/…/AccountButton.kt`, `core/designsystem/components/InitialsAvatar.kt` |
| Low | Account screen | `Account.dc.html` | Close, after M8 trimmed it. Re-check against the canvas once the app-lock and notification rows are settled. | `feature/account/…` |

---

## 7. Design docs: three overlapping sources, and how to merge them

### 7.1 What exists

| # | Source | Kind | Last change | What it holds |
|---|---|---|---|---|
| A | **"Rent Tracker"**, https://claude.ai/artifact/RnysvSXyvSWschcxKL9tGm | Design System | 2026-11-12 | 33 color tokens in **light and dark**, M3 type scale names, 6 spacing steps, 5 radii. Components: `CalendarSquare` (all **7** states) and `StatusChip`. **Screens shown as components**: `HomeScreen`, `GroupScreen` and `TenantScreen` previews. Material Symbols Rounded icons. |
| B | **"Rent Tracker UI"**, https://claude.ai/artifact/25Hfu7vh6MwYYqFyk4uECh | Design System | 2026-11-14 | Built from canvas C. 33 color tokens, **light only**, with M3-ish names plus custom ones (`surface-card`, `border-card`, `warning-*`, `success-*`). Custom type names, 7 radii, 5 shadows, 4 sizes. **29 components** with READMEs (Button, TopAppBar, StatusChip, Tag, Banner, LedgerRow, CalendarMonth, BottomSheet and so on). Custom stroke icons. Says the dark theme and 4 calendar states are "not designed yet". |
| C | **"Rent Tracker — Screens"**, https://claude.ai/artifact/QPeodhG39jRLGvYsJuQ3Ut | Design canvas | 2026-11-15 | 107 artboards on 5 pages (Core, Auth & account, Payments, Sharing & permissions, Offline & sync), all with **inline hex values** and no design system installed (`designSystems: []`). Dark variants exist only on the Offline page. |

Also around:

- An empty **"Design System"** artifact (https://claude.ai/artifact/BztLuGKVxRjsJewvUZuHnZ, no content).
- A stale local copy at `docs/designs/project/` in the main checkout (untracked). It has 103 files that differ from the canvas: it has `TenantArchived`, `TenantCredit`, `TenantOwes` and `TenantPaidUp`, but lacks `Account`, `Code`, `Name`, `SignIn`, `SignOut`, `Search`, `LogPayment` and `Duplicate`.
- The words in `docs/specs/tech/12-ui-design-system.md`.

### 7.2 Where they conflict

| Topic | A "Rent Tracker" | B "Rent Tracker UI" | C canvas | App |
|---|---|---|---|---|
| Dark mode | Full token set | "Not designed yet" | Offline page only, with its own hexes (for example cards `#1A2121`, borders `#2E3535`) | Full M3 dark scheme |
| Card | `surface-container-low` `#EEF5F4` + `outline-variant`, radius 12 | `surface-card` `#FFFFFF` + `border-card` `#DAE4E3`, radius 16 | = B | White + `#BEC9C8`, radius 16 |
| State colors | `late-` / `owes-` / `paid-container` (`#FFDEA6`, `#C4EFC0`) | `error-` / `warning-` / `success-container` (`#FFE0A3`, `#CDEBD0`) | = B | Mixed (6.2) |
| Calendar fills | **Solid** (`state-paid` `#2E6B30`, white text) | **Pale** containers | = B | Solid, with a third set of hexes |
| Calendar states | 7 states, fee bottom-right "Fee" | 4 drawn, fee top-right "Fee $50", current-month outline | = B | 7 states, fee under the name, no outline |
| Chip | 28px, 13px text | 24px, 12px text | = B | ~24dp, `labelMedium` |
| Icons | Material Symbols Rounded, filled for states | Custom 2px stroke | = B | Material Icons |
| Type names | M3 (`display-small`, `headline-small`…) | Custom (`amount-xl`, `amount-l`, `headline`, `dialog-title`, `caption`, `tag`) | Inline sizes | M3 defaults |
| Copy | "Archived debt $2,400.00" | | "Archived tenants owe $2,400.00" | Matches C |
| Obsolete content | History icon, Trash in the group menu (replaced by the bank-style ledger on 2026-11-11) | | | |

### 7.3 Merge plan: one design system, three layers

Make **B ("Rent Tracker UI") the one system**, because it is the most complete and the canvas already follows it. Fold A's dark theme and calendar states into it, then retire A. After that, the canvas only *uses* the system and the app *implements* it.

1. **Primitives (tokens) in B's `tokens.json`.**
   - Keep B's names, but rename the few that aren't M3 so they map 1:1 to Compose: `surface-card` → `surface-container-lowest`, and `divider` → `outline-variant` at reduced weight, or keep `divider` as an extra token. Add `warning`, `success` and their `on-*` and `-container` versions as a `RentTrackerTheme.extendedColors` in Compose.
   - Add a **dark** theme. Start from A's dark values and the canvas's Offline-dark hexes, and keep one value per token. Check 4.5:1 contrast in both themes.
   - Decide the calendar once: either **pale containers (B/C)** or **solid fills (A/app)**. Recommendation: pale containers, since they match the chips and banners and are what the user approved on the canvas. Then add tokens for the 4 states B lacks (Owed, Due-not-late, Prepaid, Not a tenant) using A's state table and icons.
   - Spacing `space-1…8`, radii `tag 6 / sm 8 / month 10 / md 12 / lg 16 / xl 28 / pill`, shadows, and sizes (`touch-target 48`, `field 56`, `row 72`, `appbar 64`).
   - Choose **one icon set**. Recommendation: Material Symbols Rounded (A), because Compose ships it and the custom strokes in B/C would have to be redrawn as vectors. Otherwise export B's stroke icons to `composeResources/drawable` as an icon set.
2. **Styles (components) in B's `components/`.** Keep B's 29 components. Add A's `CalendarSquare` state table into `CalendarMonth`, and rename it to one name used in both B and the app (`CalendarSquare`, the app's name). For each component, the README lists the variants that section 6.1 needs (Button variants, StatusChip kinds, Tag kinds, Banner kinds, ListRow slots), and every README names the Compose function that implements it.
3. **Screens on the canvas C.** Install B on the canvas (`designSystems` record and `ds/<folder>/tokens.json`), so the Theme menu shows token names instead of bare hexes. Replace the inline hexes page by page, starting with the Core page, then Payments, Sharing and Offline. Make each screen's light and dark variants come from the theme instead of separate `*Dark` boards, or keep the dark boards and note on each that it is generated from the same tokens. Remove the duplicate boards (`SignOut` vs `SignOutPending`; `Duplicate` vs `SheetDuplicate`, if they show the same dialog).
4. **Retire the rest.**
   - Move A's screen previews (Home, Group, Tenant) to an `archived/` section of B or delete them. They show obsolete History and Trash UI and old copy.
   - Delete or repurpose the empty "Design System" artifact.
   - Delete the stale local `docs/designs/project/` copy, or regenerate it from the canvas and say in a README that the canvas is the source.
   - Point `docs/specs/tech/12-ui-design-system.md` at B and C by URL, and drop any visual detail from the spec that B now owns.
5. **Keep the app in sync.** Generate `Color.kt`, `CalendarColors.kt`, the spacing, shapes and typography from B's `tokens.json` (a small script next to `tools/codegen`). Add the screenshot tests from section 4 so any drift shows up in CI.

---

## 8. The "·" separator

**Proposed rule.** Use "·" only to join **two short, independent facts on one secondary line** (a list-row subtitle or an app-bar subtitle), such as "Dana Whitfield · Due Apr 1". Don't use it:

- as a label suffix ("Note · optional"),
- at the start of a string ("· You"),
- inside a title ("Payment · Zelle"),
- to chain three or more parts,
- or as punctuation in a sentence.

Screen readers handle it inconsistently: some read it as "dot" and some skip it. Each use should be checked with TalkBack.

### 8.1 Designs (canvas C): 57 distinct strings, 286 uses

| Text | Boards | Keep? | Replace with |
|---|---|---|---|
| "Method · optional", "Note · optional" (label suffix) | 20 (AddCharge*, Dup*, Edit*, LogPayment*, Notify*, Off*) | Change | "Note (optional)", or mark only required fields |
| "· optional, last used: Zelle" | 10 | Change | Label "Method (optional)", helper "Last used: Zelle" |
| "Note · required", "Reason · required" | 2 (AddChargeCustom, CancelEntry) | Change | "Reason (required)" |
| "Offline · last updated 40 min ago / 2h ago" | 20 (Off*, Pending*, Notify*) | Change | "Offline. Last updated 40 min ago." |
| "Payment · Zelle / Check / Bank transfer" (ledger titles) | 19 | Change | "Zelle payment" / "Check payment" / "Payment" when there is no method |
| "Payment · Apt 3B" (sheet title) | 14 (Sheet*, Rejected*, CancelPending*) | Change | "Payment for Apt 3B" |
| "Rent · March / February / January" | 19 | Change | "March rent" |
| "Late fee · for February / January" | 12 | Change | "February late fee" (the canvas's own MonthFeb already says "Late fee for February") |
| "6 tenants · 2 late", "4 tenants · none late", "3 tenants · 1 late" | 16 | **Keep** | two counts, one line |
| "Archived (3) · Owes $2,400.00" | 9 | Change | "3 archived tenants owe $2,400.00", matching Home's "Archived tenants owe …". This needs the spec line in area 12 updated too. |
| "Marcus Lee · Due Apr 1" and the other tenant-row subtitles (9 names) | 9–10 each | **Keep** | |
| "$1,500.00 a month · due the 1st" | 18 | Change | "$1,500.00 a month, due the 1st" |
| "· held separately, not part of the balance" | 9 | Change | Supporting line under the deposit: "Held separately. Not part of the balance." |
| "by you · Mar 4" (edited line in sheets) | 5 | Change | "Edited Mar 4 by you" (same pattern as "Logged Mar 2 by you") |
| "Need_Not · You" (member row) | 5 | Change | "Need_Not (you)", or a "You" tag |
| "Bookkeeper · Expires Mar 16", "Viewer · Expires Mar 22" | 10 | **Keep** | |
| "$50.00 suggested · saved as a separate charge" | 4 | Change | "Suggested $50.00. Saved as a separate charge." |
| "Suggested amount · dated today, Mar 10, 2026 · saved with the payment" | 2 | Change | "Suggested amount. Dated today, Mar 10, 2026, and saved with the payment." |
| "Custom debt · added by you" | 4 | Change | "Added by you", the same as late-fee rows ("Added by Sam Rivera") |
| "Mar 2 payment · Zelle", "Feb 4 payment · Check" (month detail) | 5 | Change | "Mar 2 Zelle payment" |
| "Received Jan 18 · logged Jan 24 by you" | 3 | Change | "Received Jan 18, logged Jan 24 by you" |
| "Late · no late fee added" | 3 | Change | Title "Late", body "No late fee added." |
| "Late fee for February · unpaid" | 1 | Change | Title "February late fee", plus an "Unpaid" chip |
| "Apt 2A · 1st Street", "Apt 3B · 1st Street", "Apt 4C · 1st Street" (app-bar subtitle) | 6 | **Keep** | |
| "3 tenants · archived Jan 5, 2026" | 1 | **Keep** | |
| "1st Street · Dana Whitfield" and the other search results | 3 | **Keep** | |
| "Payment · Mar 10 · Sam Rivera · $550.00", "Payment · Mar 2 · Check · Sam Rivera", "Late fee for March · Mar 8 · Sam Rivera", "Mar 9 · Zelle · Sam Rivera", "Mar 8 · Sam Rivera", "Mar 2 · Zelle · you" (duplicate and changed dialogs) | 8 | Change | Two lines per match: "Check payment, Mar 2" and "Logged by Sam Rivera", amount on the right |

Design-system docs that teach the pattern also need editing. B's README has "Note · optional" under Content fundamentals, and B's `body` type sample is "Dana Whitfield · Due Apr 1" (that one is fine). A's README and `body-medium` sample use "Owes $550.00 · Next due Dec 1", and A's Home preview has "… invited you as Bookkeeper · Expires Nov 24".

### 8.2 App strings: 49 lines in `shared/src/commonMain/composeResources/values/`

| String name(s) | Text | Keep? |
|---|---|---|
| `payment_editor_method_optional`, `payment_editor_note`, `charge_editor_note_optional`, `group_settings_late_fee` | "· optional", "Note · optional", "Suggested late fee · optional" | Change → "(optional)" |
| `payment_editor_method_last_used` | "· optional, last used: %1$s" | Change → helper text |
| `charge_editor_note_custom`, `ledger_cancel_reason_label` | "Note · required", "Reason · required" | Change → "(required)" |
| `payment_editor_late_fee_hint` | "Suggested amount · dated today, %1$s · saved with the payment" | Change → sentence |
| `ledger_payment_title_*` (6) | "Payment · Cash/Check/Zelle/Venmo/Bank transfer/Other" | Change → "Cash payment"… (use "Payment" for Other) |
| `ledger_rent_title` | "Rent · %1$s" | Change → "%1$s rent" |
| `ledger_late_fee_title` | "Late fee · for %1$s" | Change → "%1$s late fee" |
| `ledger_payment_received_you` / `_named` | "Received %1$s · logged %2$s by …" | Change → comma |
| `ledger_custom_added_you` / `_named` | "Custom debt · added by …" | Change → "Added by …" |
| `ledger_edited_you` / `_named` | "by you · %1$s" | Change → "Edited %1$s by you" |
| `ledger_cancelled_you` / `_named` | "%1$s · you" | Change → "Cancelled %1$s by you" |
| `ledger_subtitle` | "%1$s · %2$s" (tenant · group, app bar) | **Keep** |
| `calendar_payment_*` (6) | "%1$s payment · Cash/…" | Change → "%1$s Zelle payment" |
| `calendar_late_title` | "Late · no late fee added" | Change |
| `calendar_fee_title_unpaid` / `_paid` | "Late fee for %1$s · unpaid/paid" | Change → title plus chip |
| `charge_duplicate_line` | "%1$s · %2$s %3$s" | Change → two lines |
| `members_you` | "· You" | Change → "(you)" |
| `members_invite_detail` | "%1$s · Expires %2$s" | **Keep** |
| `home_tenant_count_none_late`, `group_tenant_count_late`, `group_tenant_count_none_late` (plurals) | "%1$d tenants · none late" / "· %2$d late" | **Keep** |
| `archived_groups_summary` (plural) | "%1$d tenants · archived %2$s" | **Keep** |
| `group_archived_owed` | "Archived (%1$d) · Owes %2$s" | Change (with the spec) |
| `group_tenant_contact_and_due` | "%1$s · Due %2$s" | **Keep** |
| `tenant_rent_term_summary` | "%1$s a month · due the %2$s" | Change → comma |
| `tenant_details_deposit_note` | "· held separately, not part of the balance" | Change → supporting line |

The separator also appears in code, not in strings: `feature/payments/presentation/components/DuplicateDialog.kt:20` declares `private const val PART_SEPARATOR = " · "`, which joins parts at runtime. Move it into string resources, or better, drop it with the two-line layout above. The other "·" uses in Kotlin are hard-coded `UiText.Plain(...)` text in `@Preview` functions (`HomeScreen.kt:283`, `GroupScreen.kt:315-329`, `TenantRow.kt:68`, `TenantScreen.kt:457-463`, `ArchivedGroupCard.kt:70`, `ArchivedGroupsScreen.kt:179`), and those should follow whatever the strings become.

**Totals:** of the 57 design strings, 21 stay and 36 change. Of the 49 app string lines, 11 stay and 38 change.

---

## 9. Suggested order of cleanup passes

1. **Design source of truth (7.3, steps 1–2):** settle the calendar fills, the icon set and the dark tokens in B. This decides the next two passes.
2. **Core design system in code (6.1):** theme tokens (spacing, shapes, typography, extended colors), then `RtCard`, `RtListRow`, `StatusChip`, `Tag`, `Notice`-as-banner, `ConfirmDialog`, `EmptyState`, `TenantStrip` and `EditorTopBar(isWorking)`. Delete the feature copies (2.2). Add screenshot tests as you go (4).
3. **Fix the visual drift (6.2)** using the new components.
4. **Copy pass (8):** rewrite the strings and the canvas text together.
5. **Typed contract (5.1, 2.1, 2.3):** codegen for shared validators, function names, error codes and details, then `TypedBackend<E>`. Delete the `*BackendCalls` copies, the error-code constants and `PaymentMethodDetails`.
6. **Server text (5.2)** and **input limits (5.3)**.
7. **Small tidy-ups:** dead code (2.4), `ConvexBackend` placement and its test (5.4, 4), and the spec gaps (5.5).
