package org.civicops.cases.client;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "client")
public class Client extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(name = "external_reference_number", length = 100)
  private String externalReferenceNumber;

  @Column(nullable = false, length = 100)
  private String firstName;

  @Column(nullable = false, length = 100)
  private String lastName;

  @Column(length = 100)
  private String preferredName;

  private LocalDate dateOfBirth;

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

  @Column(length = 30)
  private String preferredContactMethod;

  @Column(nullable = false)
  private boolean active = true;

  protected Client() {}

  public Client(
      Organization o,
      String ref,
      String first,
      String last,
      String preferred,
      LocalDate dob,
      String email,
      String phone,
      String line1,
      String line2,
      String city,
      String state,
      String postal,
      String country,
      String contact) {
    organization = o;
    externalReferenceNumber = ref;
    firstName = first;
    lastName = last;
    preferredName = preferred;
    dateOfBirth = dob;
    this.email = email;
    this.phone = phone;
    addressLine1 = line1;
    addressLine2 = line2;
    this.city = city;
    this.state = state;
    postalCode = postal;
    this.country = country;
    preferredContactMethod = contact;
  }

  public Organization getOrganization() {
    return organization;
  }

  public String getExternalReferenceNumber() {
    return externalReferenceNumber;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getPreferredName() {
    return preferredName;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
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

  public String getPreferredContactMethod() {
    return preferredContactMethod;
  }

  public boolean isActive() {
    return active;
  }

  public String displayName() {
    return preferredName == null ? (firstName + " " + lastName).trim() : preferredName;
  }

  public void update(
      String ref,
      String first,
      String last,
      String preferred,
      LocalDate dob,
      String email,
      String phone,
      String line1,
      String line2,
      String city,
      String state,
      String postal,
      String country,
      String contact,
      Boolean active) {
    if (ref != null) externalReferenceNumber = ref;
    if (first != null) firstName = first;
    if (last != null) lastName = last;
    if (preferred != null) preferredName = preferred;
    if (dob != null) dateOfBirth = dob;
    if (email != null) this.email = email;
    if (phone != null) this.phone = phone;
    if (line1 != null) addressLine1 = line1;
    if (line2 != null) addressLine2 = line2;
    if (city != null) this.city = city;
    if (state != null) this.state = state;
    if (postal != null) postalCode = postal;
    if (country != null) this.country = country;
    if (contact != null) preferredContactMethod = contact;
    if (active != null) this.active = active;
  }
}
