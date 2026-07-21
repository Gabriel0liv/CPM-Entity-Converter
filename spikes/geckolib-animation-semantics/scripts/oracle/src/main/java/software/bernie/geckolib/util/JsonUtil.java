// NON_PRODUCTION: only the helper methods called by the real adapters.
package software.bernie.geckolib.util;
import com.google.gson.*; import java.util.*; import java.util.function.Function;
public final class JsonUtil {
    public static final Gson GEO_GSON = new GsonBuilder().create();
    public static <T> List<T> jsonArrayToList(JsonArray a, Function<JsonElement,T> f) { List<T> out=new ArrayList<>(); if(a!=null) for(JsonElement e:a) out.add(f.apply(e)); return out; }
    private JsonUtil() {}
}
