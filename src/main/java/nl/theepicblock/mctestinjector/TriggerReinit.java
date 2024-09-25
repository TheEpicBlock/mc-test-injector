package nl.theepicblock.mctestinjector;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.MixinEnvironment;

import nilloader.NilAgent;
import nilloader.api.lib.mini.MiniTransformer;
import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;

@Patch.Class("net.minecraft.server.MinecraftServer")
public class TriggerReinit extends MiniTransformer {
    @Patch.Method("<clinit>()V")
	public void patchServerStart(PatchContext ctx) {
        // We rerun nilloader's init phase. This is because
        // when nilloader first initialized, MinecraftServer was not on the classpath
        // yet, and it didn't know about mappings
        try {
            TestPremain.log.info("Reiniting nilloader with mappings");
            Class<?> clazz = NilAgent.class;
            Field f = clazz.getDeclaredField("instrumentation");
            f.setAccessible(true);
            NilAgent.agentmain("", (Instrumentation)f.get(null));
        } catch (Exception e) {
            TestPremain.log.error("Failed to reinit", e);
        }
	}
}
