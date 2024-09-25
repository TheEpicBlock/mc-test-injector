package nl.theepicblock.mctestinjector.tests;

import org.spongepowered.asm.mixin.MixinEnvironment;

public class MixinEnvTest {
    public static void doTest() {
        MixinEnvironment.getCurrentEnvironment().audit();
    }
}
