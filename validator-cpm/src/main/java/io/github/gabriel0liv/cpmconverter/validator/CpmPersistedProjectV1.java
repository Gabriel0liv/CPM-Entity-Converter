package io.github.gabriel0liv.cpmconverter.validator;

import java.util.*;

public record CpmPersistedProjectV1(int version, String skinType, CpmPersistedSize2i skinSize,
    List<CpmPersistedRootV1> roots, List<CpmPersistedElementV1> elements,
    Map<Long, CpmPersistedElementV1> generatedStoreIds, CpmPersistedTextureV1 texture) {
  public CpmPersistedProjectV1 { if(version != 1) throw new IllegalArgumentException("version"); skinType=skinType==null?"default":skinType; roots=List.copyOf(roots); elements=List.copyOf(elements); generatedStoreIds=Collections.unmodifiableMap(new LinkedHashMap<>(generatedStoreIds)); }
}
