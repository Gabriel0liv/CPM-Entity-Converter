// NON_PRODUCTION: logger shim; parser/animation classes remain pinned GeckoLib classes.
package software.bernie.geckolib;
public final class GeckoLib {
    public static final Logger LOGGER = new Logger();
    public static final class Logger { public void error(String ignored) {} }
    private GeckoLib() {}
}
