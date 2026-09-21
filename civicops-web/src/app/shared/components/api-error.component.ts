import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { ApiError } from "../../core/models/api.models";

@Component({
  selector: "cop-api-error",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (error(); as problem) {
      <div class="alert" role="alert">
        <strong>{{
          problem.status === 403 ? "Access restricted" : "Request not completed"
        }}</strong>
        <span>{{ problem.message }}</span>
      </div>
    }
  `,
  styles: `
    .alert {
      display: grid;
      gap: 0.25rem;
      padding: 0.9rem 1rem;
      color: #772b2b;
      background: #fff0f0;
      border: 1px solid #efc4c4;
      border-radius: 0.5rem;
    }
  `,
})
export class ApiErrorComponent {
  readonly error = input<ApiError | null>();
}
