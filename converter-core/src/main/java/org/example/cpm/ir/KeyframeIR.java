package org.example.cpm.ir;
public record KeyframeIR<T>(double time,T incomingValue,T outgoingValue,InterpolationIR interpolation){public KeyframeIR{if(!Double.isFinite(time)||time<0||interpolation==null)throw new IllegalArgumentException("keyframe");}}
