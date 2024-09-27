package nl.theepicblock.mctestinjector;

import nl.theepicblock.mctestinjector.support.LoggerReinitializer;
import nl.theepicblock.mctestinjector.tests.MixinEnvTest;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class TestRunner {
    public static void runTests() {
        // We now have a full Minecraft environment
        // look if we have a better logger
        LoggerReinitializer.reinit();

        TestPremain.log.info("Running tests");
        // Run mixin tests
        try {
            MixinEnvTest.doTest();
        } catch (NoClassDefFoundError e) {
            TestPremain.log.warn("Environment doesn't appear to have mixin installed. Couldn't run mixin tests");
        }

        TestPremain.log.info("Everything seems to be fine! Forcibly halting the jvm.");
    }
}
