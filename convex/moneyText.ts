const CENTS_PER_DOLLAR = 100;

export function formatMoney(cents: number): string {
  const dollars = Math.floor(cents / CENTS_PER_DOLLAR).toLocaleString("en-US");
  const remainder = String(cents % CENTS_PER_DOLLAR).padStart(2, "0");
  return `$${dollars}.${remainder}`;
}
