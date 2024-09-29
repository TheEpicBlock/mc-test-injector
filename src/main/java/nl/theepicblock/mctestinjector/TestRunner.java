package nl.theepicblock.mctestinjector;

import nl.theepicblock.mctestinjector.tests.MixinEnvTest;

public class TestRunner {
    public static void runTests() {
        TestPremain.log.get().info("Running tests");
        // Run mixin tests
        try {
            MixinEnvTest.doTest();
        } catch (NoClassDefFoundError e) {
            TestPremain.log.get().warn("Environment doesn't appear to have mixin installed. Couldn't run mixin tests");
        }

        TestPremain.log.get().info("Everything seems to be fine! Forcibly halting the jvm.");
    }
}
