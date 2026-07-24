package io.github.gabriel0liv.cpmconverter.verification;

import com.google.gson.*;
import java.nio.file.*;

final class T304ConformanceTestSupport {
  private T304ConformanceTestSupport() {}

  static JsonArray projects() throws Exception {
    Path report = Path.of("build/t304/projectio/projectio-report.json");
    if (!Files.exists(report)) report = Path.of("../../build/t304/projectio/projectio-report.json");
    return new JsonParser()
        .parse(Files.readString(report))
        .getAsJsonObject()
        .getAsJsonArray("projects");
  }

  static JsonObject project(String name) throws Exception {
    for (JsonElement entry : projects())
      if (entry.getAsJsonObject().get("name").getAsString().equals(name))
        return entry.getAsJsonObject();
    throw new AssertionError("missing project " + name);
  }
}
