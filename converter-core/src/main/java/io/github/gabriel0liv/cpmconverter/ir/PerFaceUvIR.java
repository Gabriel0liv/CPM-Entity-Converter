package io.github.gabriel0liv.cpmconverter.ir;
import java.util.*;
public record PerFaceUvIR(Map<String,FaceUvIR> faces) implements UvIR {public PerFaceUvIR{faces=Map.copyOf(faces==null?Map.of():faces);}}
