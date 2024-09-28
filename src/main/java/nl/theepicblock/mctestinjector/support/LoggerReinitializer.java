package nl.theepicblock.mctestinjector.support;

import nilloader.api.NilLogger;
import nl.theepicblock.mctestinjector.TestPremain;

import static nl.theepicblock.mctestinjector.support.Util.classExists;

public class LoggerReinitializer {
    public static void reinit() {
        if (classExists("org.slf4j.Logger")) {
            TestPremain.log = new NilLogger(new Slf4jLogImpl(TestPremain.NAME));
        }
    }
}
