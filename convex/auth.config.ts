import type { AuthConfig } from "convex/server";

const ISSUER_VARIABLE = "AUTH_ISSUER";

// Convex throws when auth config reads an unset variable, and neither `in` nor Object.keys can see whether it is set.
// A deployment without the variable (CI, codegen) must still push, so it gets no provider and rejects every token.
function configuredIssuer(): string | undefined {
  try {
    return process.env[ISSUER_VARIABLE];
  } catch (error) {
    if (error instanceof Error && error.message.includes(ISSUER_VARIABLE)) {
      return undefined;
    }
    throw error;
  }
}

function trustedProviders(issuer: string | undefined): AuthConfig["providers"] {
  if (issuer === undefined) {
    return [];
  }
  return [
    {
      type: "customJwt",
      applicationID: "rent-tracker",
      issuer,
      jwks: `${issuer}/api/auth/jwks`,
      algorithm: "ES256",
    },
  ];
}

export default { providers: trustedProviders(configuredIssuer()) } satisfies AuthConfig;
