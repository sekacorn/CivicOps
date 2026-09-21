import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  CreateGrantExpense,
  GrantDetail,
  GrantExpense,
  GrantFinancialSummary,
  GrantMutation,
  GrantStatus,
  GrantSummary,
  OrganizationGrantSummary,
} from "../../core/models/grant.models";

export interface GrantFilters {
  status?: GrantStatus | "";
  grantor?: string;
  restricted?: boolean | null;
  page: number;
  size: number;
  sort: string;
  direction: "asc" | "desc";
}

@Injectable({ providedIn: "root" })
export class GrantApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(
    organizationId: string,
    filters: GrantFilters,
  ): Observable<PageResponse<GrantSummary>> {
    let params = new HttpParams()
      .set("page", filters.page)
      .set("size", filters.size)
      .set("sort", `${filters.sort},${filters.direction}`);
    if (filters.status) params = params.set("status", filters.status);
    if (filters.grantor?.trim())
      params = params.set("grantor", filters.grantor.trim());
    if (filters.restricted !== null && filters.restricted !== undefined) {
      params = params.set("restricted", filters.restricted);
    }
    return this.http.get<PageResponse<GrantSummary>>(
      this.grantsUrl(organizationId),
      { params },
    );
  }

  detail(organizationId: string, grantId: string): Observable<GrantDetail> {
    return this.http.get<GrantDetail>(
      `${this.grantsUrl(organizationId)}/${grantId}`,
    );
  }

  create(
    organizationId: string,
    grant: GrantMutation,
  ): Observable<GrantDetail> {
    return this.http.post<GrantDetail>(this.grantsUrl(organizationId), grant);
  }

  update(
    organizationId: string,
    grantId: string,
    grant: GrantMutation,
  ): Observable<GrantDetail> {
    return this.http.patch<GrantDetail>(
      `${this.grantsUrl(organizationId)}/${grantId}`,
      grant,
    );
  }

  transition(
    organizationId: string,
    grantId: string,
    action: string,
  ): Observable<GrantDetail> {
    return this.http.post<GrantDetail>(
      `${this.grantsUrl(organizationId)}/${grantId}/${action}`,
      {},
    );
  }

  expenses(
    organizationId: string,
    grantId: string,
    page = 0,
  ): Observable<PageResponse<GrantExpense>> {
    const params = new HttpParams()
      .set("page", page)
      .set("size", 20)
      .set("sort", "expenseDate,desc");
    return this.http.get<PageResponse<GrantExpense>>(
      `${this.grantsUrl(organizationId)}/${grantId}/expenses`,
      { params },
    );
  }

  addExpense(
    organizationId: string,
    grantId: string,
    expense: CreateGrantExpense,
  ): Observable<GrantExpense> {
    return this.http.post<GrantExpense>(
      `${this.grantsUrl(organizationId)}/${grantId}/expenses`,
      expense,
    );
  }

  financialSummary(
    organizationId: string,
    grantId: string,
  ): Observable<GrantFinancialSummary> {
    return this.http.get<GrantFinancialSummary>(
      `${this.grantsUrl(organizationId)}/${grantId}/financial-summary`,
    );
  }

  portfolioSummary(
    organizationId: string,
  ): Observable<OrganizationGrantSummary> {
    return this.http.get<OrganizationGrantSummary>(
      `${this.apiBaseUrl}/organizations/${organizationId}/grant-reports/summary`,
    );
  }

  private grantsUrl(organizationId: string): string {
    return `${this.apiBaseUrl}/organizations/${organizationId}/grants`;
  }
}
