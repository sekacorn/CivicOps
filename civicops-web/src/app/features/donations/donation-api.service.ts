import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  CampaignFinancialSummary,
  CampaignStatus,
  DonationCampaign,
  DonationDetail,
  DonationReportSummary,
  DonationSummary,
  DonorDetail,
  DonorSummary,
  DonorType,
  PaymentMethod,
  PaymentMethodTotal,
} from "../../core/models/operations.models";

export interface DonorMutation {
  donorType: DonorType;
  firstName: string | null;
  lastName: string | null;
  organizationName: string | null;
  email: string | null;
  phone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  anonymous: boolean;
  communicationOptOut: boolean;
  notes: string | null;
}
export interface CampaignMutation {
  name: string;
  description: string | null;
  goalAmount: number;
  startDate: string | null;
  endDate: string | null;
}
export interface DonationMutation {
  donorId: string | null;
  anonymous: boolean;
  amount: number;
  donationDate: string;
  paymentMethod: PaymentMethod;
  inKindDescription: string | null;
  campaignId: string | null;
  restricted: boolean;
  restrictionDescription: string | null;
  designation: string | null;
  referenceNumber: string | null;
  receiptNumber: string | null;
  acknowledgementStatus: "NOT_REQUIRED" | "PENDING" | "SENT";
  notes: string | null;
}

@Injectable({ providedIn: "root" })
export class DonationApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  private root(org: string) {
    return `${this.base}/organizations/${org}`;
  }
  donors(org: string, page = 0) {
    return this.http.get<PageResponse<DonorSummary>>(
      `${this.root(org)}/donors`,
      {
        params: { page, size: 20, sort: "createdAt,desc" },
      },
    );
  }
  donor(org: string, id: string) {
    return this.http.get<DonorDetail>(`${this.root(org)}/donors/${id}`);
  }
  createDonor(org: string, value: DonorMutation) {
    return this.http.post<DonorDetail>(`${this.root(org)}/donors`, value);
  }
  updateDonor(org: string, id: string, value: Partial<DonorMutation>) {
    return this.http.patch<DonorDetail>(
      `${this.root(org)}/donors/${id}`,
      value,
    );
  }
  campaigns(org: string, status?: CampaignStatus) {
    let params = new HttpParams()
      .set("page", 0)
      .set("size", 50)
      .set("sort", "name,asc");
    if (status) params = params.set("status", status);
    return this.http.get<PageResponse<DonationCampaign>>(
      `${this.root(org)}/donation-campaigns`,
      { params },
    );
  }
  campaign(org: string, id: string) {
    return this.http.get<DonationCampaign>(
      `${this.root(org)}/donation-campaigns/${id}`,
    );
  }
  createCampaign(org: string, value: CampaignMutation) {
    return this.http.post<DonationCampaign>(
      `${this.root(org)}/donation-campaigns`,
      value,
    );
  }
  updateCampaign(org: string, id: string, value: Partial<CampaignMutation>) {
    return this.http.patch<DonationCampaign>(
      `${this.root(org)}/donation-campaigns/${id}`,
      value,
    );
  }
  transitionCampaign(
    org: string,
    id: string,
    action: "activate" | "close" | "cancel",
  ) {
    return this.http.post<DonationCampaign>(
      `${this.root(org)}/donation-campaigns/${id}/${action}`,
      {},
    );
  }
  donations(org: string, page = 0) {
    return this.http.get<PageResponse<DonationSummary>>(
      `${this.root(org)}/donations`,
      {
        params: { page, size: 20, sort: "donationDate,desc" },
      },
    );
  }
  donation(org: string, id: string) {
    return this.http.get<DonationDetail>(`${this.root(org)}/donations/${id}`);
  }
  createDonation(org: string, value: DonationMutation) {
    return this.http.post<DonationDetail>(`${this.root(org)}/donations`, value);
  }
  reverse(org: string, id: string, reason: string) {
    return this.http.post<DonationDetail>(
      `${this.root(org)}/donations/${id}/reverse`,
      { reason },
    );
  }
  summary(org: string, from?: string, to?: string) {
    let params = new HttpParams();
    if (from) params = params.set("from", from);
    if (to) params = params.set("to", to);
    return this.http.get<DonationReportSummary>(
      `${this.root(org)}/donation-reports/summary`,
      { params },
    );
  }
  paymentMethods(org: string) {
    return this.http.get<PaymentMethodTotal[]>(
      `${this.root(org)}/donation-reports/by-payment-method`,
    );
  }
  campaignFinancial(org: string, id: string) {
    return this.http.get<CampaignFinancialSummary>(
      `${this.root(org)}/donation-campaigns/${id}/financial-summary`,
    );
  }
}
