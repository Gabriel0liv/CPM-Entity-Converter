package io.github.gabriel0liv.cpmconverter.diagnostics;

import java.util.Objects;
import java.util.function.Function;

public final class Result<T> {
  private final T value;
  private final DiagnosticBag diagnostics;

  private Result(T value, DiagnosticBag diagnostics) {
    if ((value == null) != diagnostics.hasErrors()) {
      throw new IllegalArgumentException(
          "Result must be success(value, no errors) or failure(null, error)");
    }
    this.value = value;
    this.diagnostics = diagnostics;
  }

  public static <T> Result<T> success(T value) {
    return success(value, new DiagnosticBag());
  }

  public static <T> Result<T> success(T value, DiagnosticBag diagnostics) {
    return new Result<>(Objects.requireNonNull(value), diagnostics);
  }

  public static <T> Result<T> failure(Diagnostic diagnostic) {
    return failure(new DiagnosticBag().add(diagnostic));
  }

  public static <T> Result<T> failure(DiagnosticBag diagnostics) {
    if (!diagnostics.hasErrors()) throw new IllegalArgumentException("failure requires ERROR");
    return new Result<>(null, diagnostics);
  }

  public T value() {
    return value;
  }

  public DiagnosticBag diagnostics() {
    return diagnostics;
  }

  public boolean success() {
    return value != null && !diagnostics.hasErrors();
  }

  public <U> Result<U> map(Function<? super T, ? extends U> function) {
    if (!success()) return Result.failure(diagnostics);
    return Result.success(function.apply(value), diagnostics);
  }

  public <U> Result<U> flatMap(Function<? super T, Result<U>> function) {
    if (!success()) return Result.failure(diagnostics);
    Result<U> next = Objects.requireNonNull(function.apply(value));
    DiagnosticBag merged = diagnostics.addAll(next.diagnostics());
    return next.success() ? Result.success(next.value(), merged) : Result.failure(merged);
  }
}
