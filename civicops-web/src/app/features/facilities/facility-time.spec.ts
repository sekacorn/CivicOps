import {
  facilityLocalToInstant,
  instantToFacilityLocal,
} from "./facility-time";

describe("facility timezone conversion", () => {
  it("converts New York facility-local summer time without using the browser zone", () => {
    expect(facilityLocalToInstant("2026-07-15T09:30", "America/New_York")).toBe(
      "2026-07-15T13:30:00.000Z",
    );
  });
  it("converts Los Angeles facility-local winter time", () => {
    expect(
      facilityLocalToInstant("2026-01-15T09:30", "America/Los_Angeles"),
    ).toBe("2026-01-15T17:30:00.000Z");
  });
  it("round trips a facility-local time", () => {
    const instant = facilityLocalToInstant(
      "2026-09-14T14:45",
      "America/Chicago",
    );
    expect(instantToFacilityLocal(instant, "America/Chicago")).toBe(
      "2026-09-14T14:45",
    );
  });
});
