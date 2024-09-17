package nl.theepicblock.mctestinjector;

import org.spongepowered.asm.mixin.MixinEnvironment;

import nilloader.api.lib.mini.MiniTransformer;
import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;

@Patch.Class("net.minecraft.Minecraft")
public class TestInjectionTransformer extends MiniTransformer {
	@Patch.Method("<clinit>()V")
	public void patchClinit(PatchContext ctx) {
		ctx.jumpToStart();
		
		ctx.add(
			INVOKESTATIC("nl/theepicblock/mctestinjector/TestInjectionTransformer$Hooks", "onClinit", "()V")
		);
	}
	
	public static class Hooks {
		public static void onClinit() {
			TestPremain.log.info("Running mixin audit");
			MixinEnvironment.getCurrentEnvironment().audit();
			TestPremain.log.info("Everything seems to be fine! Exiting minecraft");
			System.exit(0);
		}
	}
}
