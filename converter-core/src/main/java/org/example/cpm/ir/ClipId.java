package org.example.cpm.ir;
public record ClipId(String value){public ClipId{if(value==null||value.isBlank())throw new IllegalArgumentException("empty clip id");}}
