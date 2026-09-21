import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  input,
  OnDestroy,
  output,
  ViewChild,
} from "@angular/core";

@Component({
  selector: "cop-confirmation-dialog",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog
      #dialog
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="confirm-title"
      aria-describedby="confirm-message"
      (cancel)="cancelDialog($event)"
    >
      <h2 id="confirm-title">{{ title() }}</h2>
      <p id="confirm-message">{{ message() }}</p>
      <div>
        <button
          #cancelButton
          type="button"
          class="button button--secondary"
          (click)="cancelled.emit()"
        >
          Cancel
        </button>
        <button
          type="button"
          class="button button--danger"
          (click)="confirmed.emit()"
        >
          {{ confirmLabel() }}
        </button>
      </div>
    </dialog>
  `,
  styles: `
    dialog {
      width: min(30rem, 100%);
      padding: 1.5rem;
      background: white;
      border: 0;
      border-radius: 0.7rem;
      box-shadow: var(--shadow);
    }
    dialog::backdrop {
      background: rgb(8 30 34 / 62%);
    }
    h2 {
      margin-top: 0;
    }
    div {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
    }
  `,
})
export class ConfirmationDialogComponent implements AfterViewInit, OnDestroy {
  @ViewChild("cancelButton")
  private cancelButton!: ElementRef<HTMLButtonElement>;
  @ViewChild("dialog")
  private dialog!: ElementRef<HTMLDialogElement>;
  private readonly previouslyFocused =
    document.activeElement as HTMLElement | null;
  private focusTimer: ReturnType<typeof setTimeout> | undefined;
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input("Confirm");
  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  ngAfterViewInit(): void {
    // Defer until the click that opened the dialog has completed. Otherwise,
    // the browser can return focus to the invoking button after this hook.
    this.focusTimer = setTimeout(
      () => this.cancelButton.nativeElement.focus(),
      50,
    );
    try {
      if (typeof this.dialog.nativeElement.showModal === "function") {
        this.dialog.nativeElement.showModal();
      } else {
        this.dialog.nativeElement.setAttribute("open", "");
      }
    } catch {
      // A modal may be unavailable in an embedded renderer; retain the
      // labelled dialog and explicit focus management as a safe fallback.
      this.dialog.nativeElement.setAttribute("open", "");
    }
  }

  ngOnDestroy(): void {
    if (this.focusTimer !== undefined) clearTimeout(this.focusTimer);
    this.previouslyFocused?.focus();
  }

  @HostListener("document:keydown.escape")
  cancelOnEscape(): void {
    this.cancelled.emit();
  }

  cancelDialog(event: Event): void {
    event.preventDefault();
    this.cancelled.emit();
  }

  @HostListener("document:keydown.tab", ["$event"])
  trapFocus(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    const buttons = Array.from(
      this.cancelButton.nativeElement
        .closest("dialog")
        ?.querySelectorAll("button") ?? [],
    );
    if (buttons.length < 2) return;
    const first = buttons[0];
    const last = buttons[buttons.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }
}
