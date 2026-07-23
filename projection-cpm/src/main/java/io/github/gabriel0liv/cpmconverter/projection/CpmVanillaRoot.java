package io.github.gabriel0liv.cpmconverter.projection;

public enum CpmVanillaRoot {
  HEAD("head", 0),
  BODY("body", 1),
  LEFT_ARM("left_arm", 2),
  RIGHT_ARM("right_arm", 3),
  LEFT_LEG("left_leg", 4),
  RIGHT_LEG("right_leg", 5);
  private final String id;
  private final int reservedId;

  CpmVanillaRoot(String id, int reservedId) {
    this.id = id;
    this.reservedId = reservedId;
  }

  public String id() {
    return id;
  }

  public int reservedId() {
    return reservedId;
  }
}
