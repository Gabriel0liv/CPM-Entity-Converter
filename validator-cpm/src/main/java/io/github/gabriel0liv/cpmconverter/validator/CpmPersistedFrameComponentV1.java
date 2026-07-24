package io.github.gabriel0liv.cpmconverter.validator;
public record CpmPersistedFrameComponentV1(long storeId, CpmPersistedVec3 position,
    CpmPersistedVec3 rotation, String color, boolean show, CpmPersistedVec3 scale, int index, String pointer) {
  public CpmPersistedFrameComponentV1(long storeId, CpmPersistedVec3 value){this(storeId,value,value,"000000",true,new CpmPersistedVec3(1,1,1),0,null);}
}
