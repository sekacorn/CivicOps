import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from "@angular/core";

@Component({
  selector: "cop-pagination",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="pager" aria-label="Pagination">
      <p>
        Page {{ page() + 1 }} of {{ displayTotalPages() }} ·
        {{ totalElements() }} records
      </p>
      <div>
        <button
          type="button"
          class="button button--secondary"
          [disabled]="first()"
          (click)="pageChange.emit(page() - 1)"
        >
          Previous
        </button>
        <button
          type="button"
          class="button button--secondary"
          [disabled]="last()"
          (click)="pageChange.emit(page() + 1)"
        >
          Next
        </button>
      </div>
    </nav>
  `,
  styles: `
    .pager {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
      padding-top: 1rem;
    }
    .pager p {
      margin: 0;
      color: var(--ink-muted);
      font-size: 0.875rem;
    }
    .pager div {
      display: flex;
      gap: 0.5rem;
    }
    @media (max-width: 520px) {
      .pager {
        align-items: stretch;
        flex-direction: column;
      }
      .pager div button {
        flex: 1;
      }
    }
  `,
})
export class PaginationComponent {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly first = input.required<boolean>();
  readonly last = input.required<boolean>();
  readonly pageChange = output<number>();
  readonly displayTotalPages = computed(() => Math.max(this.totalPages(), 1));
}
