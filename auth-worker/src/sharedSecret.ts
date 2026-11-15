const encoder = new TextEncoder();

export function matchesSharedSecret(given: string | null, expected: string | undefined): boolean {
  if (given === null || expected === undefined || expected === "") {
    return false;
  }
  const givenBytes = encoder.encode(given);
  const expectedBytes = encoder.encode(expected);
  let difference = givenBytes.length ^ expectedBytes.length;
  for (let index = 0; index < expectedBytes.length; index++) {
    difference |= (givenBytes[index] ?? 0) ^ (expectedBytes[index] ?? 0);
  }
  return difference === 0;
}
