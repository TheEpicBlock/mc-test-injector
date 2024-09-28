package nl.theepicblock.mctestinjector;

import nilloader.api.ClassTransformer;
import nilloader.api.ModRemapper;
import nilloader.api.NilLogger;
import nl.theepicblock.mctestinjector.support.AsmTransformerWrapper;
import nl.theepicblock.mctestinjector.support.mappings.LateMappingsDetector;

// All entrypoint classes must implement Runnable.
public class TestPremain implements Runnable {
	public static final String NAME = "mc-test-injector";
	public static NilLogger log = NilLogger.get(NAME);

	// Matches mappings.json
	public static final String INTERMEDIARY = "net.fabricmc.intermediary-1.20.1";
	public static final String HASHED = "org.quiltmc.hashed-1.20.1";
	public static final String MOJMAP = "com.mojang.launcher.server-0b4dba049482496c507b2387a73a913230ebbd76";
	public static final String SRGV2 = "de.oceanlabs.mcp.mcp_config-1.20.1";
	
	@Override
	public void run() {
		log.info("Initializing mc-test-injector");

		// prevents nilloader from trying to remap
		// (it can't because of java versions)
		ModRemapper.setTargetMapping("bogus");

		LateMappingsDetector detector = new LateMappingsDetector();
		ClassTransformer.register(new AsmTransformerWrapper(new TestInjectionTransformer(detector)));
	}
}
