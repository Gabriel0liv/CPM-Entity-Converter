package io.github.gabriel0liv.cpmconverter.ir;
import java.util.*; import io.github.gabriel0liv.cpmconverter.math.*;
public record BoneIR(BoneId id,String name,BoneId parent,List<BoneId> children,Transform bind,List<CubeIR> cubes){public BoneIR{if(id==null||name==null||bind==null)throw new IllegalArgumentException("bone");children=List.copyOf(children==null?List.of():children);cubes=List.copyOf(cubes==null?List.of():cubes);}}
