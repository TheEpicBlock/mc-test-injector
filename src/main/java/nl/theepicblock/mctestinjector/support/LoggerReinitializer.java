package nl.theepicblock.mctestinjector.support;

import nilloader.api.NilLogger;
import nl.theepicblock.mctestinjector.TestPremain;

public class LoggerReinitializer {
    public static void reinit() {
        if (classExists("org.slf4j.Logger")) {
            TestPremain.log = new NilLogger(new Slf4jLogImpl(TestPremain.NAME));
        }
    }

    private static boolean classExists(String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
