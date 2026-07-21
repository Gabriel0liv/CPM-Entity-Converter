package org.example.cpm.ir;
public record BoneId(String value){public BoneId{if(value==null||value.isBlank())throw new IllegalArgumentException("empty bone id");}}
