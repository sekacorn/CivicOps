package org.civicops.events.reporting.dto;

public record EventSummaryReportResponse(
    long upcomingEvents,
    long openRegistrationEvents,
    long registeredAttendees,
    long waitlistedAttendees,
    long completedEvents) {}
