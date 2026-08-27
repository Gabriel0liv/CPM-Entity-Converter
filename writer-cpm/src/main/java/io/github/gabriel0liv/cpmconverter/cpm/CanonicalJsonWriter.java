package io.github.gabriel0liv.cpmconverter.cpm;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Minimal deterministic JSON encoder for writer-owned project structures. */
final class CanonicalJsonWriter {
  private CanonicalJsonWriter() {}

  static byte[] write(Object value) {
    StringBuilder builder = new StringBuilder();
    appendValue(builder, value);
    builder.append('\n');
    return builder.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void appendValue(StringBuilder builder, Object value) {
    if (value == null) {
      builder.append("null");
    } else if (value instanceof String string) {
      appendString(builder, string);
    } else if (value instanceof Boolean bool) {
      builder.append(bool.booleanValue());
    } else if (value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long) {
      builder.append(value);
    } else if (value instanceof Float number) {
      appendFloating(builder, number.doubleValue());
    } else if (value instanceof Double number) {
      appendFloating(builder, number.doubleValue());
    } else if (value instanceof Map<?, ?> map) {
      appendMap(builder, map);
    } else if (value instanceof Iterable<?> iterable) {
      appendIterable(builder, iterable);
    } else {
      throw new IllegalArgumentException("unsupported JSON value " + value.getClass().getName());
    }
  }

  private static void appendFloating(StringBuilder builder, double value) {
    if (!Double.isFinite(value)) throw new IllegalArgumentException("non-finite JSON number");
    builder.append(Double.toString(value));
  }

  private static void appendMap(StringBuilder builder, Map<?, ?> map) {
    List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
    for (Map.Entry<?, ?> entry : entries) {
      if (!(entry.getKey() instanceof String)) {
        throw new IllegalArgumentException("JSON object keys must be strings");
      }
    }
    entries.sort(Comparator.comparing(entry -> (String) entry.getKey()));

    builder.append('{');
    boolean first = true;
    for (Map.Entry<?, ?> entry : entries) {
      if (!first) builder.append(',');
      first = false;
      appendString(builder, (String) entry.getKey());
      builder.append(':');
      appendValue(builder, entry.getValue());
    }
    builder.append('}');
  }

  private static void appendIterable(StringBuilder builder, Iterable<?> values) {
    builder.append('[');
    boolean first = true;
    for (Object value : values) {
      if (!first) builder.append(',');
      first = false;
      appendValue(builder, value);
    }
    builder.append(']');
  }

  private static void appendString(StringBuilder builder, String value) {
    builder.append('"');
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '"' -> builder.append("\\\"");
        case '\\' -> builder.append("\\\\");
        case '\b' -> builder.append("\\b");
        case '\f' -> builder.append("\\f");
        case '\n' -> builder.append("\\n");
        case '\r' -> builder.append("\\r");
        case '\t' -> builder.append("\\t");
        default -> {
          if (character < 0x20) {
            builder.append(String.format("\\u%04x", (int) character));
          } else {
            builder.append(character);
          }
        }
      }
    }
    builder.append('"');
  }
}
