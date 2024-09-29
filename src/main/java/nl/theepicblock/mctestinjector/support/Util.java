package nl.theepicblock.mctestinjector.support;

import java.lang.reflect.Method;

public class Util {
    public static boolean classExists(String clazz, ClassLoader loader) {
        try {
            Class.forName(clazz, false, loader);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean classDefined(String clazz, ClassLoader loader) {
        try {
            // Use findLoadedClass to prevent triggering any actual lookups
            Method find = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            return (boolean)find.invoke(loader, clazz);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean classExists(String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
