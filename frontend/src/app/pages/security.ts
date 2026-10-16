import { Component, inject, signal } from "@angular/core";
import { DatePipe, JsonPipe } from "@angular/common";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { Router } from "@angular/router";
import { Api, Auth, errorMessage } from "../core/api";
@Component({
  imports: [ReactiveFormsModule, DatePipe, JsonPipe],
  template: `<div class="page-heading">
      <div>
        <span class="eyebrow">ACCOUNT PROTECTION</span>
        <h1>Security</h1>
        <p>A few simple controls. A safer account.</p>
      </div>
      <span class="status active">Protected session</span>
    </div>
    @if (error()) {
      <div class="alert error" role="alert">{{ error() }}</div>
    }
    @if (message()) {
      <div class="alert success" role="status">{{ message() }}</div>
    }
    <div class="two-columns">
      <section class="panel">
        <div class="section-heading">
          <h2>Change password</h2>
          <span>◇</span>
        </div>
        <p class="muted">Choose a unique password you don't use elsewhere.</p>
        <form [formGroup]="form" (ngSubmit)="change()">
          <label
            >Current password<input
              type="password"
              formControlName="currentPassword"
              autocomplete="current-password" /></label
          ><label
            >New password<input
              type="password"
              formControlName="newPassword"
              autocomplete="new-password" /></label
          ><label
            >Confirm new password<input
              type="password"
              formControlName="confirm"
              autocomplete="new-password"
          /></label>
          <p class="field-hint">
            At least 12 characters, up to 72 UTF-8 bytes. Changing your password
            signs you out.
          </p>
          <button class="button primary" [disabled]="busy() || form.invalid">
            {{ busy() ? "Updating…" : "Update password" }}
          </button>
        </form>
      </section>
      <section class="panel">
        <div class="section-heading">
          <h2>Current session</h2>
          <span class="status active">Connected</span>
        </div>
        <dl class="details">
          <div>
            <dt>Authentication</dt>
            <dd>OAuth2 + PKCE</dd>
          </div>
          <div>
            <dt>Token expiry</dt>
            <dd>{{ auth.expiresAt() | date: "medium" }}</dd>
          </div>
          <div>
            <dt>Credential storage</dt>
            <dd>Server only</dd>
          </div>
        </dl>
        <div class="session-actions">
          <button
            class="button secondary"
            (click)="refresh()"
            [disabled]="busy()"
          >
            ↻ Refresh session</button
          ><button
            class="button secondary"
            (click)="inspect()"
            [disabled]="busy()"
          >
            Inspect session
          </button>
        </div>
        @if (info()) {
          <pre>{{ info() | json }}</pre>
        }
        <div class="security-note">
          Client secrets and tokens stay on the server. Your browser uses an
          HttpOnly session cookie.
        </div>
      </section>
    </div>`,
})
export class Security {
  auth = inject(Auth);
  api = inject(Api);
  router = inject(Router);
  busy = signal(false);
  error = signal("");
  message = signal("");
  info = signal<unknown>(null);
  form = inject(FormBuilder).nonNullable.group({
    currentPassword: ["", Validators.required],
    newPassword: ["", [Validators.required, Validators.minLength(12)]],
    confirm: ["", Validators.required],
  });
  change() {
    const v = this.form.getRawValue();
    if (v.newPassword !== v.confirm) {
      this.error.set("New passwords do not match.");
      return;
    }
    if (new TextEncoder().encode(v.newPassword).length > 72) {
      this.error.set("Password must be at most 72 UTF-8 bytes.");
      return;
    }
    this.busy.set(true);
    this.api
      .mutate("PUT", "/bff/users/me/password", {
        currentPassword: v.currentPassword,
        newPassword: v.newPassword,
      })
      .subscribe({
        next: () => {
          this.auth.clear();
          this.form.reset();
          this.router.navigateByUrl("/sign-in");
        },
        error: (e) => this.fail(e),
      });
  }
  refresh() {
    this.busy.set(true);
    this.error.set("");
    this.auth.refresh().subscribe({
      next: () => {
        this.busy.set(false);
        this.message.set("Your session has been refreshed.");
      },
      error: (e) => this.fail(e),
    });
  }
  inspect() {
    this.busy.set(true);
    this.api.get("/bff/token/info").subscribe({
      next: (v) => {
        this.info.set(v);
        this.busy.set(false);
      },
      error: (e) => this.fail(e),
    });
  }
  private fail(e: unknown) {
    this.busy.set(false);
    this.error.set(errorMessage(e));
  }
}
