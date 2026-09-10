package org.civicops.donations.donor;

import jakarta.persistence.*;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "donor")
public class Donor extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private DonorType donorType;

  @Column(length = 100)
  private String firstName;

  @Column(length = 100)
  private String lastName;

  @Column(length = 200)
  private String organizationName;

  @Column(length = 320)
  private String email;

  @Column(length = 50)
  private String phone;

  @Column(length = 250)
  private String addressLine1;

  @Column(length = 250)
  private String addressLine2;

  @Column(length = 120)
  private String city;

  @Column(length = 120)
  private String state;

  @Column(length = 30)
  private String postalCode;

  @Column(length = 2)
  private String country;

  @Column(nullable = false)
  private boolean anonymous;

  @Column(nullable = false)
  private boolean communicationOptOut;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected Donor() {}

  public Donor(
      Organization organization,
      DonorType donorType,
      String firstName,
      String lastName,
      String organizationName,
      String email,
      String phone,
      String addressLine1,
      String addressLine2,
      String city,
      String state,
      String postalCode,
      String country,
      boolean anonymous,
      boolean communicationOptOut,
      String notes) {
    this.organization = organization;
    this.donorType = donorType;
    this.firstName = firstName;
    this.lastName = lastName;
    this.organizationName = organizationName;
    this.email = email;
    this.phone = phone;
    this.addressLine1 = addressLine1;
    this.addressLine2 = addressLine2;
    this.city = city;
    this.state = state;
    this.postalCode = postalCode;
    this.country = country;
    this.anonymous = anonymous;
    this.communicationOptOut = communicationOptOut;
    this.notes = notes;
  }

  public Organization getOrganization() {
    return organization;
  }

  public DonorType getDonorType() {
    return donorType;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public String getAddressLine1() {
    return addressLine1;
  }

  public String getAddressLine2() {
    return addressLine2;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getCountry() {
    return country;
  }

  public boolean isAnonymous() {
    return anonymous;
  }

  public boolean isCommunicationOptOut() {
    return communicationOptOut;
  }

  public String getNotes() {
    return notes;
  }

  public String displayName() {
    if (anonymous) return "Anonymous donor";
    if (donorType == DonorType.INDIVIDUAL) return (firstName + " " + lastName).trim();
    return organizationName;
  }

  public void update(
      String firstName,
      String lastName,
      String organizationName,
      String email,
      String phone,
      String addressLine1,
      String addressLine2,
      String city,
      String state,
      String postalCode,
      String country,
      Boolean communicationOptOut,
      String notes) {
    if (firstName != null) this.firstName = firstName;
    if (lastName != null) this.lastName = lastName;
    if (organizationName != null) this.organizationName = organizationName;
    if (email != null) this.email = email;
    if (phone != null) this.phone = phone;
    if (addressLine1 != null) this.addressLine1 = addressLine1;
    if (addressLine2 != null) this.addressLine2 = addressLine2;
    if (city != null) this.city = city;
    if (state != null) this.state = state;
    if (postalCode != null) this.postalCode = postalCode;
    if (country != null) this.country = country;
    if (communicationOptOut != null) this.communicationOptOut = communicationOptOut;
    if (notes != null) this.notes = notes;
  }
}
