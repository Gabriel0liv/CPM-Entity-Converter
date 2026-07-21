// NON_PRODUCTION: minimal API shim for the adapter's tuple usage only.
package com.mojang.datafixers.util;
public record Pair<F,S>(F first,S second){public F getFirst(){return first;}public S getSecond(){return second;}public static <F,S> Pair<F,S> of(F first,S second){return new Pair<>(first,second);}}
