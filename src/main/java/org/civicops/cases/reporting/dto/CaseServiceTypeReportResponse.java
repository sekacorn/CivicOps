package org.civicops.cases.reporting.dto;

import java.math.BigDecimal;
import org.civicops.cases.service.CaseServiceType;

public record CaseServiceTypeReportResponse(
    CaseServiceType serviceType, long serviceCount, BigDecimal totalValueAmount) {}
