package org.example.cpm.ir;
import java.util.*;
public record ChannelIR<T>(String component,TransformMode mode,TransformSpace space,List<KeyframeIR<T>> keyframes){public ChannelIR{if(component==null||mode==null||space==null)throw new IllegalArgumentException("channel");keyframes=List.copyOf(keyframes==null?List.of():keyframes);}}
