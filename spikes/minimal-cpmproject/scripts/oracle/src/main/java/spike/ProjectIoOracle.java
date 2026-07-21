// NON_PRODUCTION: disposable S003 harness; no upstream source is copied here.
package spike;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletionException;

import com.google.gson.JsonObject;
import com.tom.cpm.shared.editor.Editor;
import com.tom.cpm.shared.MinecraftCommonAccess;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.MinecraftObjectHolder;
import com.tom.cpm.shared.PlatformFeature;
import com.tom.cpl.config.ModConfigFile;
import com.tom.cpl.tag.AllTagManagers;
import com.tom.cpm.shared.editor.elements.ElementType;
import com.tom.cpm.shared.editor.elements.ModelElement;
import com.tom.cpm.shared.editor.project.ProjectFile;
import com.tom.cpm.shared.editor.project.ProjectIO;
import com.tom.cpm.shared.model.PlayerModelParts;
import com.tom.cpm.shared.model.SkinType;

public final class ProjectIoOracle {
    private static void initializeHeadlessAccess() {
        File config = new File(System.getProperty("java.io.tmpdir"), "cpm-s003-NON_PRODUCTION.json");
        com.tom.cpl.item.ItemStackHandler<Object> itemHandler = new com.tom.cpl.item.ItemStackHandler<>() {
            @Override public List<String> listTags(Object value) { return Collections.emptyList(); }
            @Override public List<String> listNativeTags() { return Collections.emptyList(); }
            @Override public List<com.tom.cpl.item.Stack> listNativeEntries(String tag) { return Collections.emptyList(); }
            @Override public List<com.tom.cpl.item.Stack> getAllElements() { return Collections.emptyList(); }
            @Override public com.tom.cpl.item.Stack emptyObject() { return wrap(null); }
            @Override public int getCount(Object value) { return 0; }
            @Override public int getMaxCount(Object value) { return 0; }
            @Override public int getDamage(Object value) { return 0; }
            @Override public int getMaxDamage(Object value) { return 0; }
            @Override public boolean itemEquals(Object a, Object b) { return a == b; }
            @Override public boolean itemEqualsFull(Object a, Object b) { return a == b; }
            @Override public com.tom.cpl.nbt.NBTTagCompound getTag(Object value) { return null; }
            @Override public boolean isInTag(String tag, Object value) { return false; }
            @Override public String getItemId(Object value) { return ""; }
            @Override public String getItemDisplayName(Object value) { return ""; }
        };
        com.tom.cpl.block.BlockStateHandler<Object> blockHandler = new com.tom.cpl.block.BlockStateHandler<>() {
            @Override public String getBlockId(Object value) { return ""; }
            @Override public List<String> getBlockStates(Object value) { return Collections.emptyList(); }
            @Override public String getPropertyValue(Object value, String property) { return ""; }
            @Override public int getPropertyValueInt(Object value, String property) { return 0; }
            @Override public List<String> getAllValuesFor(Object value, String property) { return Collections.emptyList(); }
            @Override public List<String> listNativeTags() { return Collections.emptyList(); }
            @Override public List<com.tom.cpl.block.BlockState> listNativeEntries(String tag) { return Collections.emptyList(); }
            @Override public List<com.tom.cpl.block.BlockState> getAllElements() { return Collections.emptyList(); }
            @Override public com.tom.cpl.block.BlockState emptyObject() { return wrap(null); }
            @Override public boolean equals(Object a, Object b) { return a == b; }
            @Override public boolean equalsFull(Object a, Object b) { return a == b; }
            @Override public com.tom.cpl.item.Stack getStackFromState(Object value) { return null; }
            @Override public boolean isInTag(String tag, Object value) { return false; }
            @Override public List<String> listTags(Object value) { return Collections.emptyList(); }
        };
        com.tom.cpl.block.entity.EntityTypeHandler<Object> entityHandler = new com.tom.cpl.block.entity.EntityTypeHandler<>() {
            @Override public boolean isInTag(String tag, Object value) { return false; }
            @Override public List<String> listTags(Object value) { return Collections.emptyList(); }
            @Override public boolean equals(Object a, Object b) { return a == b; }
            @Override public String getEntityId(Object value) { return ""; }
            @Override public List<String> listAllActiveEffectTypes() { return Collections.emptyList(); }
            @Override public List<String> listNativeTags() { return Collections.emptyList(); }
            @Override public List<com.tom.cpl.block.entity.EntityType> listNativeEntries(String tag) { return Collections.emptyList(); }
            @Override public List<com.tom.cpl.block.entity.EntityType> getAllElements() { return Collections.emptyList(); }
            @Override public com.tom.cpl.block.entity.EntityType emptyObject() { return wrap(null); }
        };
        com.tom.cpl.block.BiomeHandler<Object> biomeHandler = new com.tom.cpl.block.BiomeHandler<>() {
            @Override public boolean isInTag(String tag, Object value) { return false; }
            @Override public List<String> listTags(Object value) { return Collections.emptyList(); }
            @Override public boolean equals(Object a, Object b) { return a == b; }
            @Override public String getBiomeId(Object value) { return ""; }
            @Override public float getTemperature(Object value) { return 0; }
            @Override public float getHumidity(Object value) { return 0; }
            @Override public RainType getRainType(Object value) { return RainType.NONE; }
            @Override public boolean isAvailable() { return false; }
            @Override public List<String> listNativeTags() { return Collections.emptyList(); }
            @Override public List<com.tom.cpl.block.Biome> listNativeEntries(String tag) { return Collections.emptyList(); }
            @Override public List<com.tom.cpl.block.Biome> getAllElements() { return Collections.emptyList(); }
            @Override public com.tom.cpl.block.Biome emptyObject() { return wrap(null); }
        };
        MinecraftObjectHolder.setCommonObject(new MinecraftCommonAccess() {
            private final ModConfigFile configFile = new ModConfigFile(config);

            @Override public ModConfigFile getConfig() { return configFile; }
            @Override public com.tom.cpl.util.ILogger getLogger() { return null; }
            @Override public EnumSet<PlatformFeature> getSupportedFeatures() { return EnumSet.noneOf(PlatformFeature.class); }
            @Override public com.tom.cpl.text.TextRemapper<?> getTextRemapper() { return null; }
            @Override public com.tom.cpm.api.CPMApiManager getApi() { return null; }
            @Override public String getMCVersion() { return "1.20.1"; }
            @Override public String getMCBrand() { return "S003-NON_PRODUCTION"; }
            @Override public String getModVersion() { return "0.6.27"; }
            @Override public com.tom.cpl.item.ItemStackHandler<?> getItemStackHandler() { return itemHandler; }
            @Override public com.tom.cpl.block.BlockStateHandler<?> getBlockStateHandler() { return blockHandler; }
            @Override public com.tom.cpl.block.entity.EntityTypeHandler<?> getEntityTypeHandler() { return entityHandler; }
        });
        try {
            AllTagManagers[] tags = new AllTagManagers[1];
            MinecraftClientAccess client = (MinecraftClientAccess) Proxy.newProxyInstance(
                    ProjectIoOracle.class.getClassLoader(),
                    new Class<?>[] { MinecraftClientAccess.class },
                    (proxy, method, args) -> {
                        if (method.getName().equals("getBuiltinTags")) return tags[0];
                        if (method.getName().equals("getBiomeHandler")) return biomeHandler;
                        if (method.getName().equals("getSkinType")) return SkinType.DEFAULT;
                        if (method.getName().equals("getImageIO")) return new com.tom.cpl.util.AWTImageIO();
                        if (method.getName().equals("executeOnGameThread")) { ((Runnable) args[0]).run(); return null; }
                        Class<?> type = method.getReturnType();
                        if (type == boolean.class) return false;
                        if (type == int.class) return 0;
                        return null;
                    });
            Field field = MinecraftObjectHolder.class.getDeclaredField("clientObject");
            field.setAccessible(true);
            field.set(null, client);
            tags[0] = new AllTagManagers();
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to initialize NON_PRODUCTION client stub", error);
        }
    }

    private static Editor initializedEditor() {
        Editor editor = new Editor();
        editor.ui = (com.tom.cpl.gui.UI) Proxy.newProxyInstance(
                ProjectIoOracle.class.getClassLoader(),
                new Class<?>[] { com.tom.cpl.gui.UI.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("i18nFormat")) return String.valueOf(args[0]);
                    if (method.getName().equals("executeLater")) { ((Runnable) args[0]).run(); return null; }
                    return null;
                });
        editor.skinType = SkinType.DEFAULT;
        for (PlayerModelParts part : PlayerModelParts.VALUES) {
            if (part != PlayerModelParts.CUSTOM_PART) {
                editor.elements.add(new ModelElement(editor, ElementType.ROOT_PART, part));
            }
        }
        return editor;
    }

    public static void main(String[] args) {
        initializeHeadlessAccess();
        for (String arg : args) {
            JsonObject result = new JsonObject();
            result.addProperty("marker", "NON_PRODUCTION");
            result.addProperty("file", new File(arg).getName());
            try {
                ProjectFile project = new ProjectFile();
                project.load(new File(arg)).join();
                Editor editor = initializedEditor();
                ProjectIO.loadProject(editor, project);
                result.addProperty("projectIo", "PASS");
                result.addProperty("rootCount", editor.elements.size());
                result.addProperty("animationCount", editor.animations.size());
            } catch (Throwable error) {
                while (error instanceof CompletionException && error.getCause() != null) {
                    error = error.getCause();
                }
                result.addProperty("projectIo", "FAIL");
                result.addProperty("errorType", error.getClass().getName());
                result.addProperty("message", String.valueOf(error.getMessage()));
            }
            System.out.println(result);
        }
    }
}
