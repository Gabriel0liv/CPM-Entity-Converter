package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

public final class ConstantMolangEvaluator {
  public Result<Double> evaluate(
      String expression, SourceLocation source, Map<String, String> context) {
    if (expression == null || expression.isBlank())
      return failure(source, DiagnosticCodes.ANIM_MOLANG_PARSE_ERROR);
    String s = expression.trim();
    if (s.regionMatches(true, 0, "return", 0, 6)) s = s.substring(6).trim();
    if (s.endsWith(";")) s = s.substring(0, s.length() - 1).trim();
    String low = s.toLowerCase(Locale.ROOT);
    if (low.matches(
            ".*(?:query\\.|q\\.|variable\\.|v\\.|temp\\.|t\\.|context\\.|math\\.(random|random_integer|die_roll|die_roll_integer)\\b|[a-z_][a-z0-9_]*\\b).*")
        && !low.matches(
            ".*math\\.(pi|abs|ceil|floor|round|trunc|sqrt|exp|ln|pow|min|max|clamp|mod|lerp|sin|cos)\\b.*"))
      return failure(source, DiagnosticCodes.ANIM_DYNAMIC_MOLANG_UNSUPPORTED);
    try {
      double v = new Parser(s).parse();
      if (!Double.isFinite(v)) throw new IllegalArgumentException();
      return Result.success(v);
    } catch (Exception e) {
      return failure(source, DiagnosticCodes.ANIM_MOLANG_PARSE_ERROR);
    }
  }

  private Result<Double> failure(SourceLocation s, String c) {
    return Result.failure(
        new Diagnostic(
            Severity.ERROR,
            DiagnosticCode.fromCatalog(c),
            s,
            "Molang expression rejected",
            "Use a numeric constant expression",
            null,
            null,
            new java.util.TreeMap<>()));
  }

  private static final class Parser {
    final String s;
    int p;

    Parser(String s) {
      this.s = s;
    }

    double parse() {
      double v = expr();
      ws();
      if (p != s.length()) throw new IllegalArgumentException();
      return v;
    }

    void ws() {
      while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++;
    }

    double expr() {
      double v = term();
      for (; ; ) {
        ws();
        if (p >= s.length()) return v;
        char c = s.charAt(p);
        if (c != '+' && c != '-') return v;
        p++;
        double r = term();
        v = c == '+' ? v + r : v - r;
      }
    }

    double term() {
      double v = pow();
      for (; ; ) {
        ws();
        if (p >= s.length()) return v;
        char c = s.charAt(p);
        if (c != '*' && c != '/' && c != '%') return v;
        p++;
        double r = pow();
        if ((c == '/' || c == '%') && r == 0) throw new IllegalArgumentException();
        v = c == '*' ? v * r : c == '/' ? v / r : v % r;
      }
    }

    double pow() {
      double v = unary();
      ws();
      if (p < s.length() && s.charAt(p) == '^') {
        p++;
        v = Math.pow(v, pow());
      }
      return v;
    }

    double unary() {
      ws();
      if (p < s.length() && (s.charAt(p) == '+' || s.charAt(p) == '-')) {
        char c = s.charAt(p++);
        double v = unary();
        return c == '-' ? -v : v;
      }
      return primary();
    }

    double primary() {
      ws();
      if (p >= s.length()) throw new IllegalArgumentException();
      if (s.charAt(p) == '(') {
        p++;
        double v = expr();
        ws();
        if (p >= s.length() || s.charAt(p++) != ')') throw new IllegalArgumentException();
        return v;
      }
      int st = p;
      while (p < s.length()
          && (Character.isLetterOrDigit(s.charAt(p)) || s.charAt(p) == '.' || s.charAt(p) == '_'))
        p++;
      String n = s.substring(st, p);
      ws();
      if (p < s.length() && s.charAt(p) == '(') {
        p++;
        List<Double> a = new ArrayList<>();
        ws();
        if (p < s.length() && s.charAt(p) != ')')
          for (; ; ) {
            a.add(expr());
            ws();
            if (p >= s.length()) throw new IllegalArgumentException();
            if (s.charAt(p++) == ')') break;
            if (s.charAt(p - 1) != ',') throw new IllegalArgumentException();
          }
        else {
          if (p >= s.length() || s.charAt(p++) != ')') throw new IllegalArgumentException();
        }
        return fn(n, a);
      }
      if (n.equalsIgnoreCase("math.pi")) return Math.PI;
      return Double.parseDouble(n);
    }

    double fn(String n, List<Double> a) {
      String x = n.toLowerCase(Locale.ROOT);
      return switch (x) {
        case "math.abs" -> one(a, Math::abs);
        case "math.ceil" -> one(a, Math::ceil);
        case "math.floor" -> one(a, Math::floor);
        case "math.round" -> one(a, v -> (double) Math.round(v));
        case "math.trunc" -> one(a, v -> v < 0 ? Math.ceil(v) : Math.floor(v));
        case "math.sqrt" -> one(a, Math::sqrt);
        case "math.exp" -> one(a, Math::exp);
        case "math.ln" -> one(a, Math::log);
        case "math.pow" -> two(a, Math::pow);
        case "math.min" -> two(a, Math::min);
        case "math.max" -> two(a, Math::max);
        case "math.clamp" -> three(a, (v, l, h) -> Math.max(l, Math.min(h, v)));
        case "math.mod" -> two(a, (v, w) -> v % w);
        case "math.lerp" -> three(a, (v, w, t) -> v + (w - v) * t);
        case "math.sin" -> one(a, v -> Math.sin(Math.toRadians(v)));
        case "math.cos" -> one(a, v -> Math.cos(Math.toRadians(v)));
        default -> throw new IllegalArgumentException();
      };
    }

    double one(List<Double> a, java.util.function.DoubleUnaryOperator f) {
      if (a.size() != 1) throw new IllegalArgumentException();
      return f.applyAsDouble(a.get(0));
    }

    double two(List<Double> a, java.util.function.DoubleBinaryOperator f) {
      if (a.size() != 2) throw new IllegalArgumentException();
      return f.applyAsDouble(a.get(0), a.get(1));
    }

    double three(List<Double> a, Tri f) {
      if (a.size() != 3) throw new IllegalArgumentException();
      return f.x(a.get(0), a.get(1), a.get(2));
    }

    interface Tri {
      double x(double a, double b, double c);
    }
  }
}
