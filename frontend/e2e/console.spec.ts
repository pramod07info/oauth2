import { test, expect } from "@playwright/test";
const admin = {
  id: "admin-1",
  email: "admin@example.com",
  displayName: "Pramod Verma",
  role: "ADMIN",
  enabled: true,
  version: 0,
  createdAt: "2026-10-01T10:00:00Z",
  updatedAt: "2026-10-01T10:00:00Z",
};
const member = {
  ...admin,
  id: "member-1",
  email: "developer@example.com",
  displayName: "Developer",
  role: "USER",
};
test.beforeEach(async ({ page }) => {
  let person = { ...member };
  let owner = { ...admin };
  await page.route("**/api/**", async (route) => {
    const req = route.request(),
      path = new URL(req.url()).pathname;
    let body: unknown = {};
    if (path === "/api/auth/refresh")
      body = {
        user: owner,
        accessToken: "mock-access",
        expiresAt: "2099-01-01T00:00:00Z",
        tokenType: "Bearer",
      };
    else if (path === "/api/users/me" && req.method() === "GET") body = owner;
    else if (path === "/api/auth/csrf")
      body = { headerName: "X-CSRF-TOKEN", token: "mock-csrf" };
    else if (path === "/api/users")
      body = { content: [admin, person], page: 0, size: 20, totalElements: 2 };
    else if (path === "/api/users/member-1" && req.method() === "GET")
      body = person;
    else if (path === "/api/users/member-1" && req.method() === "PUT") {
      person = {
        ...person,
        ...req.postDataJSON(),
        version: person.version + 1,
      };
      body = person;
    } else if (path === "/api/users/member-1/permissions") {
      person = {
        ...person,
        ...req.postDataJSON(),
        version: person.version + 1,
      };
      body = person;
    } else if (path === "/api/auth/register") body = member;
    else if (path === "/api/users/me" && req.method() === "PUT")
      body = owner = { ...owner, ...req.postDataJSON(), version: 1 };
    if (path === "/api/auth/logout" || path === "/api/auth/refresh")
      expect(req.headers()["x-csrf-token"]).toBe("mock-csrf");
    if (path === "/api/auth/logout" || req.method() === "DELETE")
      return route.fulfill({ status: 204 });
    return route.fulfill({ json: body });
  });
});
test("admin overview and member permissions flow", async ({ page }) => {
  await page.goto("/dashboard");
  await expect(
    page.getByRole("heading", { name: "Good to see you, Pramod." }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Users & roles" }).click();
  await expect(
    page.getByRole("heading", { name: /All members/ }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Manage Developer" }).click();
  await page.getByLabel("Display name").fill("Updated Developer");
  await page.getByRole("button", { name: "Save profile", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Updated Developer", exact: true }),
  ).toBeVisible();
  await page.getByLabel("Account enabled").uncheck();
  await page.getByRole("button", { name: "Save permissions" }).click();
  await expect(page.getByText("Disabled", { exact: true })).toBeVisible();
  await page
    .getByRole("button", { name: "Delete account", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: "Yes, delete account" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
});
test("registration validates fields and shows completion", async ({ page }) => {
  await page.goto("/register");
  await page
    .getByRole("button", { name: "Create account", exact: false })
    .click();
  await expect(page.getByText("Enter your name, a valid email")).toBeVisible();
  await page.getByLabel("Full name").fill("Developer");
  await page.getByLabel("Email address").fill("developer@example.com");
  await page
    .getByLabel("Password", { exact: true })
    .fill("A-strong-password-2026!");
  await page
    .getByRole("button", { name: "Create account", exact: false })
    .click();
  await expect(page.getByText("Your account is ready.")).toBeVisible();
});
test("members cannot navigate to the administration screen", async ({
  page,
}) => {
  await page.route("**/api/auth/refresh", (r) =>
    r.fulfill({
      json: {
        user: member,
        accessToken: "mock-member",
        expiresAt: "2099-01-01T00:00:00Z",
        tokenType: "Bearer",
      },
    }),
  );
  await page.route("**/api/users/me", (r) => r.fulfill({ json: member }));
  await page.goto("/users");
  await expect(page).toHaveURL(/dashboard$/);
  await expect(page.getByRole("link", { name: "Users & roles" })).toHaveCount(
    0,
  );
});
test("mobile profile remains usable", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/profile");
  await page.getByLabel("Display name").fill("Updated Pramod");
  await page.getByRole("button", { name: "Save changes" }).click();
  await expect(page.getByText("Profile updated successfully.")).toBeVisible();
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth),
  ).toBeLessThanOrEqual(390);
});
