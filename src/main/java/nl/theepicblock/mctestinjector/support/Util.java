package nl.theepicblock.mctestinjector.support;

public class Util {
    public static boolean classExists(String clazz, ClassLoader loader) {
        try {
            Class.forName(clazz, false, loader);
            return true;
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
