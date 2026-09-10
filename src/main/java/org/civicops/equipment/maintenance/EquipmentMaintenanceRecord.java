package org.civicops.equipment.maintenance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.equipment.asset.EquipmentAsset;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "equipment_maintenance")
public class EquipmentMaintenanceRecord extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "equipment_asset_id", nullable = false)
  private EquipmentAsset equipmentAsset;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MaintenanceType maintenanceType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private Instant startedAt;

  private Instant completedAt;

  @Column(precision = 19, scale = 2)
  private BigDecimal cost;

  @Column(length = 200)
  private String vendor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MaintenanceStatus status = MaintenanceStatus.OPEN;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false)
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "completed_by_user_id")
  private User completedBy;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected EquipmentMaintenanceRecord() {}

  public EquipmentMaintenanceRecord(
      Organization o,
      EquipmentAsset a,
      MaintenanceType type,
      String description,
      Instant started,
      BigDecimal cost,
      String vendor,
      User creator,
      String notes) {
    organization = o;
    equipmentAsset = a;
    maintenanceType = type;
    this.description = description;
    startedAt = started;
    this.cost = cost;
    this.vendor = vendor;
    createdBy = creator;
    this.notes = notes;
  }

  public Organization getOrganization() {
    return organization;
  }

  public EquipmentAsset getEquipmentAsset() {
    return equipmentAsset;
  }

  public MaintenanceType getMaintenanceType() {
    return maintenanceType;
  }

  public String getDescription() {
    return description;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public BigDecimal getCost() {
    return cost;
  }

  public String getVendor() {
    return vendor;
  }

  public MaintenanceStatus getStatus() {
    return status;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public User getCompletedBy() {
    return completedBy;
  }

  public String getNotes() {
    return notes;
  }

  public void start() {
    if (status != MaintenanceStatus.OPEN)
      throw new BusinessRuleException(
          "INVALID_MAINTENANCE_TRANSITION", "Only open maintenance can start");
    status = MaintenanceStatus.IN_PROGRESS;
  }

  public void complete(Instant now, User user) {
    if (status != MaintenanceStatus.OPEN && status != MaintenanceStatus.IN_PROGRESS)
      throw new BusinessRuleException(
          "INVALID_MAINTENANCE_TRANSITION", "Maintenance cannot be completed");
    status = MaintenanceStatus.COMPLETED;
    completedAt = now;
    completedBy = user;
  }

  public void cancel() {
    if (status == MaintenanceStatus.COMPLETED || status == MaintenanceStatus.CANCELLED)
      throw new BusinessRuleException(
          "INVALID_MAINTENANCE_TRANSITION", "Maintenance cannot be cancelled");
    status = MaintenanceStatus.CANCELLED;
  }
}
