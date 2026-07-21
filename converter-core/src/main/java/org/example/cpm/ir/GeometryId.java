package org.example.cpm.ir;
public record GeometryId(String value){public GeometryId{if(value==null||value.isBlank())throw new IllegalArgumentException("empty geometry id");}}
