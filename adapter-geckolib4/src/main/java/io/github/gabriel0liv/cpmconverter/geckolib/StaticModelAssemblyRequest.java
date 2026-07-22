package io.github.gabriel0liv.cpmconverter.geckolib;

public record StaticModelAssemblyRequest(PngValidationRequest png) {
  public StaticModelAssemblyRequest {
    if (png == null) png = PngValidationRequest.defaults();
  }

  public static StaticModelAssemblyRequest defaults() {
    return new StaticModelAssemblyRequest(PngValidationRequest.defaults());
  }
}
