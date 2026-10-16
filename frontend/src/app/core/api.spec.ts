import { TestBed } from "@angular/core/testing";
import {
  provideHttpClient,
  withInterceptors,
  HttpErrorResponse,
  HttpClient,
} from "@angular/common/http";
import {
  provideHttpClientTesting,
  HttpTestingController,
} from "@angular/common/http/testing";
import { provideRouter, Router } from "@angular/router";
import { firstValueFrom } from "rxjs";
import { Api, Auth, errorMessage, tokenInterceptor, Tokens } from "./api";
const user = {
  id: "1",
  email: "member@example.com",
  displayName: "Member",
  role: "USER" as const,
  enabled: true,
  version: 0,
  createdAt: "2026-01-01",
  updatedAt: "2026-01-01",
};
const tokens = (token: string): Tokens => ({
  accessToken: token,
  tokenType: "Bearer",
  expiresAt: "2099-01-01T00:00:00Z",
  user,
});
describe("JWT browser authentication", () => {
  let http: HttpTestingController, auth: Auth, api: Api;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([tokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(Auth);
    api = TestBed.inject(Api);
    vi.spyOn(TestBed.inject(Router), "navigateByUrl").mockResolvedValue(true);
  });
  afterEach(() => http.verify());
  function csrf() {
    http
      .expectOne("/api/auth/csrf")
      .flush({ headerName: "X-CSRF-TOKEN", token: "csrf" });
  }
  async function signIn() {
    const result = firstValueFrom(auth.login(user.email, "password"));
    csrf();
    const request = http.expectOne("/api/auth/login");
    expect(request.request.headers.get("X-CSRF-TOKEN")).toBe("csrf");
    request.flush(tokens("old"));
    await result;
  }
  it("logs in with CSRF, keeps access in memory and attaches it only to resource APIs", async () => {
    await signIn();
    api.get("/api/users/me").subscribe();
    const req = http.expectOne("/api/users/me");
    expect(req.request.headers.get("Authorization")).toBe("Bearer old");
    req.flush(user);
  });
  it("shares one refresh across concurrent 401s and retries each request once", async () => {
    await signIn();
    const first = firstValueFrom(api.get("/api/users/me")),
      second = firstValueFrom(api.get("/api/users?size=1"));
    http
      .expectOne("/api/users/me")
      .flush({}, { status: 401, statusText: "Expired" });
    http
      .expectOne("/api/users?size=1")
      .flush({}, { status: 401, statusText: "Expired" });
    csrf();
    http.expectOne("/api/auth/refresh").flush(tokens("new"));
    await new Promise((resolve) => setTimeout(resolve, 0));
    const one = http.expectOne("/api/users/me"),
      two = http.expectOne("/api/users?size=1");
    expect(one.request.headers.get("Authorization")).toBe("Bearer new");
    expect(two.request.headers.get("Authorization")).toBe("Bearer new");
    one.flush(user);
    two.flush({});
    await Promise.all([first, second]);
  });
  it("does not refresh again if the retried resource also returns 401", async () => {
    await signIn();
    let status = 0;
    api.get("/api/users/me").subscribe({ error: (e) => (status = e.status) });
    http
      .expectOne("/api/users/me")
      .flush({}, { status: 401, statusText: "Expired" });
    csrf();
    http.expectOne("/api/auth/refresh").flush(tokens("new"));
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne("/api/users/me")
      .flush({}, { status: 401, statusText: "Revoked" });
    expect(status).toBe(401);
    expect(auth.token()).toBe("");
  });
  it("restores memory state from the refresh cookie after a reload", async () => {
    const result = firstValueFrom(auth.load());
    csrf();
    const refresh = http.expectOne("/api/auth/refresh");
    expect(refresh.request.headers.has("Authorization")).toBe(false);
    refresh.flush(tokens("restored"));
    expect(await result).toEqual(user);
    expect(auth.token()).toBe("restored");
  });
  it("does not attach a bearer token to external URLs or public diagnostics", async () => {
    await signIn();
    const client = TestBed.inject(HttpClient);
    client.get("https://example.com/report").subscribe();
    const external = http.expectOne("https://example.com/report");
    expect(external.request.headers.has("Authorization")).toBe(false);
    external.flush({});
    api.get("/oauth2/jwks").subscribe();
    const publicRequest = http.expectOne("/oauth2/jwks");
    expect(publicRequest.request.headers.has("Authorization")).toBe(false);
    publicRequest.flush({});
  });
  it("describes stale writes", () =>
    expect(errorMessage(new HttpErrorResponse({ status: 409 }))).toContain(
      "Reload",
    ));

  it("serializes logout behind an outstanding refresh so credentials cannot return after logout", async () => {
    await signIn();
    const refresh = firstValueFrom(auth.refresh());
    csrf();
    const refreshRequest = http.expectOne("/api/auth/refresh");
    const logout = firstValueFrom(auth.logout());
    http.expectNone("/api/auth/logout");
    refreshRequest.flush(tokens("new"));
    await new Promise((resolve) => setTimeout(resolve, 0));
    csrf();
    http.expectOne("/api/auth/logout").flush(null);
    await Promise.all([refresh, logout]);
    expect(auth.token()).toBe("");
    expect(auth.user()).toBeNull();
  });
  it("clears memory when a manual refresh fails", async () => {
    await signIn();
    const refresh = firstValueFrom(auth.refresh()).catch(() => null);
    csrf();
    http
      .expectOne("/api/auth/refresh")
      .flush({}, { status: 401, statusText: "Expired" });
    await refresh;
    expect(auth.token()).toBe("");
    expect(auth.user()).toBeNull();
  });
});
