package nl.theepicblock.mctestinjector;

import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;
import nl.theepicblock.mctestinjector.support.mappings.LateMappingsDetector;
import nl.theepicblock.mctestinjector.support.MiniMiniTransformer;

import java.io.IOException;

@Patch.Class("net.minecraft.server.MinecraftServer")
public class TestInjectionTransformer extends MiniMiniTransformer {
	public TestInjectionTransformer(LateMappingsDetector detector) {
		super(detector);
	}

	@Patch.Method("loadLevel()V")
	public void patchServerStart(PatchContext ctx) throws IOException {
		ctx.jumpToStart();

		injectInvoke(ctx, Hooks.class, "runTestsAndExit");
	}
	
	public static class Hooks {
		public static void runTestsAndExit() {
			TestRunner.runTests();
			Runtime.getRuntime().halt(0);
		}
	}
}
