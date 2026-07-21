package org.example.cpm.ir;
import java.util.*;
public record SourceRotationChannelIR(List<SourceRotationKeyframe> keyframes,String rotationOrder){public SourceRotationChannelIR{keyframes=List.copyOf(keyframes==null?List.of():keyframes);rotationOrder=rotationOrder==null?"ZYX":rotationOrder;}}
