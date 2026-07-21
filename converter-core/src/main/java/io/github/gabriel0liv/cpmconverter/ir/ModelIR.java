package io.github.gabriel0liv.cpmconverter.ir;
import java.util.*;
public record ModelIR(SourceDescriptor source,List<BoneIR> bones,List<BoneId> roots,List<AnimationClipIR> clips,List<TextureIR> textures){public ModelIR{if(source==null)throw new IllegalArgumentException("source");bones=List.copyOf(bones==null?List.of():bones);roots=List.copyOf(roots==null?List.of():roots);clips=List.copyOf(clips==null?List.of():clips);textures=List.copyOf(textures==null?List.of():textures);}}
