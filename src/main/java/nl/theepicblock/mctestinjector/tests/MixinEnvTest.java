package nl.theepicblock.mctestinjector.tests;

import nl.theepicblock.mctestinjector.TestPremain;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class MixinEnvTest {
    public static void doTest() {
        TestPremain.log.info("Running mixin audit");
        MixinEnvironment.getCurrentEnvironment().audit();
    }
}
