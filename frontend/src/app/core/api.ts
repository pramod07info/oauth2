import { inject, Injectable, signal } from "@angular/core";
import {
  HttpClient,
  HttpErrorResponse,
  HttpBackend,
  HttpInterceptorFn,
} from "@angular/common/http";
import {
  catchError,
  map,
  Observable,
  of,
  switchMap,
  tap,
  defer,
  firstValueFrom,
  finalize,
  shareReplay,
  throwError,
} from "rxjs";
import { Router } from "@angular/router";
export interface User {
  id: string;
  email: string;
  displayName: string;
  role: "USER" | "ADMIN";
  enabled: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}
export interface Page {
  content: User[];
  page: number;
  size: number;
  totalElements: number;
}
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 401)
      return "Your session has expired. Please sign in again.";
    if (error.status === 403)
      return "You do not have permission to perform this action.";
    if (error.status === 409)
      return "This record changed or the email already exists. Reload and try again.";
    if (error.status === 0)
      return "Cannot reach the server. Check that the backend is running.";
    return (
      error.error?.detail ||
      "The request could not be completed. Please try again."
    );
  }
  return "Something went wrong. Please try again.";
}

@Injectable({ providedIn: "root" })
export class Api {
  private http = inject(HttpClient);
  get<T>(path: string): Observable<T> {
    return this.http.get<T>(path);
  }
  mutate<T>(method: string, path: string, body?: unknown): Observable<T> {
    return this.http.request<T>(method, path, { body });
  }
}
export interface Tokens {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}
@Injectable({ providedIn: "root" })
export class Auth {
  private raw = new HttpClient(inject(HttpBackend));
  private http = inject(HttpClient);
  private router = inject(Router);
  private access = "";
  private inFlight: Observable<User> | null = null;
  private cookieBusy = false;
  private cookieDone: Promise<void> = Promise.resolve();
  user = signal<User | null>(null);
  expiresAt = signal("");
  token() {
    return this.access;
  }
  private accept(tokens: Tokens) {
    this.access = tokens.accessToken;
    this.user.set(tokens.user);
    this.expiresAt.set(tokens.expiresAt);
    return tokens.user;
  }
  private cookieRequest<T>(path: string, body: unknown = {}) {
    return this.raw
      .get<{ headerName: string; token: string }>("/api/auth/csrf")
      .pipe(
        switchMap((csrf) =>
          this.raw.post<T>(path, body, {
            headers: { [csrf.headerName]: csrf.token },
          }),
        ),
      );
  }
  private locked<T>(operation: () => Observable<T>): Observable<T> {
    return defer(async (): Promise<T> => {
      const run = async () => await firstValueFrom(operation());
      if (typeof navigator !== "undefined" && navigator.locks)
        return await navigator.locks.request("identity-refresh", run);
      while (this.cookieBusy) await this.cookieDone;
      this.cookieBusy = true;
      let release!: () => void;
      this.cookieDone = new Promise<void>((resolve) => (release = resolve));
      try {
        return await run();
      } finally {
        this.cookieBusy = false;
        release();
      }
    });
  }
  login(email: string, password: string) {
    return this.locked(() =>
      this.cookieRequest<Tokens>("/api/auth/login", { email, password }).pipe(
        map((tokens) => this.accept(tokens)),
      ),
    );
  }
  load(): Observable<User | null> {
    const request = this.access
      ? this.http
          .get<User>("/api/users/me")
          .pipe(tap((user) => this.user.set(user)))
      : this.refresh();
    return request.pipe(
      catchError(() => {
        this.clear();
        return of(null);
      }),
    );
  }
  signIn() {
    this.router.navigateByUrl("/sign-in");
  }
  clear() {
    this.access = "";
    this.user.set(null);
    this.expiresAt.set("");
  }
  logout() {
    return this.locked(() =>
      this.cookieRequest<void>("/api/auth/logout").pipe(
        tap(() => this.clear()),
      ),
    );
  }
  refresh(): Observable<User> {
    if (!this.inFlight) {
      this.inFlight = this.locked(() =>
        this.cookieRequest<Tokens>("/api/auth/refresh").pipe(
          map((tokens) => this.accept(tokens)),
          catchError((error) => {
            this.clear();
            return throwError(() => error);
          }),
        ),
      ).pipe(
        finalize(() => (this.inFlight = null)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.inFlight;
  }
}
export const tokenInterceptor: HttpInterceptorFn = (request, next) => {
  const protectedApi =
    /^\/api\/users(?:[/?]|$)/.test(request.url) ||
    request.url === "/api/auth/token-info";
  if (!protectedApi) return next(request);
  const auth = inject(Auth),
    router = inject(Router);
  const fail = (error: unknown) => {
    auth.clear();
    router.navigateByUrl("/sign-in");
    return throwError(() => error);
  };
  const send = (token: string) =>
    next(
      token
        ? request.clone({ setHeaders: { Authorization: "Bearer " + token } })
        : request,
    );
  const retry = () =>
    send(auth.token()).pipe(
      catchError((error) =>
        error.status === 401 ? fail(error) : throwError(() => error),
      ),
    );
  if (auth.token() && Date.parse(auth.expiresAt()) <= Date.now())
    return auth.refresh().pipe(catchError(fail), switchMap(retry));
  const sent = auth.token();
  return send(sent).pipe(
    catchError((error) => {
      if (error.status !== 401) return throwError(() => error);
      if (sent && sent !== auth.token() && auth.token()) return retry();
      return auth.refresh().pipe(catchError(fail), switchMap(retry));
    }),
  );
};
