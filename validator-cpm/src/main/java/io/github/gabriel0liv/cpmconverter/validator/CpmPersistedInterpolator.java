package io.github.gabriel0liv.cpmconverter.validator;
public enum CpmPersistedInterpolator {
  POLY_LOOP("poly_loop"), POLY_SINGLE("poly_single"), LINEAR_LOOP("linear_loop"), LINEAR_SINGLE("linear_single"), NO_INTERPOLATE("no_interpolate"), TRIG_LOOP("trig_loop"), TRIG_SINGLE("trig_single");
  private final String persisted;
  CpmPersistedInterpolator(String persisted){this.persisted=persisted;}
  public String persistedValue(){return persisted;}
  public static CpmPersistedInterpolator fromPersistedValue(String value){for(var i:values()) if(i.persisted.equals(value)) return i; throw new IllegalArgumentException("unknown interpolator");}
}
