import { HttpClient } from "@angular/common/http";
import { computed, inject, Injectable, signal } from "@angular/core";
import { forkJoin, map, Observable, of, tap } from "rxjs";
import { API_BASE_URL } from "../http/api-url";
import {
  Organization,
  OrganizationMembership,
  Role,
} from "../models/organization.models";

const SELECTED_ORGANIZATION = "civicops.selectedOrganizationId";

@Injectable({ providedIn: "root" })
export class OrganizationContextService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly organizationsState = signal<Organization[]>([]);
  private readonly membershipsState = signal<OrganizationMembership[]>([]);
  private readonly selectedIdState = signal<string | null>(null);
  private loaded = false;

  readonly organizations = this.organizationsState.asReadonly();
  readonly memberships = this.membershipsState.asReadonly();
  readonly selectedOrganizationId = this.selectedIdState.asReadonly();
  readonly selectedOrganization = computed(() =>
    this.organizationsState().find(
      (organization) => organization.id === this.selectedIdState(),
    ),
  );
  readonly selectedMembership = computed(() =>
    this.membershipsState().find(
      (membership) => membership.organizationId === this.selectedIdState(),
    ),
  );
  readonly selectedRole = computed(
    () => this.selectedMembership()?.role ?? null,
  );

  load(force = false): Observable<Organization[]> {
    if (this.loaded && !force) {
      return of(this.organizationsState());
    }
    return forkJoin({
      organizations: this.http.get<Organization[]>(
        `${this.apiBaseUrl}/organizations`,
      ),
      memberships: this.http.get<OrganizationMembership[]>(
        `${this.apiBaseUrl}/auth/me/memberships`,
      ),
    }).pipe(
      tap(({ organizations, memberships }) => {
        this.organizationsState.set(
          organizations.filter((organization) => organization.active),
        );
        this.membershipsState.set(
          memberships.filter((membership) => membership.active),
        );
        this.loaded = true;
        const persisted = localStorage.getItem(SELECTED_ORGANIZATION);
        const valid =
          organizations.some(
            (organization) =>
              organization.active && organization.id === persisted,
          ) &&
          memberships.some(
            (membership) =>
              membership.active && membership.organizationId === persisted,
          );
        this.select(valid ? persisted : (organizations[0]?.id ?? null));
      }),
      map(({ organizations }) => organizations),
    );
  }

  select(organizationId: string | null): void {
    const valid =
      organizationId !== null &&
      this.organizationsState().some(
        (organization) => organization.id === organizationId,
      ) &&
      this.membershipsState().some(
        (membership) => membership.organizationId === organizationId,
      );
    const selected = valid ? organizationId : null;
    this.selectedIdState.set(selected);
    if (selected) {
      localStorage.setItem(SELECTED_ORGANIZATION, selected);
    } else {
      localStorage.removeItem(SELECTED_ORGANIZATION);
    }
  }

  contains(organizationId: string): boolean {
    return (
      this.organizationsState().some(
        (organization) => organization.id === organizationId,
      ) &&
      this.membershipsState().some(
        (membership) => membership.organizationId === organizationId,
      )
    );
  }

  hasAnyRole(...roles: Role[]): boolean {
    const role = this.selectedRole();
    return role === "ORG_ADMIN" || (role !== null && roles.includes(role));
  }

  clear(): void {
    this.loaded = false;
    this.organizationsState.set([]);
    this.membershipsState.set([]);
    this.selectedIdState.set(null);
    localStorage.removeItem(SELECTED_ORGANIZATION);
  }
}
