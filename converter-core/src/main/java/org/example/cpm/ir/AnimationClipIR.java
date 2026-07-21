package org.example.cpm.ir;
import java.util.*;
public record AnimationClipIR(ClipId id,double duration,PlaybackMode playback,String customLoop,List<BoneTrackIR> tracks){public AnimationClipIR{if(id==null||!Double.isFinite(duration)||duration<=0||playback==null)throw new IllegalArgumentException("clip");tracks=List.copyOf(tracks==null?List.of():tracks);if(playback==PlaybackMode.CUSTOM&&(customLoop==null||customLoop.isBlank()))throw new IllegalArgumentException("custom loop");}}
