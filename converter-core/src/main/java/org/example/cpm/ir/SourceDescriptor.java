package org.example.cpm.ir;
public record SourceDescriptor(String path,String format){public SourceDescriptor{if(path==null||path.isBlank())throw new IllegalArgumentException("source path");path=path.replace('\\','/');}}
