import { Component, computed, inject, signal } from "@angular/core";
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from "@angular/router";
import { AuthService } from "../auth/auth.service";
import { Role } from "../models/organization.models";
import { OrganizationContextService } from "../organization/organization-context.service";

interface NavigationItem {
  label: string;
  roles: Role[];
  route?: string;
}

@Component({
  selector: "cop-app-shell",
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="shell" [class.shell--menu-open]="menuOpen()">
      <header class="topbar">
        <button
          class="menu-button"
          type="button"
          aria-label="Toggle navigation"
          (click)="menuOpen.set(!menuOpen())"
        >
          ☰
        </button>
        <a class="brand" routerLink="/dashboard"><span>CO</span>CivicOps</a>
        <label class="organization-picker">
          <span>Organization</span>
          <select
            [value]="organizations.selectedOrganizationId() ?? ''"
            (change)="switchOrganization($event)"
          >
            @for (
              organization of organizations.organizations();
              track organization.id
            ) {
              <option [value]="organization.id">{{ organization.name }}</option>
            }
          </select>
        </label>
        <div class="user-menu">
          <div>
            <strong>{{ displayName() }}</strong
            ><span>{{
              organizations.selectedRole()?.replaceAll("_", " ")
            }}</span>
          </div>
          <button type="button" class="button button--quiet" (click)="logout()">
            Log out
          </button>
        </div>
      </header>
      <aside class="sidebar" aria-label="Primary navigation">
        <nav>
          <a
            routerLink="/dashboard"
            routerLinkActive="active"
            [routerLinkActiveOptions]="{ exact: true }"
            >Dashboard</a
          >
          @for (item of navigation(); track item.label) {
            @if (item.route; as route) {
              <a [routerLink]="route" routerLinkActive="active">{{
                item.label
              }}</a>
            } @else {
              <span
                class="future"
                [attr.aria-label]="
                  item.label + ' frontend coming in a later phase'
                "
                >{{ item.label }}</span
              >
            }
          }
        </nav>
        <p class="version">CivicOps 0.1.0</p>
      </aside>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    .shell {
      min-height: 100vh;
      display: grid;
      grid-template: 4.25rem 1fr / 15rem 1fr;
    }
    .topbar {
      grid-column: 1 / -1;
      position: sticky;
      top: 0;
      z-index: 5;
      display: flex;
      align-items: center;
      gap: 1.5rem;
      padding: 0.65rem 1.25rem;
      color: white;
      background: var(--brand-dark);
      box-shadow: 0 1px 0 rgb(255 255 255 / 12%);
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      color: white;
      font-weight: 800;
      text-decoration: none;
      font-size: 1.18rem;
    }
    .brand span {
      display: grid;
      place-items: center;
      width: 2.35rem;
      height: 2.35rem;
      background: var(--accent);
      border-radius: 0.55rem;
      font-size: 0.78rem;
    }
    .organization-picker {
      display: grid;
      gap: 0.1rem;
      min-width: 14rem;
      margin-right: auto;
    }
    .organization-picker span,
    .user-menu span {
      color: #bed1ce;
      font-size: 0.7rem;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    .organization-picker select {
      padding: 0.35rem 2rem 0.35rem 0.55rem;
      color: white;
      background: #244c52;
      border: 1px solid #527177;
      border-radius: 0.4rem;
    }
    .user-menu {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .user-menu div {
      display: grid;
      text-align: right;
    }
    .menu-button {
      display: none;
      color: white;
      background: none;
      border: 0;
      font-size: 1.4rem;
    }
    .sidebar {
      position: sticky;
      top: 4.25rem;
      height: calc(100vh - 4.25rem);
      display: flex;
      flex-direction: column;
      padding: 1.25rem 0.85rem;
      background: white;
      border-right: 1px solid var(--line);
    }
    nav {
      display: grid;
      gap: 0.2rem;
    }
    nav a,
    nav .future {
      display: block;
      padding: 0.7rem 0.85rem;
      color: var(--ink-muted);
      border-radius: 0.45rem;
      text-decoration: none;
      font-weight: 650;
    }
    nav a:hover,
    nav a:focus-visible,
    nav a.active {
      color: var(--brand-dark);
      background: var(--brand-soft);
    }
    nav .future {
      opacity: 0.62;
      cursor: default;
    }
    .version {
      margin: auto 0.85rem 0;
      color: var(--ink-subtle);
      font-size: 0.75rem;
    }
    .content {
      min-width: 0;
      padding: 2rem clamp(1rem, 3vw, 3rem);
    }
    @media (max-width: 960px) {
      .shell {
        grid-template-columns: 1fr;
      }
      .menu-button {
        display: block;
      }
      .sidebar {
        position: fixed;
        left: 0;
        top: 4.25rem;
        z-index: 4;
        width: 15rem;
        transform: translateX(-105%);
        transition: transform 0.15s ease;
        box-shadow: var(--shadow);
      }
      .shell--menu-open .sidebar {
        transform: translateX(0);
      }
      .user-menu div {
        display: none;
      }
    }
    @media (max-width: 560px) {
      .topbar {
        gap: 0.65rem;
        padding-inline: 0.75rem;
      }
      .brand {
        font-size: 0;
      }
      .organization-picker {
        min-width: 0;
        flex: 1;
      }
      .organization-picker span {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
      }
      .user-menu {
        gap: 0;
      }
      .content {
        padding: 1.25rem 0.85rem;
      }
    }
  `,
})
export class AppShellComponent {
  readonly auth = inject(AuthService);
  readonly organizations = inject(OrganizationContextService);
  private readonly router = inject(Router);
  readonly menuOpen = signal(false);
  readonly displayName = computed(() => {
    const user = this.auth.currentUser();
    return user ? `${user.firstName} ${user.lastName}` : "";
  });
  readonly navigation = computed(() => {
    const role = this.organizations.selectedRole();
    const organizationId = this.organizations.selectedOrganizationId();
    if (!role || !organizationId) return [];
    const items: NavigationItem[] = [
      {
        label: "Grants",
        roles: ["GRANT_MANAGER", "PROGRAM_MANAGER", "VIEWER"],
        route: `/organizations/${organizationId}/grants`,
      },
      {
        label: "Grant Reporting",
        roles: ["GRANT_MANAGER", "PROGRAM_MANAGER"],
        route: `/organizations/${organizationId}/grant-reporting`,
      },
      {
        label: "Volunteers",
        roles: ["VOLUNTEER_COORDINATOR", "PROGRAM_MANAGER", "VIEWER"],
        route: `/organizations/${organizationId}/volunteers`,
      },
      {
        label: "Events",
        roles: [
          "EVENT_COORDINATOR",
          "PROGRAM_MANAGER",
          "VOLUNTEER_COORDINATOR",
          "GRANT_MANAGER",
          "DONATION_MANAGER",
          "VIEWER",
          "VOLUNTEER",
        ],
        route: `/organizations/${organizationId}/events`,
      },
      {
        label: "Donations",
        roles: ["DONATION_MANAGER", "PROGRAM_MANAGER", "VIEWER"],
        route: `/organizations/${organizationId}/donations`,
      },
      {
        label: "Cases",
        roles: ["CASE_MANAGER", "CASE_WORKER", "PROGRAM_MANAGER"],
        route: `/organizations/${organizationId}/cases`,
      },
      {
        label: "Equipment",
        roles: [
          "EQUIPMENT_MANAGER",
          "PROGRAM_MANAGER",
          "EVENT_COORDINATOR",
          "VOLUNTEER_COORDINATOR",
          "VIEWER",
        ],
        route: `/organizations/${organizationId}/equipment`,
      },
      {
        label: "Facilities",
        roles: [
          "FACILITY_MANAGER",
          "PROGRAM_MANAGER",
          "EVENT_COORDINATOR",
          "VIEWER",
        ],
        route: `/organizations/${organizationId}/facilities`,
      },
      {
        label: "Scholarships",
        roles: [
          "SCHOLARSHIP_MANAGER",
          "SCHOLARSHIP_REVIEWER",
          "PROGRAM_MANAGER",
          "VIEWER",
        ],
        route: `/organizations/${organizationId}/scholarships`,
      },
      {
        label: "Food Pantry",
        roles: ["FOOD_PANTRY_MANAGER", "PROGRAM_MANAGER", "VIEWER"],
        route: `/organizations/${organizationId}/food-pantry`,
      },
      {
        label: "Board",
        roles: ["BOARD_MANAGER", "BOARD_MEMBER", "PROGRAM_MANAGER"],
        route: `/organizations/${organizationId}/board`,
      },
    ];
    return role === "ORG_ADMIN"
      ? items
      : items.filter((item) => item.roles.includes(role));
  });

  switchOrganization(event: Event): void {
    const organizationId = (event.target as HTMLSelectElement).value;
    this.organizations.select(organizationId);
    void this.router.navigateByUrl("/dashboard");
  }

  logout(): void {
    this.auth.logout().subscribe();
  }
}
