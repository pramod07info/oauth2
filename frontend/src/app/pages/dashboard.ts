import { Component, inject, signal } from "@angular/core";
import { DatePipe } from "@angular/common";
import { RouterLink } from "@angular/router";
import { Api, Auth, Page } from "../core/api";
@Component({
  imports: [RouterLink, DatePipe],
  template: `<div class="page-heading">
      <div>
        <span class="eyebrow">WORKSPACE OVERVIEW</span>
        <h1>
          Good to see you, {{ auth.user()?.displayName?.split(" ")?.[0]
          }}<span class="accent">.</span>
        </h1>
        <p>A clear view of your identity and access.</p>
      </div>
      <a class="button secondary" routerLink="/profile">Manage profile ↗</a>
    </div>
    <section class="welcome-banner">
      <div>
        <span class="label-tag light">YOU'RE IN GOOD HANDS</span>
        <h2>One identity. A protected workspace.</h2>
        <p>Your account is connected through a secure, encrypted session.</p>
        <a routerLink="/security">Review your security <span>→</span></a>
      </div>
      <div class="banner-art" aria-hidden="true"><span>◇</span><i>✓</i></div>
    </section>
    <div class="stat-grid">
      <article class="stat-card">
        <span class="stat-icon">◎</span>
        <p>Account status</p>
        <h3>Active <span class="status active">Verified</span></h3>
        <small>Ready to access your workspace</small>
      </article>
      <article class="stat-card">
        <span class="stat-icon purple">♧</span>
        <p>Your access level</p>
        <h3>
          {{ auth.user()?.role === "ADMIN" ? "Administrator" : "Member" }}
        </h3>
        <small>{{
          auth.user()?.role === "ADMIN"
            ? "Manage people and permissions"
            : "Personal workspace access"
        }}</small>
      </article>
      <article class="stat-card">
        <span class="stat-icon gold">◇</span>
        <p>
          {{
            auth.user()?.role === "ADMIN"
              ? "Workspace members"
              : "Session protection"
          }}
        </p>
        <h3>
          {{
            auth.user()?.role === "ADMIN"
              ? total() === null
                ? "—"
                : total()
              : "OAuth2 + PKCE"
          }}
        </h3>
        <small>{{
          auth.user()?.role === "ADMIN"
            ? "Registered user accounts"
            : "Server-managed token lifecycle"
        }}</small>
      </article>
    </div>
    <div class="two-columns">
      <section class="panel">
        <div class="section-heading">
          <h2>Your identity</h2>
          <a routerLink="/profile">Edit profile ↗</a>
        </div>
        <div class="identity-row">
          <span class="avatar large">{{
            auth.user()?.displayName?.slice(0, 1)
          }}</span>
          <div>
            <h3>{{ auth.user()?.displayName }}</h3>
            <p>{{ auth.user()?.email }}</p>
          </div>
        </div>
        <dl class="details">
          <div>
            <dt>Role</dt>
            <dd>{{ auth.user()?.role }}</dd>
          </div>
          <div>
            <dt>Member since</dt>
            <dd>{{ auth.user()?.createdAt | date: "mediumDate" }}</dd>
          </div>
          <div>
            <dt>Session expires</dt>
            <dd>{{ auth.expiresAt() | date: "shortTime" }}</dd>
          </div>
        </dl>
      </section>
      <section class="panel">
        <div class="section-heading">
          <h2>Make yourself at home</h2>
          <span class="muted">QUICK ACTIONS</span>
        </div>
        <a class="action-row" routerLink="/profile"
          ><span class="action-icon">◎</span>
          <div>
            <strong>Keep your profile up to date</strong
            ><small>Update your name and view account details</small>
          </div>
          <span>↗</span></a
        ><a class="action-row" routerLink="/security"
          ><span class="action-icon">◇</span>
          <div>
            <strong>A little peace of mind</strong
            ><small>Change your password and review your session</small>
          </div>
          <span>↗</span></a
        >
        @if (auth.user()?.role === "ADMIN") {
          <a class="action-row" routerLink="/users"
            ><span class="action-icon">♧</span>
            <div>
              <strong>Bring your team together</strong
              ><small>Manage users, roles and account access</small>
            </div>
            <span>↗</span></a
          >
        } @else {
          <a class="action-row" routerLink="/system"
            ><span class="action-icon">⌘</span>
            <div>
              <strong>Check your connection</strong
              ><small>View identity server availability</small>
            </div>
            <span>↗</span></a
          >
        }
      </section>
    </div>`,
})
export class Dashboard {
  auth = inject(Auth);
  api = inject(Api);
  total = signal<number | null>(null);
  constructor() {
    if (this.auth.user()?.role === "ADMIN")
      this.api
        .get<Page>("/bff/users?size=1&page=0")
        .subscribe({
          next: (p) => this.total.set(p.totalElements),
          error: () => this.total.set(null),
        });
  }
}
