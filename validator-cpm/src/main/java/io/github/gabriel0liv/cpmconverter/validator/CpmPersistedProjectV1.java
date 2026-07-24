package io.github.gabriel0liv.cpmconverter.validator;

import java.util.*;

public record CpmPersistedProjectV1(int version, String skinType, CpmPersistedSize2i skinSize,
    List<CpmPersistedRootV1> roots, List<CpmPersistedElementV1> elements,
    Map<Long, CpmPersistedElementV1> generatedStoreIds, Map<Long,CpmPersistedTargetV1> effectiveTargets, CpmPersistedTextureV1 texture) {
  public CpmPersistedProjectV1 { if(version != 1) throw new IllegalArgumentException("version"); skinType=skinType==null?"default":skinType; roots=List.copyOf(roots); elements=List.copyOf(elements); generatedStoreIds=Collections.unmodifiableMap(new LinkedHashMap<>(generatedStoreIds)); effectiveTargets=Collections.unmodifiableMap(new LinkedHashMap<>(effectiveTargets)); }
  public Map<Long, CpmPersistedTargetV1> persistedTargets() {
    var result = new LinkedHashMap<Long, CpmPersistedTargetV1>();
    effectiveTargets.forEach((id, target) -> { if (id > 6) result.put(id, target); });
    return Collections.unmodifiableMap(result);
  }
}
