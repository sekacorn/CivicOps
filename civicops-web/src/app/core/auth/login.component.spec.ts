import { TestBed } from "@angular/core/testing";
import { provideRouter } from "@angular/router";
import { AuthService } from "./auth.service";
import { LoginComponent } from "./login.component";

describe("LoginComponent", () => {
  it("associates validation messages with invalid fields", async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { login: vi.fn() } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    fixture.nativeElement
      .querySelector("form")
      .dispatchEvent(new Event("submit"));
    fixture.detectChanges();

    const email = fixture.nativeElement.querySelector("#email");
    const password = fixture.nativeElement.querySelector("#password");
    expect(email.getAttribute("aria-invalid")).toBe("true");
    expect(email.getAttribute("aria-describedby")).toBe("email-error");
    expect(password.getAttribute("aria-invalid")).toBe("true");
    expect(password.getAttribute("aria-describedby")).toBe("password-error");
    expect(fixture.nativeElement.querySelector("#email-error")).not.toBeNull();
    expect(
      fixture.nativeElement.querySelector("#password-error"),
    ).not.toBeNull();
  });
});
