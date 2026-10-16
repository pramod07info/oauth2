import { test, expect, Page } from "@playwright/test";
test.skip(
  !process.env["REAL_BACKEND"],
  "Requires the live backend with the documented development bootstrap admin.",
);
async function login(page: Page, email: string, password: string) {
  await page.goto("/sign-in");
  await page.getByLabel("Email address").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/dashboard$/, { timeout: 20000 });
}
test("direct JWT login, profile, refresh, password, admin roles and deletion", async ({
  page,
}) => {
  test.setTimeout(120000);
  const authorizeRequests: string[] = [];
  page.on("request", (request) => {
    if (request.url().includes("/oauth2/authorize"))
      authorizeRequests.push(request.url());
  });
  const email = `browser-${Date.now()}@example.com`,
    password = "Browser-Password-2026!",
    newPassword = "Browser-NewPassword-2026!";
  await page.goto("/register");
  await page.getByLabel("Full name").fill("Browser Test Member");
  await page.getByLabel("Email address").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page
    .getByRole("button", { name: "Create account", exact: false })
    .click();
  await expect(page.getByText("Your account is ready.")).toBeVisible();
  await login(page, email, password);
  await page.getByRole("link", { name: "My profile", exact: false }).click();
  await page.getByLabel("Display name").fill("Browser Updated Member");
  await page.getByRole("button", { name: "Save changes" }).click();
  await expect(page.getByText("Profile updated successfully.")).toBeVisible();
  await page.getByRole("link", { name: "Security", exact: true }).click();
  await page.getByRole("button", { name: "Refresh session" }).click();
  await expect(
    page.getByText("Your session has been refreshed."),
  ).toBeVisible();
  await page.getByRole("button", { name: "Inspect session" }).click();
  await expect(page.locator("pre")).toContainText('"active": true');
  await page.getByLabel("Current password").fill(password);
  await page.getByLabel("New password", { exact: true }).fill(newPassword);
  await page.getByLabel("Confirm new password").fill(newPassword);
  await page.getByRole("button", { name: "Update password" }).click();
  await expect(page).toHaveURL(/sign-in$/);
  await login(page, email, newPassword);
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page).toHaveURL(/sign-in$/);
  await login(
    page,
    process.env["E2E_ADMIN_EMAIL"] || "admin@example.com",
    process.env["E2E_ADMIN_PASSWORD"] || "LocalAdmin-Test2026!",
  );
  await page.reload();
  await expect(
    page.getByRole("heading", { name: /Good to see you/ }),
  ).toBeVisible();
  expect(
    await page.evaluate(() => localStorage.length + sessionStorage.length),
  ).toBe(0);
  await page.screenshot({
    path: "test-results/admin-overview.png",
    fullPage: true,
  });
  await page.getByRole("link", { name: "System status", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "UP", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Users & roles" }).click();
  await page
    .getByRole("link", { name: "Manage Browser Updated Member" })
    .click();
  await page.getByLabel("Workspace role").selectOption("ADMIN");
  await page.getByRole("button", { name: "Save permissions" }).click();
  await expect(page.getByText("Member updated successfully.")).toBeVisible();
  await page.getByLabel("Account enabled").uncheck();
  await page.getByRole("button", { name: "Save permissions" }).click();
  await expect(page.getByText("Disabled", { exact: true })).toBeVisible();
  await page
    .getByRole("button", { name: "Delete account", exact: true })
    .click();
  await page.getByRole("button", { name: "Yes, delete account" }).click();
  await expect(page).toHaveURL(/users$/);
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page).toHaveURL(/sign-in$/);
  expect(authorizeRequests).toHaveLength(0);
});
