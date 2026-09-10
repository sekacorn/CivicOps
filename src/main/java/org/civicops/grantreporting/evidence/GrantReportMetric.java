package org.civicops.grantreporting.evidence;

public enum GrantReportMetric {
  GRANT_AWARD_AMOUNT("grant.award_amount", "Grant award amount"),
  GRANT_TOTAL_SPENT("grant.total_spent", "Grant total spent"),
  GRANT_REMAINING_BALANCE("grant.remaining_balance", "Grant remaining balance"),
  GRANT_UTILIZATION_PERCENT("grant.utilization_percent", "Grant utilization"),
  VOLUNTEER_APPROVED_HOURS("volunteer.approved_hours", "Approved volunteer hours"),
  EVENT_EVENTS_COMPLETED("event.events_completed", "Grant-linked events completed"),
  EVENT_ATTENDEES("event.attendees", "Attendees at grant-linked events"),
  EVENT_ATTENDANCE_RATE("event.attendance_rate", "Grant-linked event attendance rate"),
  DONATION_AMOUNT_RAISED("donation.amount_raised", "Linked campaign amount raised"),
  DONATION_COUNT("donation.count", "Linked campaign donation count"),
  CASE_CASES_CLOSED("case.cases_closed", "Cases closed"),
  CASE_SERVICES_PROVIDED("case.services_provided", "Services provided"),
  SCHOLARSHIP_APPLICATIONS_SUBMITTED(
      "scholarship.applications_submitted", "Scholarship applications submitted"),
  SCHOLARSHIP_AWARDS_COUNT("scholarship.awards_count", "Scholarship awards"),
  SCHOLARSHIP_TOTAL_AWARDED("scholarship.total_awarded", "Scholarship amount awarded"),
  FOODPANTRY_HOUSEHOLDS_SERVED("foodpantry.households_served", "Food pantry households served"),
  FOODPANTRY_VISITS_COMPLETED("foodpantry.visits_completed", "Food pantry visits completed"),
  FOODPANTRY_QUANTITY_DISTRIBUTED("foodpantry.quantity_distributed", "Food quantity distributed"),
  MANUAL_VALUE("manual.value", "Manual evidence");
  private final String key, label;

  GrantReportMetric(String k, String l) {
    key = k;
    label = l;
  }

  public String key() {
    return key;
  }

  public String label() {
    return label;
  }

  public static GrantReportMetric fromKey(String key) {
    for (var m : values()) if (m.key.equals(key)) return m;
    throw new IllegalArgumentException("Unsupported metric key: " + key);
  }
}
