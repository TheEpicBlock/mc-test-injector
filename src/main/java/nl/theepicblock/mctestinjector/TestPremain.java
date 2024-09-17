package nl.theepicblock.mctestinjector;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

import nilloader.api.ClassTransformer;
import nilloader.api.ModRemapper;
import nilloader.api.NilLogger;

// All entrypoint classes must implement Runnable.
public class TestPremain implements Runnable {

	// NilLoader comes with a logger abstraction that Does The Right Thing depending on the environment.
	// You should always use it.
	public static final NilLogger log = NilLogger.get("mc-test-injector");
	
	@Override
	public void run() {
		try {
			Class<?> mcServer = Class.forName("net.minecraft.server.MinecraftServer");
			List<String> methodNames = List.of();
			for (Method m : mcServer.getMethods()) {
				methodNames.add(m.getName());
			}

			if (methodNames.contains("initServer")) {
				log.info("Detected mojmap as the runtime mapping");
				ModRemapper.setTargetMapping("com.mojang.launcher.server-0b4dba049482496c507b2387a73a913230ebbd76");
			} else if (anyMatch(methodNames, "method_\\d+")) {
				log.info("Detected intermediary as the runtime mapping");
				ModRemapper.setTargetMapping("net.fabricmc.intermediary-1.20.1");
			} else if (anyMatch(methodNames, "m_\\d+_")) {
				log.info("Detected srg v2 as the runtime mapping");
				ModRemapper.setTargetMapping("de.oceanlabs.mcp.mcp_config-1.20.1");
			}
		} catch (Exception ignored) {}

		ClassTransformer.register(new AsmTransformerWrapper(new TestInjectionTransformer()));
	}

	public static boolean anyMatch(List<String> names, String regex) {
		Pattern p = Pattern.compile(regex);
		for (String n : names) {
			if (p.matcher(n).matches()) {
				return true;
			}
		}
		return false;
	}
}
