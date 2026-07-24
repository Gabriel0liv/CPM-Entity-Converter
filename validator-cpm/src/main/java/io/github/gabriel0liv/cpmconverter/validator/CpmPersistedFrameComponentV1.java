package io.github.gabriel0liv.cpmconverter.validator;
public record CpmPersistedFrameComponentV1(long storeId, CpmPersistedVec3 position,
    CpmPersistedVec3 rotation, String color, boolean show, CpmPersistedVec3 scale, int index, String pointer) {
}
