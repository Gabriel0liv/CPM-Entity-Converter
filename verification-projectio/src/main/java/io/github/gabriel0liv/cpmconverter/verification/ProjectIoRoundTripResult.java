package io.github.gabriel0liv.cpmconverter.verification;

/** Explicit result of an official CPM save/reopen verification round trip. */
public record ProjectIoRoundTripResult(
    Status status, ProjectIoSnapshot before, ProjectIoSnapshot after, String message) {
  public enum Status {
    PASS,
    FAIL
  }

  public ProjectIoRoundTripResult {
    message = message == null ? "" : message;
  }

  public static ProjectIoRoundTripResult pass(
      ProjectIoSnapshot before, ProjectIoSnapshot after) {
    return new ProjectIoRoundTripResult(Status.PASS, before, after, "");
  }

  public static ProjectIoRoundTripResult fail(ProjectIoSnapshot before, Throwable error) {
    ProjectIoSnapshot failure = ProjectIoSnapshot.failure(error);
    ProjectIoSnapshot safeBefore = before == null ? failure : before;
    String message = failure.failureType();
    if (!failure.failureMessage().isEmpty()) message += ": " + failure.failureMessage();
    return new ProjectIoRoundTripResult(Status.FAIL, safeBefore, failure, message);
  }
}
