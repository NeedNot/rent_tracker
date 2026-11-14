/* eslint-disable */
/**
 * Generated `api` utility.
 *
 * THIS CODE IS AUTOMATICALLY GENERATED.
 *
 * To regenerate, run `npx convex dev`.
 * @module
 */

import type * as access from "../access.js";
import type * as accounts from "../accounts.js";
import type * as appError from "../appError.js";
import type * as documentValidators from "../documentValidators.js";
import type * as editConflict from "../editConflict.js";
import type * as engine_allocation from "../engine/allocation.js";
import type * as engine_balance from "../engine/balance.js";
import type * as engine_calendarDate from "../engine/calendarDate.js";
import type * as engine_calendarMonths from "../engine/calendarMonths.js";
import type * as engine_charges from "../engine/charges.js";
import type * as engine_iterables from "../engine/iterables.js";
import type * as engine_lateFees from "../engine/lateFees.js";
import type * as engine_lateness from "../engine/lateness.js";
import type * as engine_ledgerInput from "../engine/ledgerInput.js";
import type * as engine_rentPeriods from "../engine/rentPeriods.js";
import type * as engine_tenantLedger from "../engine/tenantLedger.js";
import type * as groupSummary from "../groupSummary.js";
import type * as groupTenantSummaries from "../groupTenantSummaries.js";
import type * as groups from "../groups.js";
import type * as home from "../home.js";
import type * as inputValidation from "../inputValidation.js";
import type * as localDate from "../localDate.js";
import type * as rentTermRules from "../rentTermRules.js";
import type * as rentTerms from "../rentTerms.js";
import type * as tenantDetails from "../tenantDetails.js";
import type * as tenantLedgerAdapter from "../tenantLedgerAdapter.js";
import type * as tenantLedgerRows from "../tenantLedgerRows.js";
import type * as tenantScope from "../tenantScope.js";
import type * as tenantSummary from "../tenantSummary.js";
import type * as tenants from "../tenants.js";
import type * as validators from "../validators.js";

import type {
  ApiFromModules,
  FilterApi,
  FunctionReference,
} from "convex/server";

declare const fullApi: ApiFromModules<{
  access: typeof access;
  accounts: typeof accounts;
  appError: typeof appError;
  documentValidators: typeof documentValidators;
  editConflict: typeof editConflict;
  "engine/allocation": typeof engine_allocation;
  "engine/balance": typeof engine_balance;
  "engine/calendarDate": typeof engine_calendarDate;
  "engine/calendarMonths": typeof engine_calendarMonths;
  "engine/charges": typeof engine_charges;
  "engine/iterables": typeof engine_iterables;
  "engine/lateFees": typeof engine_lateFees;
  "engine/lateness": typeof engine_lateness;
  "engine/ledgerInput": typeof engine_ledgerInput;
  "engine/rentPeriods": typeof engine_rentPeriods;
  "engine/tenantLedger": typeof engine_tenantLedger;
  groupSummary: typeof groupSummary;
  groupTenantSummaries: typeof groupTenantSummaries;
  groups: typeof groups;
  home: typeof home;
  inputValidation: typeof inputValidation;
  localDate: typeof localDate;
  rentTermRules: typeof rentTermRules;
  rentTerms: typeof rentTerms;
  tenantDetails: typeof tenantDetails;
  tenantLedgerAdapter: typeof tenantLedgerAdapter;
  tenantLedgerRows: typeof tenantLedgerRows;
  tenantScope: typeof tenantScope;
  tenantSummary: typeof tenantSummary;
  tenants: typeof tenants;
  validators: typeof validators;
}>;

/**
 * A utility for referencing Convex functions in your app's public API.
 *
 * Usage:
 * ```js
 * const myFunctionReference = api.myModule.myFunction;
 * ```
 */
export declare const api: FilterApi<
  typeof fullApi,
  FunctionReference<any, "public">
>;

/**
 * A utility for referencing Convex functions in your app's internal API.
 *
 * Usage:
 * ```js
 * const myFunctionReference = internal.myModule.myFunction;
 * ```
 */
export declare const internal: FilterApi<
  typeof fullApi,
  FunctionReference<any, "internal">
>;

export declare const components: {};
