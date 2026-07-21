package org.example.cpm.ir;
public record TextureIR(String path,int width,int height){public TextureIR{if(path==null||path.isBlank()||width<=0||height<=0)throw new IllegalArgumentException("texture");}}
