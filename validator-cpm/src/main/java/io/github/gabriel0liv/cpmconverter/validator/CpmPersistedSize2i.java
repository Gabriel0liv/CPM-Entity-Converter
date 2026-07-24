package io.github.gabriel0liv.cpmconverter.validator;
public record CpmPersistedSize2i(int x,int y) { public CpmPersistedSize2i { if(x<=0||y<=0) throw new IllegalArgumentException("positive size"); } }
