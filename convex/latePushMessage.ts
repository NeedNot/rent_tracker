import { formatMoney } from "./moneyText";

export const LATE_PUSH_TITLE = "Rent is late";

export interface LateRentFacts {
  groupName: string;
  newlyLateCount: number;
  lateCount: number;
  owedCents: number;
  isWeeklyReminderDay: boolean;
}

export interface LatePushMessage {
  title: string;
  body: string;
}

export function latePushMessage(facts: LateRentFacts): LatePushMessage | null {
  const owed = `${formatMoney(facts.owedCents)} owed`;
  if (facts.newlyLateCount > 0) {
    const justLate = `${tenantCount(facts.newlyLateCount)} just became late`;
    return messageFor(facts.groupName, `${justLate} — ${facts.lateCount} late in total, ${owed}.`);
  }
  if (facts.isWeeklyReminderDay && facts.lateCount > 0) {
    const verb = facts.lateCount === 1 ? "is" : "are";
    return messageFor(facts.groupName, `${tenantCount(facts.lateCount)} ${verb} late, ${owed}.`);
  }
  return null;
}

function messageFor(groupName: string, summary: string): LatePushMessage {
  return { title: LATE_PUSH_TITLE, body: `${groupName}: ${summary}` };
}

function tenantCount(count: number): string {
  return count === 1 ? "1 tenant" : `${count} tenants`;
}
