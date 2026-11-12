const PRODUCT_NAME = "Rent Tracker";
const CODE_LIFETIME = "10 minutes";

export type EmailContent = {
  readonly subject: string;
  readonly text: string;
  readonly html: string;
};

export function signInCodeEmail(code: string): EmailContent {
  const expiry = `This code expires in ${CODE_LIFETIME} and can be used once.`;
  const ignore = "If you did not ask for this code, you can ignore this email.";
  return {
    subject: `${code} is your ${PRODUCT_NAME} sign-in code`,
    text: [`Your ${PRODUCT_NAME} sign-in code is ${code}`, expiry, ignore].join("\n\n"),
    html: [
      `<p>Your ${PRODUCT_NAME} sign-in code is</p>`,
      `<p style="font-size: 28px; font-weight: bold; letter-spacing: 4px;">${code}</p>`,
      `<p>${expiry}</p>`,
      `<p>${ignore}</p>`,
    ].join("\n"),
  };
}
