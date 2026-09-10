package org.civicops.scholarships.applicant;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "scholarship_applicant")
public class ScholarshipApplicant extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false, length = 100)
  private String firstName;

  @Column(nullable = false, length = 100)
  private String lastName;

  @Column(length = 100)
  private String preferredName;

  @Column(nullable = false, length = 320)
  private String email;

  @Column(nullable = false, length = 320)
  private String normalizedEmail;

  @Column(length = 50)
  private String phone;

  private LocalDate dateOfBirth;

  @Column(columnDefinition = "TEXT")
  private String address;

  @Column(length = 200)
  private String schoolName;

  private Integer graduationYear;

  @Column(length = 100)
  private String studentId;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected ScholarshipApplicant() {}

  public ScholarshipApplicant(
      Organization o,
      User u,
      String f,
      String l,
      String preferred,
      String email,
      String normalized,
      String phone,
      LocalDate dob,
      String address,
      String school,
      Integer year,
      String student,
      String notes) {
    organization = o;
    user = u;
    firstName = f;
    lastName = l;
    preferredName = preferred;
    this.email = email;
    normalizedEmail = normalized;
    this.phone = phone;
    dateOfBirth = dob;
    this.address = address;
    schoolName = school;
    graduationYear = year;
    studentId = student;
    this.notes = notes;
  }

  public void update(
      String f,
      String l,
      String preferred,
      String email,
      String normalized,
      String phone,
      LocalDate dob,
      String address,
      String school,
      Integer year,
      String student,
      String notes) {
    if (f != null) firstName = f;
    if (l != null) lastName = l;
    if (preferred != null) preferredName = preferred;
    if (email != null) {
      this.email = email;
      normalizedEmail = normalized;
    }
    if (phone != null) this.phone = phone;
    if (dob != null) dateOfBirth = dob;
    if (address != null) this.address = address;
    if (school != null) schoolName = school;
    if (year != null) graduationYear = year;
    if (student != null) studentId = student;
    if (notes != null) this.notes = notes;
  }

  public Organization getOrganization() {
    return organization;
  }

  public User getUser() {
    return user;
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

  public String getEmail() {
    return email;
  }

  public String getNormalizedEmail() {
    return normalizedEmail;
  }

  public String getPhone() {
    return phone;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public String getAddress() {
    return address;
  }

  public String getSchoolName() {
    return schoolName;
  }

  public Integer getGraduationYear() {
    return graduationYear;
  }

  public String getStudentId() {
    return studentId;
  }

  public String getNotes() {
    return notes;
  }

  public String displayName() {
    return preferredName == null ? firstName + " " + lastName : preferredName + " " + lastName;
  }
}
