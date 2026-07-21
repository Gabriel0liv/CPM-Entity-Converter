package io.github.gabriel0liv.cpmconverter.ir;
import java.util.*;
public record SourceRotationChannelIR(List<SourceRotationKeyframeIR> keyframes,RotationOrder rotationOrder){public SourceRotationChannelIR{keyframes=List.copyOf(keyframes==null?List.of():keyframes);rotationOrder=rotationOrder==null?RotationOrder.ZYX:rotationOrder;}}
