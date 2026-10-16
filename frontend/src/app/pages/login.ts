import { Component, inject, signal } from "@angular/core";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { Auth, errorMessage } from "../core/api";
@Component({
  imports: [RouterLink, ReactiveFormsModule],
  template: `<div class="auth-layout">
    <section class="auth-story">
      <a class="brand" routerLink="/sign-in"
        ><span class="brand-mark">i</span>identity.</a
      >
      <div>
        <span class="eyebrow">YOUR WORKSPACE, PROTECTED</span>
        <h1>The right access.<br />For the right people.</h1>
        <p>
          A single place to manage your identity, protect your account, and keep
          your team moving.
        </p>
        <div class="auth-orbit">
          <span class="orbit-center">i</span
          ><span class="orbit-badge">✓ Verified identity</span
          ><span class="orbit-badge second">◇ Secure by design</span>
        </div>
      </div>
      <small>Your account, protected.</small>
    </section>
    <section class="auth-form">
      <div class="auth-form-inner">
        <span class="label-tag">IDENTITY CONSOLE</span>
        <h2>Welcome back.</h2>
        <p class="muted">Sign in to your secure workspace.</p>
        @if (failed) {
          <div class="alert error" role="alert">
            We couldn't complete sign-in. Please try again.
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <label
            >Email address<input
              type="email"
              formControlName="email"
              autocomplete="username"
              placeholder="you@example.com"
          /></label>
          <label
            >Password<input
              type="password"
              formControlName="password"
              autocomplete="current-password"
              placeholder="Your password"
          /></label>
          @if (form.touched && form.invalid) {
            <p class="field-error">Enter a valid email and your password.</p>
          }
          @if (error()) {
            <div class="alert error" role="alert">{{ error() }}</div>
          }
          <button
            class="button primary full"
            style="margin-top:24px"
            [disabled]="busy()"
          >
            {{ busy() ? "Signing in…" : "Sign in" }}
          </button>
          <div class="trust-line">
            Protected access with automatic token refresh
          </div>
        </form>
        <p class="auth-switch">
          New to the workspace? <a routerLink="/register">Create an account</a>
        </p>
      </div>
    </section>
  </div>`,
})
export class Login {
  auth = inject(Auth);
  router = inject(Router);
  failed = inject(ActivatedRoute).snapshot.queryParamMap.has("error");
  busy = signal(false);
  error = signal("");
  form = inject(FormBuilder).nonNullable.group({
    email: ["", [Validators.required, Validators.email]],
    password: ["", Validators.required],
  });
  submit() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.busy.set(true);
    this.error.set("");
    const value = this.form.getRawValue();
    this.auth.login(value.email, value.password).subscribe({
      next: () => {
        this.busy.set(false);
        this.form.reset();
        this.router.navigateByUrl("/dashboard");
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(
          e.status === 401
            ? "Email or password is incorrect, or the account is unavailable."
            : errorMessage(e),
        );
      },
    });
  }
}
