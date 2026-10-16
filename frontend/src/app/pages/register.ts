import { Component, inject, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { Api, Auth, errorMessage } from "../core/api";
@Component({
  imports: [ReactiveFormsModule, RouterLink],
  template: `<div class="registration-page">
    <a class="brand" routerLink="/sign-in"
      ><span class="brand-mark">i</span>identity.</a
    >
    <section class="panel registration-card">
      <span class="label-tag">GET STARTED</span>
      <h1>Create your account</h1>
      <p class="muted">Your secure workspace starts here.</p>
      @if (done()) {
        <div class="alert success" role="status">
          Your account is ready. Sign in to continue.
        </div>
        <button class="button primary full" (click)="auth.signIn()">
          Sign in to your account ↗
        </button>
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit()">
          <label
            >Full name<input
              formControlName="displayName"
              autocomplete="name"
              placeholder="Your full name"
              maxlength="100" /></label
          ><label
            >Email address<input
              formControlName="email"
              type="email"
              autocomplete="email"
              placeholder="you@example.com"
              maxlength="254" /></label
          ><label
            >Password<input
              formControlName="password"
              type="password"
              autocomplete="new-password"
              placeholder="Create a strong password"
          /></label>
          <p class="field-hint">
            At least 12 characters, up to 72 UTF-8 bytes.
          </p>
          @if (form.touched && form.invalid) {
            <p class="field-error">
              Enter your name, a valid email and a password of at least 12
              characters.
            </p>
          }
          @if (error()) {
            <div class="alert error" role="alert">{{ error() }}</div>
          }
          <button class="button primary full" [disabled]="busy()">
            {{ busy() ? "Creating account…" : "Create account" }} <span>→</span>
          </button>
        </form>
        <p class="field-hint centered">
          New accounts have the Member role. An administrator manages elevated
          access.
        </p>
      }
      <p class="auth-switch">
        Already have an account? <a routerLink="/sign-in">Sign in</a>
      </p>
    </section>
  </div>`,
})
export class Register {
  private api = inject(Api);
  auth = inject(Auth);
  private fb = inject(FormBuilder);
  busy = signal(false);
  done = signal(false);
  error = signal("");
  form = this.fb.nonNullable.group({
    displayName: ["", [Validators.required, Validators.maxLength(100)]],
    email: [
      "",
      [Validators.required, Validators.email, Validators.maxLength(254)],
    ],
    password: ["", [Validators.required, Validators.minLength(12)]],
  });
  submit() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    if (
      new TextEncoder().encode(this.form.controls.password.value).length > 72
    ) {
      this.error.set("Password must be at most 72 UTF-8 bytes.");
      return;
    }
    this.busy.set(true);
    this.error.set("");
    this.api
      .mutate("POST", "/bff/register", this.form.getRawValue())
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.form.reset();
          this.done.set(true);
        },
        error: (e) => {
          this.busy.set(false);
          this.error.set(errorMessage(e));
        },
      });
  }
}
