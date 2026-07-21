package io.github.gabriel0liv.cpmconverter.diagnostics;

/** Stable diagnostic identifiers used by the production contracts. */
public final class DiagnosticCodes {
  private DiagnosticCodes() {}

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
}
