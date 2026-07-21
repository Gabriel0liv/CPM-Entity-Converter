package io.github.gabriel0liv.cpmconverter.ir;
public record FaceUvIR(int u,int v,int width,int height){public FaceUvIR{if(u<0||v<0||width<0||height<0)throw new IllegalArgumentException("face UV");}}
