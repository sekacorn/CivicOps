import { fakeAsync, TestBed, tick } from "@angular/core/testing";
import { ConfirmationDialogComponent } from "./confirmation-dialog.component";

describe("ConfirmationDialogComponent", () => {
  beforeEach(() =>
    TestBed.configureTestingModule({ imports: [ConfirmationDialogComponent] }),
  );
  it("provides labelled modal alert-dialog semantics", () => {
    const fixture = TestBed.createComponent(ConfirmationDialogComponent);
    fixture.componentRef.setInput("title", "Finalize report?");
    fixture.componentRef.setInput("message", "This cannot be edited later.");
    fixture.detectChanges();
    const dialog = fixture.nativeElement.querySelector('[role="alertdialog"]');
    expect(dialog.getAttribute("aria-modal")).toBe("true");
    expect(dialog.getAttribute("aria-labelledby")).toBe("confirm-title");
    expect(dialog.getAttribute("aria-describedby")).toBe("confirm-message");
  });
  it("emits cancel and confirm actions", () => {
    const fixture = TestBed.createComponent(ConfirmationDialogComponent);
    fixture.componentRef.setInput("title", "Confirm");
    fixture.componentRef.setInput("message", "Continue?");
    fixture.detectChanges();
    let cancelled = false;
    let confirmed = false;
    fixture.componentInstance.cancelled.subscribe(() => (cancelled = true));
    fixture.componentInstance.confirmed.subscribe(() => (confirmed = true));
    const buttons = fixture.nativeElement.querySelectorAll("button");
    buttons[0].click();
    buttons[1].click();
    expect(cancelled).toBe(true);
    expect(confirmed).toBe(true);
  });

  it("moves focus into the dialog and supports Escape cancellation", fakeAsync(() => {
    const outside = document.createElement("button");
    document.body.appendChild(outside);
    outside.focus();
    const fixture = TestBed.createComponent(ConfirmationDialogComponent);
    document.body.appendChild(fixture.nativeElement);
    fixture.componentRef.setInput("title", "Confirm");
    fixture.componentRef.setInput("message", "Continue?");
    let cancelled = false;
    fixture.componentInstance.cancelled.subscribe(() => (cancelled = true));
    fixture.detectChanges();
    tick(50);
    expect(document.activeElement?.textContent?.trim()).toBe("Cancel");
    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }));
    expect(cancelled).toBe(true);
    fixture.destroy();
    expect(document.activeElement).toBe(outside);
    fixture.nativeElement.remove();
    outside.remove();
  }));
});
