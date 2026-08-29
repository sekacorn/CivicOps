package org.civicops.core.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "organization")
public class Organization extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String legalName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrganizationType organizationType;

    @Column(length = 320)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String website;

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
    private boolean active = true;

    protected Organization() {
    }

    Organization(String name, String legalName, String description, OrganizationType organizationType,
                 String email, String phone, String website, String addressLine1, String addressLine2,
                 String city, String state, String postalCode, String country) {
        this.name = name;
        this.legalName = legalName;
        this.description = description;
        this.organizationType = organizationType;
        this.email = email;
        this.phone = phone;
        this.website = website;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getName() { return name; }
    public String getLegalName() { return legalName; }
    public String getDescription() { return description; }
    public OrganizationType getOrganizationType() { return organizationType; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getWebsite() { return website; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public boolean isActive() { return active; }
}
