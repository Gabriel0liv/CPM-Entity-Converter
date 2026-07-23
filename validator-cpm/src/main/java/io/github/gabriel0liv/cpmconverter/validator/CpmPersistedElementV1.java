package io.github.gabriel0liv.cpmconverter.validator;

import java.util.List;
public record CpmPersistedElementV1(String name, Long storeId, List<CpmPersistedElementV1> children, int preOrderIndex, int depth, String pointer) { public CpmPersistedElementV1 { if(name==null) throw new IllegalArgumentException("name"); children=List.copyOf(children); } }
