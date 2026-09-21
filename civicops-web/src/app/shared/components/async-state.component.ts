import { ChangeDetectionStrategy, Component, input } from "@angular/core";

export type AsyncViewState = "loading" | "empty" | "error" | "forbidden";

@Component({
  selector: "cop-async-state",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section
      class="state"
      [attr.aria-live]="state() === 'loading' ? 'polite' : 'assertive'"
    >
      <span class="state__icon" aria-hidden="true">{{ icon() }}</span>
      <h2>{{ title() }}</h2>
      <p>{{ message() }}</p>
    </section>
  `,
  styles: `
    .state {
      padding: 3rem 1.5rem;
      text-align: center;
      color: var(--ink-muted);
    }
    .state__icon {
      display: block;
      margin-bottom: 0.75rem;
      font-size: 2rem;
    }
    h2 {
      margin: 0 0 0.5rem;
      color: var(--ink);
      font-size: 1.15rem;
    }
    p {
      margin: 0 auto;
      max-width: 34rem;
    }
  `,
})
export class AsyncStateComponent {
  readonly state = input.required<AsyncViewState>();
  readonly detail = input<string>();

  title(): string {
    return {
      loading: "Loading",
      empty: "Nothing here yet",
      error: "Unable to load",
      forbidden: "Access restricted",
    }[this.state()];
  }

  message(): string {
    return (
      this.detail() ??
      {
        loading: "CivicOps is retrieving the latest information.",
        empty: "There are no records to display.",
        error:
          "Please try again or contact your administrator if the problem continues.",
        forbidden:
          "Your current organization role does not permit this action.",
      }[this.state()]
    );
  }

  icon(): string {
    return { loading: "◌", empty: "◇", error: "!", forbidden: "⊘" }[
      this.state()
    ];
  }
}
