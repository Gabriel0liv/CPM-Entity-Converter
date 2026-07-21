package io.github.gabriel0liv.cpmconverter.ir;
public record FeatureOccurrence(String feature,String source){public FeatureOccurrence{if(feature==null||feature.isBlank())throw new IllegalArgumentException("feature");}}
