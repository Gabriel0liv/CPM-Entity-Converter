// NON_PRODUCTION: minimal Minecraft API shim for compiling the real GeckoLib adapter.
package net.minecraft.util;
import com.google.gson.*;
public final class GsonHelper {
    public static JsonObject getAsJsonObject(JsonObject o, String k, JsonObject d) { JsonElement e=o.get(k); return e!=null&&e.isJsonObject()?e.getAsJsonObject():d; }
    public static JsonObject getAsJsonObject(JsonObject o, String k) { return o.getAsJsonObject(k); }
    public static JsonArray getAsJsonArray(JsonObject o, String k) { return o.getAsJsonArray(k); }
    public static String getAsString(JsonObject o, String k, String d) { JsonElement e=o.get(k); return e==null?d:e.getAsString(); }
    public static String getAsString(JsonObject o, String k) { return o.get(k).getAsString(); }
    public static double getAsDouble(JsonObject o, String k) { return o.get(k).getAsDouble(); }
    public static JsonArray getAsJsonArray(JsonObject o, String k, JsonArray d) { JsonElement e=o.get(k); return e!=null&&e.isJsonArray()?e.getAsJsonArray():d; }
    private GsonHelper() {}
}
