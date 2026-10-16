import { Component, inject } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { Auth } from "../core/api";
@Component({
  imports: [RouterLink],
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
      <small>Built on OAuth2 and OpenID Connect</small>
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
        <div class="login-card">
          <span class="large-icon">◇</span>
          <h3>Your account, securely connected</h3>
          <p>
            Continue to the identity server to enter your email and password.
          </p>
          <button class="button primary full" (click)="auth.signIn()">
            Continue to sign in <span>↗</span>
          </button>
          <div class="trust-line">✓ Protected sign-in with PKCE</div>
        </div>
        <p class="auth-switch">
          New to the workspace? <a routerLink="/register">Create an account</a>
        </p>
      </div>
    </section>
  </div>`,
})
export class Login {
  auth = inject(Auth);
  failed = inject(ActivatedRoute).snapshot.queryParamMap.has("error");
}
