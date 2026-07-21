package io.github.gabriel0liv.cpmconverter.ir;
public record BoxUvIR(int u,int v) implements UvIR{public BoxUvIR{if(u<0||v<0)throw new IllegalArgumentException("UV");}}
