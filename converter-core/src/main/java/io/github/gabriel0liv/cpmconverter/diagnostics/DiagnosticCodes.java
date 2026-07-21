package io.github.gabriel0liv.cpmconverter.diagnostics;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** Stable diagnostic identifiers used by production contracts. */
public final class DiagnosticCodes {
  private DiagnosticCodes() {}

  private static final Set<String> ALL = discover();

  /** Returns the immutable, canonical catalog used by architecture checks and reports. */
  public static Set<String> all() {
    return ALL;
  }

  private static Set<String> discover() {
    TreeSet<String> values = new TreeSet<>();
    for (Field field : DiagnosticCodes.class.getDeclaredFields()) {
      if (field.getType() != String.class) {
        continue;
      }
      try {
        values.add((String) field.get(null));
      } catch (IllegalAccessException exception) {
        throw new ExceptionInInitializerError(exception);
      }
    }
    return Collections.unmodifiableSet(values);
  }

  public static final String CONFIG_SCHEMA_VERSION = "CONFIG_SCHEMA_VERSION";
  public static final String CONFIG_SAMPLING_RANGE = "CONFIG_SAMPLING_RANGE";
  public static final String CONFIG_NON_FINITE = "CONFIG_NON_FINITE";
  public static final String CONFIG_OVERROTATION = "CONFIG_OVERROTATION";
  public static final String CONFIG_INFLUENCE_RANGE = "CONFIG_INFLUENCE_RANGE";
  public static final String CONFIG_UNKNOWN_PROPERTY = "CONFIG_UNKNOWN_PROPERTY";
  public static final String CONFIG_PARSE_ERROR = "CONFIG_PARSE_ERROR";
  public static final String CONFIG_SCHEMA_INVALID = "CONFIG_SCHEMA_INVALID";
  public static final String CONFIG_BONE_MISSING = "CONFIG_BONE_MISSING";
  public static final String CONFIG_BONE_AMBIGUOUS = "CONFIG_BONE_AMBIGUOUS";
  public static final String CONFIG_CLIP_MISSING = "CONFIG_CLIP_MISSING";
  public static final String ANIM_OPTIONAL_CLIP_MISSING = "ANIM_OPTIONAL_CLIP_MISSING";
  public static final String IR_DUPLICATE_BONE_ID = "IR_DUPLICATE_BONE_ID";
  public static final String IR_DUPLICATE_CUBE_ID = "IR_DUPLICATE_CUBE_ID";
  public static final String IR_DUPLICATE_CLIP_ID = "IR_DUPLICATE_CLIP_ID";
  public static final String IR_CYCLE = "IR_CYCLE";
  public static final String IR_TIMESTAMP_INVALID = "IR_TIMESTAMP_INVALID";
  public static final String IR_ROOT_MISSING = "IR_ROOT_MISSING";
  public static final String IR_ROOT_PARENT = "IR_ROOT_PARENT";
  public static final String IR_ROOT_DUPLICATE = "IR_ROOT_DUPLICATE";
  public static final String IR_PARENT_MISSING = "IR_PARENT_MISSING";
  public static final String IR_CHILD_MISSING = "IR_CHILD_MISSING";
  public static final String IR_CHILD_DUPLICATE = "IR_CHILD_DUPLICATE";
  public static final String IR_PARENT_CHILD_MISMATCH = "IR_PARENT_CHILD_MISMATCH";
  public static final String IR_UNREACHABLE_BONE = "IR_UNREACHABLE_BONE";
  public static final String IR_CUBE_BONE_MISSING = "IR_CUBE_BONE_MISSING";
  public static final String IR_TRACK_BONE_MISSING = "IR_TRACK_BONE_MISSING";
  public static final String IR_DURATION_INVALID = "IR_DURATION_INVALID";
  public static final String IR_KEYFRAME_ORDER = "IR_KEYFRAME_ORDER";
  public static final String IR_KEYFRAME_DUPLICATE = "IR_KEYFRAME_DUPLICATE";
  public static final String IR_KEYFRAME_AFTER_DURATION = "IR_KEYFRAME_AFTER_DURATION";
  public static final String IR_CUSTOM_PLAYBACK_ID = "IR_CUSTOM_PLAYBACK_ID";
  public static final String IR_INVALID_ID = "IR_INVALID_ID";
  public static final String IR_INVALID_VALUE = "IR_INVALID_VALUE";
  public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
  public static final String INPUT_UNSUPPORTED_VERSION = "INPUT_UNSUPPORTED_VERSION";
  public static final String INPUT_LIMIT_EXCEEDED = "INPUT_LIMIT_EXCEEDED";
  public static final String GEO_MULTIPLE_MODELS = "GEO_MULTIPLE_MODELS";
  public static final String GEO_PARENT_NOT_FOUND = "GEO_PARENT_NOT_FOUND";
  public static final String GEO_HIERARCHY_CYCLE = "GEO_HIERARCHY_CYCLE";
  public static final String GEO_DUPLICATE_BONE_NAME = "GEO_DUPLICATE_BONE_NAME";
  public static final String GEO_MESH_UNSUPPORTED = "GEO_MESH_UNSUPPORTED";
  public static final String GEO_CUBE_HELPER_SYNTHESIZED = "GEO_CUBE_HELPER_SYNTHESIZED";
  public static final String UV_OUT_OF_BOUNDS = "UV_OUT_OF_BOUNDS";
  public static final String PNG_INVALID = "PNG_INVALID";
  public static final String ANIM_CLIP_NOT_FOUND = "ANIM_CLIP_NOT_FOUND";
  public static final String ANIM_BONE_NOT_FOUND = "ANIM_BONE_NOT_FOUND";
  public static final String ANIM_DYNAMIC_MOLANG_UNSUPPORTED = "ANIM_DYNAMIC_MOLANG_UNSUPPORTED";
  public static final String ANIM_CUSTOM_EASING_UNSUPPORTED = "ANIM_CUSTOM_EASING_UNSUPPORTED";
  public static final String ANIM_LERP_MODE_IGNORED_449 = "ANIM_LERP_MODE_IGNORED_449";
  public static final String ANIM_PRE_POST_COLLAPSED_449 = "ANIM_PRE_POST_COLLAPSED_449";
  public static final String ANIM_IMPLICIT_LENGTH_UNBOUNDED = "ANIM_IMPLICIT_LENGTH_UNBOUNDED";
  public static final String ANIM_ZERO_DURATION_INVALID = "ANIM_ZERO_DURATION_INVALID";
  public static final String ANIM_DUPLICATE_TIMESTAMP = "ANIM_DUPLICATE_TIMESTAMP";
  public static final String ANIM_CUSTOM_LOOP_TYPE_UNSUPPORTED =
      "ANIM_CUSTOM_LOOP_TYPE_UNSUPPORTED";
  public static final String ANIM_EULER_DECOMPOSITION_AMBIGUOUS =
      "ANIM_EULER_DECOMPOSITION_AMBIGUOUS";
  public static final String ANIM_EVENT_IGNORED_BY_SCOPE = "ANIM_EVENT_IGNORED_BY_SCOPE";
  public static final String ANIM_HOLD_REQUIRES_MAPPING = "ANIM_HOLD_REQUIRES_MAPPING";
  public static final String ANIM_LOOP_DISCONTINUITY = "ANIM_LOOP_DISCONTINUITY";
  public static final String ANIM_RESAMPLED = "ANIM_RESAMPLED";
  public static final String ANIM_FRAME_GRID_DENSITY_DIFFERENCE =
      "ANIM_FRAME_GRID_DENSITY_DIFFERENCE";
  public static final String ANIM_APPROXIMATION = "ANIM_APPROXIMATION";
  public static final String ANIM_ZERO_SCALE_UNREPRESENTABLE = "ANIM_ZERO_SCALE_UNREPRESENTABLE";
  public static final String MAP_SCHEMA_INVALID = "MAP_SCHEMA_INVALID";
  public static final String MAP_BONE_NOT_FOUND = "MAP_BONE_NOT_FOUND";
  public static final String MAP_CLIP_NOT_FOUND = "MAP_CLIP_NOT_FOUND";
  public static final String MAP_LOOK_OVERROTATION = "MAP_LOOK_OVERROTATION";
  public static final String CPM_DUPLICATE_STORE_ID = "CPM_DUPLICATE_STORE_ID";
  public static final String CPM_DANGLING_ANIMATION_REF = "CPM_DANGLING_ANIMATION_REF";
  public static final String CPM_INVALID_ROOT = "CPM_INVALID_ROOT";
  public static final String CPM_VALIDATION_FAILED = "CPM_VALIDATION_FAILED";
  public static final String FEATURE_EXPLICITLY_IGNORED = "FEATURE_EXPLICITLY_IGNORED";
  public static final String QUADRUPED_LIMITATION = "QUADRUPED_LIMITATION";
  public static final String IO_OUTPUT_EXISTS = "IO_OUTPUT_EXISTS";
}
