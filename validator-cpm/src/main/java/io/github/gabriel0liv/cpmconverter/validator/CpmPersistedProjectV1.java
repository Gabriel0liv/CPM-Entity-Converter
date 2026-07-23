package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;

public record CpmPersistedProjectV1(JsonNode value) { public CpmPersistedProjectV1 { value = value == null ? null : value.deepCopy(); if (value == null) throw new IllegalArgumentException("value"); } @Override public JsonNode value() { return value.deepCopy(); } }
