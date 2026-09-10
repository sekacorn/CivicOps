package org.civicops.equipment.asset;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.equipment.category.EquipmentCategory;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "equipment_asset")
public class EquipmentAsset extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 100)
  private String assetTag;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private EquipmentCategory category;

  @Column(length = 150)
  private String manufacturer;

  @Column(length = 150)
  private String model;

  @Column(length = 150)
  private String serialNumber;

  private LocalDate purchaseDate;

  @Column(precision = 19, scale = 2)
  private BigDecimal purchaseValue;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EquipmentCondition condition;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AssetStatus status = AssetStatus.AVAILABLE;

  @Column(length = 250)
  private String location;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected EquipmentAsset() {}

  public EquipmentAsset(
      Organization o,
      String tag,
      String name,
      String description,
      EquipmentCategory category,
      String manufacturer,
      String model,
      String serial,
      LocalDate purchaseDate,
      BigDecimal purchaseValue,
      EquipmentCondition condition,
      String location,
      String notes) {
    organization = o;
    assetTag = tag;
    this.name = name;
    this.description = description;
    this.category = category;
    this.manufacturer = manufacturer;
    this.model = model;
    serialNumber = serial;
    this.purchaseDate = purchaseDate;
    this.purchaseValue = purchaseValue;
    this.condition = condition;
    this.location = location;
    this.notes = notes;
  }

  public Organization getOrganization() {
    return organization;
  }

  public String getAssetTag() {
    return assetTag;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public EquipmentCategory getCategory() {
    return category;
  }

  public String getManufacturer() {
    return manufacturer;
  }

  public String getModel() {
    return model;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public LocalDate getPurchaseDate() {
    return purchaseDate;
  }

  public BigDecimal getPurchaseValue() {
    return purchaseValue;
  }

  public EquipmentCondition getCondition() {
    return condition;
  }

  public AssetStatus getStatus() {
    return status;
  }

  public String getLocation() {
    return location;
  }

  public String getNotes() {
    return notes;
  }

  public void update(
      String name,
      String description,
      EquipmentCategory category,
      String manufacturer,
      String model,
      String serial,
      LocalDate date,
      BigDecimal value,
      EquipmentCondition condition,
      String location,
      String notes) {
    if (status == AssetStatus.RETIRED)
      throw new BusinessRuleException(
          "RETIRED_ASSET_NOT_EDITABLE", "Retired equipment cannot be edited");
    if (name != null) this.name = name;
    if (description != null) this.description = description;
    if (category != null) this.category = category;
    if (manufacturer != null) this.manufacturer = manufacturer;
    if (model != null) this.model = model;
    if (serial != null) serialNumber = serial;
    if (date != null) purchaseDate = date;
    if (value != null) purchaseValue = value;
    if (condition != null) this.condition = condition;
    if (location != null) this.location = location;
    if (notes != null) this.notes = notes;
  }

  public void checkout() {
    if (status != AssetStatus.AVAILABLE)
      throw new BusinessRuleException(
          "ASSET_NOT_AVAILABLE", "Equipment must be available for checkout");
    status = AssetStatus.CHECKED_OUT;
  }

  public void checkIn(EquipmentCondition returned) {
    if (status != AssetStatus.CHECKED_OUT)
      throw new BusinessRuleException("ASSET_NOT_CHECKED_OUT", "Equipment is not checked out");
    condition = returned;
    status = returned.usable() ? AssetStatus.AVAILABLE : AssetStatus.MAINTENANCE;
  }

  public void markLost() {
    if (status != AssetStatus.CHECKED_OUT)
      throw new BusinessRuleException(
          "ASSET_NOT_CHECKED_OUT", "Only checked-out equipment can be marked lost");
    status = AssetStatus.LOST;
  }

  public void beginMaintenance() {
    if (status == AssetStatus.CHECKED_OUT
        || status == AssetStatus.LOST
        || status == AssetStatus.RETIRED)
      throw new BusinessRuleException(
          "ASSET_MAINTENANCE_NOT_ALLOWED", "Asset cannot enter maintenance from " + status);
    status = AssetStatus.MAINTENANCE;
  }

  public void completeMaintenance(EquipmentCondition resulting) {
    if (status != AssetStatus.MAINTENANCE)
      throw new BusinessRuleException("ASSET_NOT_IN_MAINTENANCE", "Asset is not in maintenance");
    condition = resulting;
    if (resulting.usable()) status = AssetStatus.AVAILABLE;
  }

  public void cancelMaintenance() {
    if (status == AssetStatus.MAINTENANCE && condition.usable()) status = AssetStatus.AVAILABLE;
  }

  public void retire() {
    if (status == AssetStatus.CHECKED_OUT)
      throw new BusinessRuleException(
          "ACTIVE_CHECKOUT_PREVENTS_RETIREMENT", "Checked-out equipment cannot be retired");
    if (status == AssetStatus.RETIRED)
      throw new BusinessRuleException("ASSET_ALREADY_RETIRED", "Equipment is already retired");
    status = AssetStatus.RETIRED;
  }
}
