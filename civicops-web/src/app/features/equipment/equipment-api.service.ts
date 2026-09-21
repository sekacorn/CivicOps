import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  AssetStatus,
  EquipmentAssetDetail,
  EquipmentAssetSummary,
  EquipmentCategory,
  EquipmentCheckoutDetail,
  EquipmentCheckoutSummary,
  EquipmentCondition,
  EquipmentInventoryReport,
  EquipmentMaintenance,
  EquipmentMaintenanceReport,
  EquipmentUtilizationReport,
} from "../../core/models/equipment.models";

@Injectable({ providedIn: "root" })
export class EquipmentApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  private org(org: string) {
    return `${this.base}/organizations/${org}`;
  }
  categories(org: string) {
    return this.http.get<PageResponse<EquipmentCategory>>(
      `${this.org(org)}/equipment-categories`,
      {
        params: { page: 0, size: 100, sort: "name,asc" },
      },
    );
  }
  createCategory(org: string, value: Record<string, unknown>) {
    return this.http.post<EquipmentCategory>(
      `${this.org(org)}/equipment-categories`,
      value,
    );
  }
  assets(
    org: string,
    filter: {
      status?: AssetStatus | "";
      condition?: EquipmentCondition | "";
      categoryId?: string;
      name?: string;
      page: number;
    },
  ) {
    let params = new HttpParams()
      .set("page", filter.page)
      .set("size", 20)
      .set("sort", "assetTag,asc");
    for (const [key, value] of Object.entries(filter))
      if (key !== "page" && value) params = params.set(key, String(value));
    return this.http.get<PageResponse<EquipmentAssetSummary>>(
      `${this.org(org)}/equipment`,
      { params },
    );
  }
  asset(org: string, id: string) {
    return this.http.get<EquipmentAssetDetail>(
      `${this.org(org)}/equipment/${id}`,
    );
  }
  createAsset(org: string, value: Record<string, unknown>) {
    return this.http.post<EquipmentAssetDetail>(
      `${this.org(org)}/equipment`,
      value,
    );
  }
  retire(org: string, id: string) {
    return this.http.post<EquipmentAssetDetail>(
      `${this.org(org)}/equipment/${id}/retire`,
      {},
    );
  }
  checkout(org: string, id: string, value: Record<string, unknown>) {
    return this.http.post<EquipmentCheckoutDetail>(
      `${this.org(org)}/equipment/${id}/checkouts`,
      value,
    );
  }
  checkouts(org: string, id: string) {
    return this.http.get<PageResponse<EquipmentCheckoutSummary>>(
      `${this.org(org)}/equipment/${id}/checkouts`,
      {
        params: { page: 0, size: 50, sort: "checkedOutAt,desc" },
      },
    );
  }
  checkoutDetail(org: string, id: string) {
    return this.http.get<EquipmentCheckoutDetail>(
      `${this.org(org)}/equipment-checkouts/${id}`,
    );
  }
  checkIn(
    org: string,
    id: string,
    returnCondition: EquipmentCondition,
    notes: string | null,
  ) {
    return this.http.post<EquipmentCheckoutDetail>(
      `${this.org(org)}/equipment-checkouts/${id}/check-in`,
      { returnCondition, notes },
    );
  }
  markLost(org: string, id: string) {
    return this.http.post<EquipmentCheckoutDetail>(
      `${this.org(org)}/equipment-checkouts/${id}/mark-lost`,
      {},
    );
  }
  maintenance(org: string, assetId: string) {
    return this.http.get<PageResponse<EquipmentMaintenance>>(
      `${this.org(org)}/equipment/${assetId}/maintenance`,
      {
        params: { page: 0, size: 50, sort: "startedAt,desc" },
      },
    );
  }
  createMaintenance(
    org: string,
    assetId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<EquipmentMaintenance>(
      `${this.org(org)}/equipment/${assetId}/maintenance`,
      value,
    );
  }
  maintenanceAction(org: string, id: string, action: "start" | "cancel") {
    return this.http.post<EquipmentMaintenance>(
      `${this.org(org)}/equipment-maintenance/${id}/${action}`,
      {},
    );
  }
  completeMaintenance(
    org: string,
    id: string,
    resultingCondition: EquipmentCondition,
  ) {
    return this.http.post<EquipmentMaintenance>(
      `${this.org(org)}/equipment-maintenance/${id}/complete`,
      { resultingCondition },
    );
  }
  inventoryReport(org: string) {
    return this.http.get<EquipmentInventoryReport>(
      `${this.org(org)}/equipment-reports/summary`,
    );
  }
  utilizationReport(org: string) {
    return this.http.get<EquipmentUtilizationReport>(
      `${this.org(org)}/equipment-reports/utilization`,
    );
  }
  maintenanceReport(org: string) {
    return this.http.get<EquipmentMaintenanceReport>(
      `${this.org(org)}/equipment-reports/maintenance`,
    );
  }
}
