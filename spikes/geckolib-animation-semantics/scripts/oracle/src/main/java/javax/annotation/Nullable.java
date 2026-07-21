// NON_PRODUCTION: annotation shim for the real GeckoLib container class.
package javax.annotation;
import java.lang.annotation.*;
@Target({ElementType.METHOD,ElementType.PARAMETER,ElementType.FIELD}) @Retention(RetentionPolicy.CLASS)
public @interface Nullable {}
