import { Component, inject, signal } from "@angular/core";
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from "@angular/router";
import { Auth, errorMessage } from "./core/api";
@Component({
  selector: "app-root",
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (auth.user(); as user) {
      <div class="app-layout">
        <aside class="sidebar">
          <a class="brand" routerLink="/dashboard"
            ><span class="brand-mark">i</span>identity<span class="brand-dot"
              >.</span
            ></a
          >
          <div class="workspace">
            <span class="workspace-icon">IC</span>
            <div>Identity Console<small>Access management</small></div>
            <span class="workspace-check">✓</span>
          </div>
          <p class="nav-label">WORKSPACE</p>
          <nav>
            <a routerLink="/dashboard" routerLinkActive="active"
              ><span>◫</span>Overview</a
            ><a routerLink="/profile" routerLinkActive="active"
              ><span>◎</span>My profile</a
            ><a routerLink="/security" routerLinkActive="active"
              ><span>◇</span>Security</a
            >
            @if (user.role === "ADMIN") {
              <a routerLink="/users" routerLinkActive="active"
                ><span>♧</span>Users & roles<span class="nav-badge"
                  >Admin</span
                ></a
              >
            }
            <a routerLink="/system" routerLinkActive="active"
              ><span>⌘</span>System status</a
            >
          </nav>
          <div class="sidebar-bottom">
            <div class="secure-note">
              <span class="green-dot"></span> Protected workspace<small
                >OAuth2 · Encrypted session</small
              >
            </div>
            <div class="account">
              <span class="avatar">{{
                user.displayName.slice(0, 1).toUpperCase()
              }}</span>
              <div>
                {{ user.displayName
                }}<small>{{
                  user.role === "ADMIN" ? "Administrator" : "Member"
                }}</small>
              </div>
              <button
                class="icon-button"
                aria-label="Sign out"
                title="Sign out"
                (click)="logout()"
                [disabled]="busy()"
              >
                ↪
              </button>
            </div>
          </div>
        </aside>
        <div class="main-area">
          <header class="topbar">
            <span
              >Workspace <span class="slash">/</span>
              <strong>Identity Console</strong></span
            ><span class="environment"
              ><span class="green-dot"></span>Connected</span
            >
          </header>
          @if (error()) {
            <div class="alert error" role="alert">
              {{ error() }} <button (click)="auth.signIn()">Sign in</button>
            </div>
          }
          <main><router-outlet /></main>
          <footer>
            Identity Console <span>Secure access. Clear control.</span>
          </footer>
        </div>
      </div>
    } @else {
      <router-outlet />
    }
  `,
})
export class App {
  auth = inject(Auth);
  private router = inject(Router);
  busy = signal(false);
  error = signal("");
  logout() {
    this.busy.set(true);
    this.auth.logout().subscribe({
      next: () => {
        this.busy.set(false);
        this.router.navigateByUrl("/sign-in");
      },
      error: (e) => {
        this.busy.set(false);
        if (e.status === 401) {
          this.auth.clear();
          this.router.navigateByUrl("/sign-in");
        } else this.error.set(errorMessage(e));
      },
    });
  }
}
