package nl.theepicblock.mctestinjector;

import nl.theepicblock.mctestinjector.tests.MixinEnvTest;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class TestRunner {
    public static void runTests() {
        try {
            MixinEnvTest.doTest();
        } catch (NoClassDefFoundError e) {
            TestPremain.log.warn("Environment doesn't appear to have mixin installed. Couldn't run mixin tests");
        }
    }
}
