package org.example.cpm.diagnostics;
public record DiagnosticCode(String value) { public DiagnosticCode { if (value == null || value.isBlank()) throw new IllegalArgumentException("diagnostic code is empty"); } @Override public String toString(){return value;} }
