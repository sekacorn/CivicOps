import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
} from "@angular/core";

@Component({
  selector: "cop-status-badge",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge" [class]="'badge badge--' + tone()">{{
    label()
  }}</span>`,
  styles: `
    .badge {
      display: inline-flex;
      padding: 0.25rem 0.55rem;
      border-radius: 999px;
      font-size: 0.72rem;
      font-weight: 750;
      letter-spacing: 0.035em;
    }
    .badge--neutral {
      color: #334155;
      background: #e8edf2;
    }
    .badge--info {
      color: #17536a;
      background: #dceff5;
    }
    .badge--success {
      color: #165b3d;
      background: #d9f2e4;
    }
    .badge--warning {
      color: #764512;
      background: #f8e9c8;
    }
    .badge--danger {
      color: #852d2d;
      background: #f8dede;
    }
  `,
})
export class StatusBadgeComponent {
  readonly status = input.required<string>();
  readonly label = computed(() => this.status().replaceAll("_", " "));
  readonly tone = computed(() => {
    const status = this.status();
    if (["ACTIVE", "FINALIZED", "APPROVED", "AWARDED"].includes(status))
      return "success";
    if (["REJECTED", "CANCELLED", "WITHDRAWN"].includes(status))
      return "danger";
    if (["SUBMITTED", "APPLICATION_IN_PROGRESS", "DRAFT"].includes(status))
      return "info";
    if (["CLOSED", "PENDING"].includes(status)) return "warning";
    return "neutral";
  });
}
