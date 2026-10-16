import { TestBed } from "@angular/core/testing";
import { provideHttpClient, HttpErrorResponse } from "@angular/common/http";
import {
  provideHttpClientTesting,
  HttpTestingController,
} from "@angular/common/http/testing";
import { Api, Auth, errorMessage } from "./api";
describe("Browser API security", () => {
  let api: Api, http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(Api);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it("obtains a CSRF token before every mutation and never sends a bearer credential", () => {
    api
      .mutate("PUT", "/bff/users/me", { displayName: "Updated", version: 0 })
      .subscribe();
    http
      .expectOne("/bff/csrf")
      .flush({ headerName: "X-CSRF-TOKEN", token: "test-csrf" });
    const req = http.expectOne("/bff/users/me");
    expect(req.request.headers.get("X-CSRF-TOKEN")).toBe("test-csrf");
    expect(req.request.headers.has("Authorization")).toBe(false);
    expect(req.request.body.version).toBe(0);
    req.flush({});
  });
  it("clears user state when the server session expires", () => {
    const auth = TestBed.inject(Auth);
    auth.load().subscribe((value) => expect(value).toBeNull());
    http
      .expectOne("/bff/session")
      .flush({}, { status: 401, statusText: "Unauthorized" });
    expect(auth.user()).toBeNull();
  });
  it("describes stale updates without exposing server internals", () => {
    expect(errorMessage(new HttpErrorResponse({ status: 409 }))).toContain(
      "Reload",
    );
  });
});
