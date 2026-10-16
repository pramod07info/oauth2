import { inject, Injectable, signal } from "@angular/core";
import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { catchError, map, Observable, of, switchMap, tap } from "rxjs";
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
export interface Session {
  user: User;
  expiresAt: string;
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
    return this.http
      .get<{ headerName: string; token: string }>("/bff/csrf")
      .pipe(
        switchMap((csrf) =>
          this.http.request<T>(method, path, {
            body,
            headers: { [csrf.headerName]: csrf.token },
          }),
        ),
      );
  }
}
@Injectable({ providedIn: "root" })
export class Auth {
  private api = inject(Api);
  user = signal<User | null>(null);
  expiresAt = signal("");
  load(): Observable<User | null> {
    return this.api.get<Session>("/bff/session").pipe(
      tap((s) => {
        this.user.set(s.user);
        this.expiresAt.set(s.expiresAt);
      }),
      map((s) => s.user),
      catchError(() => {
        this.clear();
        return of(null);
      }),
    );
  }
  signIn() {
    window.location.assign("/oauth2/authorization/identity-ui");
  }
  clear() {
    this.user.set(null);
    this.expiresAt.set("");
  }
  logout() {
    return this.api
      .mutate<void>("POST", "/bff/logout")
      .pipe(tap(() => this.clear()));
  }
  refresh() {
    return this.api
      .mutate<void>("POST", "/bff/token/refresh")
      .pipe(switchMap(() => this.load()));
  }
}
