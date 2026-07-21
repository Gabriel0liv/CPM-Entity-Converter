package io.github.gabriel0liv.cpmconverter.math;
public record EulerAnglesZYX(double x,double y,double z){ public Quatd toQuaternion(){return Quatd.fromEulerZYX(x,y,z);} public static EulerAnglesZYX fromDegrees(double x,double y,double z){return new EulerAnglesZYX(Math.toRadians(x),Math.toRadians(y),Math.toRadians(z));} }
