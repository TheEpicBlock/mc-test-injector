package nl.theepicblock.mctestinjector.support.mappings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import nilloader.NilAgent;
import nilloader.api.lib.asm.tree.ClassNode;
import nilloader.api.lib.asm.tree.MethodNode;
import nilloader.impl.lib.lorenz.MappingSet;
import nl.theepicblock.mctestinjector.TestPremain;
import nl.theepicblock.mctestinjector.support.ClassloadersSuck;
import nl.theepicblock.mctestinjector.support.NilHacks;
import nl.theepicblock.mctestinjector.support.Util;

public class LateMappingsDetector {
    private Map<String, MappingSet> mappingSets;
    private Mapper mapper = null;
    public final String nilmodid;
    
    @SuppressWarnings("unchecked")
    public LateMappingsDetector() {
        this.nilmodid = NilAgent.getActiveMod();
        this.mappingSets = NilHacks.getAllMappingsForMod(this.nilmodid);
    }

    public Mapper detect(ClassNode clazz) {
        if (mapper == null) {
            List<String> methodNames = new ArrayList<>();
            for (MethodNode m : clazz.methods) {
                methodNames.add(m.name);
            }

            if (methodNames.contains("initServer")) {
				TestPremain.log.info("Late-detected mojmap as the runtime mapping");
                mapper = fromSet(TestPremain.MOJMAP);
			} else if (false && anyMatch(methodNames, "method_\\d+")) {
				TestPremain.log.info("Late-detected intermediary as the runtime mapping");
                mapper = fromSet(TestPremain.INTERMEDIARY);
			} else if (anyMatch(methodNames, "m_[a-z]{8}")) {
                TestPremain.log.info("Late-detected hashed as the runtime mapping");
                mapper = fromSet(TestPremain.HASHED);
            } else if (anyMatch(methodNames, "m_\\d+_")) {
				TestPremain.log.info("Late-detected srg v2 as the runtime mapping");
                mapper = fromSet(TestPremain.SRGV2);
			} else {
                try {
                    Optional<ClassLoader> loader = ClassloadersSuck.findClassloaders()
                            .filter(cl -> Util.classExists("net.fabricmc.loader.api.FabricLoader", cl))
                            .findAny();
                    if (loader.isPresent()) {
                        ClassLoader mcClassloader = loader.get();
                        mapper = ClassloadersSuck.run(
                                mcClassloader,
                                ClassloadersSuck.get(LateMappingsDetector.class, "getFloaderMapper"),
                                fromSet(TestPremain.INTERMEDIARY));
                    }
                } catch (Throwable ignored) {}
            }
        }
        return mapper;
    }

    public static Mapper getFloaderMapper(Mapper left) {
        TestPremain.log.info("Remapping using fabric-loader api via intermediary");
        TestPremain.log.info("Late-detected {} as the runtime mapping", FloaderMapper.getRuntime());
        return new ChainMapper(left, new FloaderMapper("intermediary"));
    }

    private Mapper fromSet(String map) {
        return new MappingSetMapper(mappingSets.get(map));
    }

    public Iterable<MappingSet> getAllMappings() {
        return this.mappingSets.values();
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
