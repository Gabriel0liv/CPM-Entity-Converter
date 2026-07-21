package org.example.cpm.ir;
import org.example.cpm.math.*;
public record CubeIR(CubeId id,BoneId bone,Vec3d origin,Vec3d size,Vec3d pivot,Quatd rotation){public CubeIR{if(id==null||bone==null||origin==null||size==null||pivot==null||rotation==null)throw new IllegalArgumentException("cube");}}
