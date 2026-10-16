import { Component, inject, signal } from "@angular/core";
import { JsonPipe } from "@angular/common";
import { Api, errorMessage } from "../core/api";
@Component({
  imports: [JsonPipe],
  template: `<div class="page-heading">
      <div>
        <span class="eyebrow">IDENTITY INFRASTRUCTURE</span>
        <h1>System status</h1>
        <p>A transparent view of your identity server.</p>
      </div>
      <button class="button secondary" (click)="load()" [disabled]="busy()">
        ↻ Run checks
      </button>
    </div>
    @if (error()) {
      <div class="alert error" role="alert">{{ error() }}</div>
    }
    <div class="stat-grid">
      <article class="stat-card">
        <span class="stat-icon">◉</span>
        <p>Identity server</p>
        <h3>{{ health()?.status ?? "Checking…" }}</h3>
        <small>Live availability check</small>
      </article>
      <article class="stat-card">
        <span class="stat-icon purple">⌘</span>
        <p>OpenID discovery</p>
        <h3>{{ discovery() ? "Available" : "Checking…" }}</h3>
        <small>Provider configuration</small>
      </article>
      <article class="stat-card">
        <span class="stat-icon gold">◇</span>
        <p>Public signing keys</p>
        <h3>{{ keys()?.keys?.length ?? "—" }}</h3>
        <small>JWT signature verification</small>
      </article>
    </div>
    <section class="panel">
      <div class="section-heading">
        <h2>Provider configuration</h2>
        <span class="role-badge">OPENID CONNECT</span>
      </div>
      <pre>{{ discovery() | json }}</pre>
    </section>
    <section class="panel spaced">
      <div class="section-heading"><h2>Public verification keys</h2></div>
      <p class="muted">
        Only public keys are shown. Private signing material stays on the
        identity server.
      </p>
      <pre>{{ keys() | json }}</pre>
    </section>`,
})
export class System {
  api = inject(Api);
  health = signal<{ status: string } | null>(null);
  discovery = signal<unknown>(null);
  keys = signal<{ keys: unknown[] } | null>(null);
  error = signal("");
  busy = signal(false);
  constructor() {
    this.load();
  }
  load() {
    this.busy.set(true);
    this.error.set("");
    import("rxjs").then(({ forkJoin }) =>
      forkJoin({
        health: this.api.get<{ status: string }>("/actuator/health"),
        discovery: this.api.get("/.well-known/openid-configuration"),
        keys: this.api.get<{ keys: unknown[] }>("/oauth2/jwks"),
      }).subscribe({
        next: (r) => {
          this.health.set(r.health);
          this.discovery.set(r.discovery);
          this.keys.set(r.keys);
          this.busy.set(false);
        },
        error: (e) => {
          this.error.set(errorMessage(e));
          this.busy.set(false);
        },
      }),
    );
  }
}
