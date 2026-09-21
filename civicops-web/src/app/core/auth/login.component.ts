import { Component, inject, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { Router } from "@angular/router";
import { finalize } from "rxjs";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { ApiErrorService } from "../http/api-error.service";
import { ApiError } from "../models/api.models";
import { AuthService } from "./auth.service";

@Component({
  selector: "cop-login",
  imports: [ReactiveFormsModule, ApiErrorComponent],
  template: `
    <main class="login-page">
      <section class="login-card" aria-labelledby="login-title">
        <div class="brand-mark" aria-hidden="true">CO</div>
        <p class="eyebrow">CIVIC OPERATIONS</p>
        <h1 id="login-title">Welcome to CivicOps</h1>
        <p class="intro">
          Sign in to manage your organization’s programs, funding, and outcomes.
        </p>
        <cop-api-error [error]="error()" />
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label for="email">Email address</label>
          <input
            id="email"
            type="email"
            formControlName="email"
            autocomplete="username"
            [attr.aria-invalid]="
              form.controls.email.touched && form.controls.email.invalid
                ? 'true'
                : null
            "
            [attr.aria-describedby]="
              form.controls.email.touched && form.controls.email.invalid
                ? 'email-error'
                : null
            "
          />
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <p class="field-error" id="email-error">
              Enter a valid email address.
            </p>
          }
          <label for="password">Password</label>
          <input
            id="password"
            type="password"
            formControlName="password"
            autocomplete="current-password"
            [attr.aria-invalid]="
              form.controls.password.touched && form.controls.password.invalid
                ? 'true'
                : null
            "
            [attr.aria-describedby]="
              form.controls.password.touched && form.controls.password.invalid
                ? 'password-error'
                : null
            "
          />
          @if (
            form.controls.password.touched && form.controls.password.invalid
          ) {
            <p class="field-error" id="password-error">Password is required.</p>
          }
          <button
            class="button button--primary button--wide"
            type="submit"
            [disabled]="submitting()"
          >
            {{ submitting() ? "Signing in…" : "Sign in" }}
          </button>
        </form>
      </section>
    </main>
  `,
  styles: `
    .login-page {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 2rem;
      background:
        radial-gradient(circle at top left, #dce9e6, transparent 40%),
        var(--canvas);
    }
    .login-card {
      width: min(100%, 27rem);
      padding: 2.25rem;
      background: white;
      border: 1px solid var(--line);
      border-radius: 1rem;
      box-shadow: var(--shadow);
    }
    .brand-mark {
      display: grid;
      place-items: center;
      width: 2.75rem;
      height: 2.75rem;
      color: white;
      background: var(--brand);
      border-radius: 0.65rem;
      font-weight: 800;
    }
    h1 {
      margin: 0.3rem 0 0.6rem;
      font-size: 1.8rem;
    }
    .intro {
      margin: 0 0 1.5rem;
      color: var(--ink-muted);
    }
    form {
      display: grid;
      gap: 0.55rem;
      margin-top: 1.25rem;
    }
    form label:not(:first-child) {
      margin-top: 0.55rem;
    }
    .button--wide {
      margin-top: 0.8rem;
      width: 100%;
    }
  `,
})
export class LoginComponent {
  private readonly builder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly errors = inject(ApiErrorService);
  private readonly router = inject(Router);
  readonly submitting = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly form = this.builder.nonNullable.group({
    email: ["", [Validators.required, Validators.email]],
    password: ["", Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    this.auth
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl("/dashboard"),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
