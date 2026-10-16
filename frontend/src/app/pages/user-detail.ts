import { Component, inject, signal } from "@angular/core";
import { DatePipe } from "@angular/common";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { Api, Auth, User, errorMessage } from "../core/api";
@Component({
  imports: [ReactiveFormsModule, DatePipe, RouterLink],
  template: `<a routerLink="/users" class="back-link">← All members</a>
    <div class="page-heading">
      <div>
        <span class="eyebrow">MEMBER DETAILS</span>
        <h1>{{ user()?.displayName || "Member" }}</h1>
        <p>{{ user()?.email }}</p>
      </div>
      @if (user(); as u) {
        <span
          class="status"
          [class.active]="u.enabled"
          [class.inactive]="!u.enabled"
          >{{ u.enabled ? "Active" : "Disabled" }}</span
        >
      }
    </div>
    @if (error()) {
      <div class="alert error" role="alert">
        {{ error() }}
        <button class="text-button" (click)="load()">Reload record</button>
      </div>
    }
    @if (message()) {
      <div class="alert success" role="status">{{ message() }}</div>
    }
    @if (user(); as u) {
      <div class="two-columns">
        <section class="panel">
          <div class="section-heading"><h2>Profile information</h2></div>
          <form [formGroup]="profile" (ngSubmit)="saveProfile()">
            <label
              >Display name<input
                formControlName="displayName"
                maxlength="100" /></label
            ><label>Email<input [value]="u.email" disabled /></label>
            <dl class="details">
              <div>
                <dt>Joined</dt>
                <dd>{{ u.createdAt | date: "mediumDate" }}</dd>
              </div>
              <div>
                <dt>Version</dt>
                <dd>{{ u.version }}</dd>
              </div>
            </dl>
            <button
              class="button primary"
              [disabled]="busy() || profile.invalid"
            >
              Save profile
            </button>
          </form>
        </section>
        <section class="panel">
          <div class="section-heading"><h2>Access & permissions</h2></div>
          <form [formGroup]="permissions" (ngSubmit)="savePermissions()">
            <label
              >Workspace role<select formControlName="role">
                <option value="USER">Member — personal account access</option>
                <option value="ADMIN">Administrator — manage all users</option>
              </select></label
            ><label class="checkbox-label"
              ><input type="checkbox" formControlName="enabled" />Account
              enabled</label
            >
            <p class="field-hint">
              Disabling an account revokes access immediately.
            </p>
            @if (own()) {
              <div class="security-note">
                You cannot change your own role or status, or delete your
                account.
              </div>
            }
            <button class="button primary" [disabled]="busy() || own()">
              Save permissions
            </button>
          </form>
          <hr />
          <h3 class="danger-text">Delete account</h3>
          <p class="muted">
            Permanently removes this member and their authorization records.
          </p>
          @if (confirming()) {
            <div class="delete-confirm" role="alert">
              <p>Delete {{ u.email }}? This cannot be undone.</p>
              <div class="session-actions">
                <button
                  class="button danger"
                  (click)="remove()"
                  [disabled]="busy()"
                >
                  Yes, delete account</button
                ><button
                  class="button secondary"
                  (click)="confirming.set(false)"
                >
                  Cancel
                </button>
              </div>
            </div>
          } @else {
            <button
              class="button danger-outline"
              (click)="confirming.set(true)"
              [disabled]="busy() || own()"
            >
              Delete account
            </button>
          }
        </section>
      </div>
    } @else if (!error()) {
      <div class="empty-state">Loading member…</div>
    }`,
})
export class UserDetail {
  api = inject(Api);
  auth = inject(Auth);
  router = inject(Router);
  id = inject(ActivatedRoute).snapshot.paramMap.get("id")!;
  fb = inject(FormBuilder);
  user = signal<User | null>(null);
  busy = signal(false);
  error = signal("");
  message = signal("");
  confirming = signal(false);
  profile = this.fb.nonNullable.group({
    displayName: ["", Validators.required],
  });
  permissions = this.fb.nonNullable.group({
    role: ["USER" as "USER" | "ADMIN"],
    enabled: [true],
  });
  constructor() {
    this.load();
  }
  own() {
    return this.id === this.auth.user()?.id;
  }
  load() {
    this.error.set("");
    this.api.get<User>("/api/users/" + this.id).subscribe({
      next: (u) => this.set(u),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
  private set(u: User) {
    this.user.set(u);
    this.profile.patchValue({ displayName: u.displayName });
    this.permissions.patchValue({ role: u.role, enabled: u.enabled });
    this.busy.set(false);
  }
  private save(path: string, body: unknown) {
    this.busy.set(true);
    this.error.set("");
    this.message.set("");
    this.api.mutate<User>("PUT", path, body).subscribe({
      next: (u) => {
        this.set(u);
        if (this.own()) this.auth.load().subscribe();
        this.message.set("Member updated successfully.");
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
  saveProfile() {
    if (this.profile.invalid) return;
    this.save("/api/users/" + this.id, {
      ...this.profile.getRawValue(),
      version: this.user()?.version,
    });
  }
  savePermissions() {
    if (this.own()) return;
    this.save("/api/users/" + this.id + "/permissions", {
      ...this.permissions.getRawValue(),
      version: this.user()?.version,
    });
  }
  remove() {
    this.busy.set(true);
    this.api.mutate("DELETE", "/api/users/" + this.id).subscribe({
      next: () => this.router.navigateByUrl("/users"),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
