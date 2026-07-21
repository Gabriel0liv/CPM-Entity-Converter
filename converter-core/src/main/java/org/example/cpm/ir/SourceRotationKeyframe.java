package org.example.cpm.ir;
import org.example.cpm.math.Vec3d;
public record SourceRotationKeyframe(double time,Vec3d value,InterpolationIR interpolation){public SourceRotationKeyframe{if(!Double.isFinite(time)||time<0||value==null)throw new IllegalArgumentException("rotation keyframe");}}
