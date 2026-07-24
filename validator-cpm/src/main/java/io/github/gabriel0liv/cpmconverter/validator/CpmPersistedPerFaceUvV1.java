package io.github.gabriel0liv.cpmconverter.validator;
import java.util.*;
public record CpmPersistedPerFaceUvV1(Map<CpmPersistedFace,CpmPersistedFaceUvV1> faces) implements CpmPersistedUvV1 {
  public CpmPersistedPerFaceUvV1 { var copy=new LinkedHashMap<CpmPersistedFace,CpmPersistedFaceUvV1>(); for(var f:CpmPersistedFace.values()) if(faces.containsKey(f)) copy.put(f, Objects.requireNonNull(faces.get(f))); faces=Collections.unmodifiableMap(copy); }
}
