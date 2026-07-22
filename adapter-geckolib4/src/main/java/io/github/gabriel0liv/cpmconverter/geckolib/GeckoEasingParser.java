package io.github.gabriel0liv.cpmconverter.geckolib;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import java.util.*;

public final class GeckoEasingParser {
  public Result<EasingIR> parse(
      JsonNode node, SourcePath source, String pointer, String clip, String bone, String channel) {
    if (node == null || !node.isObject()) return Result.success(EasingIR.linear());
    String name =
        node.has("easing")
            ? node.get("easing").isTextual() ? node.get("easing").textValue() : null
            : "linear";
    if (name == null)
      return Result.failure(
          diag(source, DiagnosticCodes.ANIM_CHANNEL_INVALID, pointer + "/easing"));
    EasingKindIR kind = map(name);
    if (kind == null)
      return Result.failure(
          diag(source, DiagnosticCodes.ANIM_CUSTOM_EASING_UNSUPPORTED, pointer + "/easing"));
    List<Double> args = new ArrayList<>();
    JsonNode a = node.get("easingArgs");
    if (a != null) {
      if (a.isArray())
        for (int i = 0; i < a.size(); i++) {
          Double v = number(a.get(i));
          if (v == null)
            return Result.failure(
                diag(source, DiagnosticCodes.ANIM_CHANNEL_INVALID, pointer + "/easingArgs/" + i));
          args.add(v);
        }
      else if (a.isTextual()) {
        Double v = number(a);
        if (v == null)
          return Result.failure(
              diag(source, DiagnosticCodes.ANIM_CHANNEL_INVALID, pointer + "/easingArgs"));
        args.add(v);
      } else
        return Result.failure(
            diag(source, DiagnosticCodes.ANIM_CHANNEL_INVALID, pointer + "/easingArgs"));
    }
    if (kind == EasingKindIR.STEP && !args.isEmpty() && args.get(0) < 2)
      return Result.failure(
          diag(source, DiagnosticCodes.ANIM_CHANNEL_INVALID, pointer + "/easingArgs/0"));
    return Result.success(new EasingIR(kind, args));
  }

  private static Double number(JsonNode n) {
    if (n == null) return null;
    if (n.isNumber()) return n.doubleValue();
    if (n.isTextual())
      try {
        double d = Double.parseDouble(n.textValue());
        return Double.isFinite(d) ? d : null;
      } catch (NumberFormatException e) {
        return null;
      }
    return null;
  }

  static EasingKindIR map(String raw) {
    String n = raw.toLowerCase(Locale.ROOT).replace("_", "");
    if (n.equals("none")) n = "linear";
    return switch (n) {
      case "linear" -> EasingKindIR.LINEAR;
      case "step" -> EasingKindIR.STEP;
      case "catmullrom" -> EasingKindIR.CATMULLROM;
      case "easeinsine" -> EasingKindIR.EASE_IN_SINE;
      case "easeoutsine" -> EasingKindIR.EASE_OUT_SINE;
      case "easeinoutsine" -> EasingKindIR.EASE_IN_OUT_SINE;
      case "easeinquad" -> EasingKindIR.EASE_IN_QUAD;
      case "easeoutquad" -> EasingKindIR.EASE_OUT_QUAD;
      case "easeinoutquad" -> EasingKindIR.EASE_IN_OUT_QUAD;
      case "easeincubic" -> EasingKindIR.EASE_IN_CUBIC;
      case "easeoutcubic" -> EasingKindIR.EASE_OUT_CUBIC;
      case "easeinoutcubic" -> EasingKindIR.EASE_IN_OUT_CUBIC;
      case "easeinquart" -> EasingKindIR.EASE_IN_QUART;
      case "easeoutquart" -> EasingKindIR.EASE_OUT_QUART;
      case "easeinoutquart" -> EasingKindIR.EASE_IN_OUT_QUART;
      case "easeinquint" -> EasingKindIR.EASE_IN_QUINT;
      case "easeoutquint" -> EasingKindIR.EASE_OUT_QUINT;
      case "easeinoutquint" -> EasingKindIR.EASE_IN_OUT_QUINT;
      case "easeinexpo" -> EasingKindIR.EASE_IN_EXPO;
      case "easeoutexpo" -> EasingKindIR.EASE_OUT_EXPO;
      case "easeinoutexpo" -> EasingKindIR.EASE_IN_OUT_EXPO;
      case "easeincirc" -> EasingKindIR.EASE_IN_CIRC;
      case "easeoutcirc" -> EasingKindIR.EASE_OUT_CIRC;
      case "easeinoutcirc" -> EasingKindIR.EASE_IN_OUT_CIRC;
      case "easeinback" -> EasingKindIR.EASE_IN_BACK;
      case "easeoutback" -> EasingKindIR.EASE_OUT_BACK;
      case "easeinoutback" -> EasingKindIR.EASE_IN_OUT_BACK;
      case "easeinelastic" -> EasingKindIR.EASE_IN_ELASTIC;
      case "easeoutelastic" -> EasingKindIR.EASE_OUT_ELASTIC;
      case "easeinoutelastic" -> EasingKindIR.EASE_IN_OUT_ELASTIC;
      case "easeinbounce" -> EasingKindIR.EASE_IN_BOUNCE;
      case "easeoutbounce" -> EasingKindIR.EASE_OUT_BOUNCE;
      case "easeinoutbounce" -> EasingKindIR.EASE_IN_OUT_BOUNCE;
      default -> null;
    };
  }

  private static Diagnostic diag(SourcePath s, String code, String p) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(code),
        new SourceLocation(s, null, null, p, null),
        "Invalid easing",
        "Use a supported GeckoLib easing",
        null,
        null,
        new TreeMap<>());
  }
}
