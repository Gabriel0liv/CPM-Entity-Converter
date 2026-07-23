package io.github.gabriel0liv.cpmconverter.validator;
import java.util.Map;
public record CpmPersistedPerFaceUvV1(Map<String,CpmPersistedFaceUvV1> faces) implements CpmPersistedUvV1 { public CpmPersistedPerFaceUvV1 { faces=Map.copyOf(faces); } }
