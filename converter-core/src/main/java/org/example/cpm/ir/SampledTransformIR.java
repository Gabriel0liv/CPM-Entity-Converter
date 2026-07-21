package org.example.cpm.ir;
import org.example.cpm.math.*;
public record SampledTransformIR(Vec3d translation,Quatd rotation,Vec3d scale){public SampledTransformIR{if(translation==null||rotation==null||scale==null)throw new IllegalArgumentException("sample");}}
