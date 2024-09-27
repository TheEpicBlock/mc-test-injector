package nl.theepicblock.mctestinjector;

import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;

@Patch.Class("net.minecraft.server.MinecraftServer")
public class TestInjectionTransformer extends MiniMiniTransformer {
	public TestInjectionTransformer(LateMappingsDetector detector) {
		super(detector);
	}

	@Patch.Method("loadLevel()V")
	public void patchServerStart(PatchContext ctx) {
		ctx.jumpToStart();

		ctx.add(
			INVOKESTATIC("nl/theepicblock/mctestinjector/TestInjectionTransformer$Hooks", "runTestsAndExit", "()V")
		);
	}
	
	public static class Hooks {
		public static void runTestsAndExit() {
			TestPremain.log.info("Running tests");
			if (true) throw new RuntimeException("e");
			TestRunner.runTests();
			TestPremain.log.info("Everything seems to be fine! Forcibly halting the jvm.");
			Runtime.getRuntime().halt(0);
		}
	}
}
