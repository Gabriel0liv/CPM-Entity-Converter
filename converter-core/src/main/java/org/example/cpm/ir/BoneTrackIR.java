package org.example.cpm.ir;
import java.util.*;
public record BoneTrackIR(BoneId bone,List<ChannelIR<?>> channels){public BoneTrackIR{if(bone==null)throw new IllegalArgumentException("track");channels=List.copyOf(channels==null?List.of():channels);}}
