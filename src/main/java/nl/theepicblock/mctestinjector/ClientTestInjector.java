package nl.theepicblock.mctestinjector;

import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;
import nl.theepicblock.mctestinjector.support.MiniMiniTransformer;
import nl.theepicblock.mctestinjector.support.mappings.LateMappingsDetector;

import java.io.IOException;

@Patch.Class("net.minecraft.client.Minecraft")
public class ClientTestInjector extends MiniMiniTransformer {
	public ClientTestInjector(LateMappingsDetector detector) {
		super(detector);
	}

	@Patch.Method("setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
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
