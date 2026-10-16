import { Component, inject, signal } from "@angular/core";
import { DatePipe } from "@angular/common";
import { RouterLink } from "@angular/router";
import { Api, Page, errorMessage } from "../core/api";
@Component({
  imports: [DatePipe, RouterLink],
  template: `<div class="page-heading">
      <div>
        <span class="eyebrow">PEOPLE & PERMISSIONS</span>
        <h1>Users & roles</h1>
        <p>A home for everyone in your workspace.</p>
      </div>
      <a class="button secondary" routerLink="/register">Register a user ↗</a>
    </div>
    <div class="users-summary">
      <span class="summary-icon">♧</span>
      <div>
        <strong>{{ data()?.totalElements ?? "—" }} members</strong>
        <p>Manage who has access, and what they can do.</p>
      </div>
      <span class="role-badge">ADMIN WORKSPACE</span>
    </div>
    <section class="panel table-panel">
      <div class="section-heading">
        <div>
          <h2>
            All members
            <span class="count-badge">{{ data()?.totalElements ?? 0 }}</span>
          </h2>
        </div>
        <button
          class="button secondary small"
          (click)="load()"
          [disabled]="loading()"
        >
          ↻ Refresh
        </button>
      </div>
      @if (error()) {
        <div class="alert error" role="alert">{{ error() }}</div>
      }
      @if (loading()) {
        <div class="empty-state" role="status">Loading workspace members…</div>
      } @else {
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Member</th>
                <th>Role</th>
                <th>Status</th>
                <th>Joined</th>
                <th><span class="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              @for (user of data()?.content; track user.id) {
                <tr>
                  <td>
                    <div class="member-cell">
                      <span class="avatar">{{
                        user.displayName.slice(0, 1)
                      }}</span>
                      <div>
                        <strong>{{ user.displayName }}</strong
                        ><small>{{ user.email }}</small>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span
                      class="role-badge"
                      [class.admin]="user.role === 'ADMIN'"
                      >{{
                        user.role === "ADMIN" ? "Administrator" : "Member"
                      }}</span
                    >
                  </td>
                  <td>
                    <span
                      class="status"
                      [class.active]="user.enabled"
                      [class.inactive]="!user.enabled"
                      >{{ user.enabled ? "Active" : "Disabled" }}</span
                    >
                  </td>
                  <td class="muted">
                    {{ user.createdAt | date: "mediumDate" }}
                  </td>
                  <td>
                    <a
                      class="table-link"
                      [routerLink]="['/users', user.id]"
                      [attr.aria-label]="'Manage ' + user.displayName"
                      >Manage ↗</a
                    >
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="empty-state">
                    No members to display.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
      <div class="pagination">
        <span>Page {{ page() + 1 }} of {{ pageCount() }}</span>
        <div>
          <button
            class="button secondary small"
            (click)="move(-1)"
            [disabled]="page() === 0 || loading()"
          >
            ← Previous</button
          ><button
            class="button secondary small"
            (click)="move(1)"
            [disabled]="
              (page() + 1) * size >= (data()?.totalElements ?? 0) || loading()
            "
          >
            Next →
          </button>
        </div>
      </div>
    </section>
    <p class="below-note">
      ◇ Roles and status changes take effect immediately. Large workspaces are
      loaded one page at a time.
    </p>`,
})
export class Users {
  api = inject(Api);
  data = signal<Page | null>(null);
  page = signal(0);
  size = 20;
  loading = signal(false);
  error = signal("");
  constructor() {
    this.load();
  }
  pageCount() {
    return Math.max(
      1,
      Math.ceil((this.data()?.totalElements ?? 0) / this.size),
    );
  }
  move(delta: number) {
    this.page.update((v) => v + delta);
    this.load();
  }
  load() {
    this.loading.set(true);
    this.error.set("");
    this.api
      .get<Page>("/bff/users?page=" + this.page() + "&size=" + this.size)
      .subscribe({
        next: (p) => {
          this.data.set(p);
          this.loading.set(false);
        },
        error: (e) => {
          this.error.set(errorMessage(e));
          this.loading.set(false);
        },
      });
  }
}
