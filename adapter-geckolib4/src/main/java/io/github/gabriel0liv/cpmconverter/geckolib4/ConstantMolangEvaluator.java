package io.github.gabriel0liv.cpmconverter.geckolib4;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Evaluates the deliberately small, provably constant Molang subset accepted by the offline MVP.
 */
final class ConstantMolangEvaluator {
  private static final Pattern RUNTIME_REFERENCE =
      Pattern.compile("(?i)(?:^|[^a-z0-9_])(query|q|variable|v|temp|t|context)\\s*\\.");

  private ConstantMolangEvaluator() {}

  static double evaluate(String expression) throws MolangEvaluationException {
    if (expression == null) throw new MolangEvaluationException(false, "Molang expression is null");
    String source = expression.trim();
    if (source.toLowerCase(Locale.ROOT).startsWith("return")) {
      if (source.length() == 6 || Character.isWhitespace(source.charAt(6))) {
        source = source.substring(6).trim();
      }
    }
    if (source.endsWith(";")) source = source.substring(0, source.length() - 1).trim();
    if (RUNTIME_REFERENCE.matcher(source).find()) {
      throw new MolangEvaluationException(true, "Molang expression depends on runtime state");
    }
    if (source.isEmpty()) throw new MolangEvaluationException(false, "Molang expression is empty");

    Parser parser = new Parser(source);
    double value = parser.parseExpression();
    parser.skipWhitespace();
    if (!parser.atEnd()) {
      throw new MolangEvaluationException(
          false, "Unsupported constant Molang syntax at index " + parser.index);
    }
    if (!Double.isFinite(value)) {
      throw new MolangEvaluationException(false, "Constant Molang result is not finite");
    }
    return value;
  }

  static final class MolangEvaluationException extends Exception {
    private static final long serialVersionUID = 1L;
    private final boolean dynamic;

    MolangEvaluationException(boolean dynamic, String message) {
      super(message);
      this.dynamic = dynamic;
    }

    boolean dynamic() {
      return dynamic;
    }
  }

  private static final class Parser {
    private final String source;
    private int index;

    private Parser(String source) {
      this.source = source;
    }

    private double parseExpression() throws MolangEvaluationException {
      double value = parseTerm();
      while (true) {
        skipWhitespace();
        if (consume('+')) value += parseTerm();
        else if (consume('-')) value -= parseTerm();
        else return value;
      }
    }

    private double parseTerm() throws MolangEvaluationException {
      double value = parseUnary();
      while (true) {
        skipWhitespace();
        if (consume('*')) value *= parseUnary();
        else if (consume('/')) value /= parseUnary();
        else if (consume('%')) value %= parseUnary();
        else return value;
      }
    }

    private double parseUnary() throws MolangEvaluationException {
      skipWhitespace();
      if (consume('+')) return parseUnary();
      if (consume('-')) return -parseUnary();
      return parsePrimary();
    }

    private double parsePrimary() throws MolangEvaluationException {
      skipWhitespace();
      if (consume('(')) {
        double value = parseExpression();
        skipWhitespace();
        if (!consume(')')) {
          throw new MolangEvaluationException(false, "Missing ')' in constant Molang expression");
        }
        return value;
      }
      return parseNumber();
    }

    private double parseNumber() throws MolangEvaluationException {
      skipWhitespace();
      int start = index;
      boolean digits = false;
      while (!atEnd() && Character.isDigit(source.charAt(index))) {
        index++;
        digits = true;
      }
      if (!atEnd() && source.charAt(index) == '.') {
        index++;
        while (!atEnd() && Character.isDigit(source.charAt(index))) {
          index++;
          digits = true;
        }
      }
      if (!digits) {
        throw new MolangEvaluationException(false, "Expected number at index " + start);
      }
      if (!atEnd() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
        int exponent = index++;
        if (!atEnd() && (source.charAt(index) == '+' || source.charAt(index) == '-')) index++;
        int exponentDigits = index;
        while (!atEnd() && Character.isDigit(source.charAt(index))) index++;
        if (exponentDigits == index) {
          index = exponent;
          throw new MolangEvaluationException(false, "Invalid exponent at index " + exponent);
        }
      }
      try {
        return Double.parseDouble(source.substring(start, index));
      } catch (NumberFormatException exception) {
        throw new MolangEvaluationException(false, "Invalid number at index " + start);
      }
    }

    private void skipWhitespace() {
      while (!atEnd() && Character.isWhitespace(source.charAt(index))) index++;
    }

    private boolean consume(char expected) {
      if (!atEnd() && source.charAt(index) == expected) {
        index++;
        return true;
      }
      return false;
    }

    private boolean atEnd() {
      return index >= source.length();
    }
  }
}
