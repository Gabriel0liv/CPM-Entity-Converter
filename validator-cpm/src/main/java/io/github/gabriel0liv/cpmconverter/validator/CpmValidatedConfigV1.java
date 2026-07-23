package io.github.gabriel0liv.cpmconverter.validator;
import com.fasterxml.jackson.databind.JsonNode;
record CpmValidatedConfigV1(JsonNode source,int version,String skinType,CpmPersistedSize2i skinSize,boolean customGridSize,boolean textureAnimationPresent) {}
