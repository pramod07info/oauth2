import { Component, inject, signal } from "@angular/core";
import { DatePipe } from "@angular/common";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { Api, Auth, errorMessage, User } from "../core/api";
@Component({
  imports: [ReactiveFormsModule, DatePipe],
  template: `<div class="page-heading">
      <div>
        <span class="eyebrow">YOUR ACCOUNT</span>
        <h1>My profile</h1>
        <p>The details that make this workspace yours.</p>
      </div>
      <span class="status active">Active account</span>
    </div>
    <div class="profile-layout">
      <section class="panel profile-summary">
        <span class="avatar extra-large">{{
          auth.user()?.displayName?.slice(0, 1)
        }}</span>
        <h2>{{ auth.user()?.displayName }}</h2>
        <p>{{ auth.user()?.email }}</p>
        <span class="role-badge">{{ auth.user()?.role }}</span>
        <hr />
        <small>MEMBER SINCE</small>
        <p>{{ auth.user()?.createdAt | date: "longDate" }}</p>
      </section>
      <section class="panel">
        <div class="section-heading">
          <h2>Personal information</h2>
          <span class="muted">PROFILE DETAILS</span>
        </div>
        <form [formGroup]="form" (ngSubmit)="save()">
          <label
            >Display name<input
              formControlName="displayName"
              maxlength="100"
              autocomplete="name" /></label
          ><label
            >Email address<input
              [value]="auth.user()?.email"
              type="email"
              disabled
          /></label>
          <p class="field-hint">
            Your email identifies your account and cannot be changed here.
          </p>
          <label
            >Account ID<input [value]="auth.user()?.id" disabled class="mono"
          /></label>
          @if (error()) {
            <div class="alert error" role="alert">{{ error() }}</div>
          }
          @if (success()) {
            <div class="alert success" role="status">
              Profile updated successfully.
            </div>
          }
          <div class="form-actions">
            <span class="muted"
              >Last updated
              {{ auth.user()?.updatedAt | date: "mediumDate" }}</span
            ><button class="button primary" [disabled]="busy() || form.invalid">
              {{ busy() ? "Saving…" : "Save changes" }}
            </button>
          </div>
        </form>
      </section>
    </div>`,
})
export class Profile {
  auth = inject(Auth);
  api = inject(Api);
  busy = signal(false);
  error = signal("");
  success = signal(false);
  form = inject(FormBuilder).nonNullable.group({
    displayName: [
      this.auth.user()?.displayName || "",
      [Validators.required, Validators.maxLength(100)],
    ],
  });
  save() {
    if (this.form.invalid) return;
    this.busy.set(true);
    this.error.set("");
    this.success.set(false);
    this.api
      .mutate<User>("PUT", "/api/users/me", {
        ...this.form.getRawValue(),
        version: this.auth.user()?.version,
      })
      .subscribe({
        next: (user) => {
          this.auth.user.set(user);
          this.auth.refresh().subscribe({
            error: () => {
              this.auth.clear();
              this.auth.signIn();
            },
          });
          this.busy.set(false);
          this.success.set(true);
        },
        error: (e) => {
          this.busy.set(false);
          this.error.set(errorMessage(e));
          if (e.status === 409) this.auth.load().subscribe();
        },
      });
  }
}
