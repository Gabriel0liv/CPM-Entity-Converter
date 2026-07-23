package io.github.gabriel0liv.cpmconverter.validator;

import java.util.List;
public record CpmPersistedRootV1(String id, List<CpmPersistedElementV1> children) { public CpmPersistedRootV1 { if(id==null||id.isBlank()) throw new IllegalArgumentException("id"); children=List.copyOf(children); } }
