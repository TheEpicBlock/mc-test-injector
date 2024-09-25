package nl.theepicblock.mctestinjector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import nilloader.NilAgent;
import nilloader.api.lib.asm.tree.ClassNode;
import nilloader.api.lib.asm.tree.MethodNode;
import nilloader.impl.lib.lorenz.MappingSet;

public class LateMappingsDetector {
    private Map<String, MappingSet> mappings;
    private String mapping = null;
    public final String nilmodid;
    
    @SuppressWarnings("unchecked")
    public LateMappingsDetector() {
        this.nilmodid = NilAgent.getActiveMod();
        this.mappings = NilHacks.getAllMappingsForMod(this.nilmodid);
    }

    public MappingSet detect(ClassNode clazz) {
        if (mapping == null) {
            TestPremain.log.info("Doing hacky stuff");
            List<String> methodNames = new ArrayList<>();
            for (MethodNode m : clazz.methods) {
                methodNames.add(m.name);
            }

            if (methodNames.contains("initServer")) {
				TestPremain.log.info("Late-detected mojmap as the runtime mapping");
                mapping = TestPremain.MOJMAP;
			} else if (anyMatch(methodNames, "method_\\d+")) {
				TestPremain.log.info("Late-detected intermediary as the runtime mapping");
                mapping = TestPremain.INTERMEDIARY;
			} else if (anyMatch(methodNames, "m_\\d+_")) {
				TestPremain.log.info("Late-detected srg v2 as the runtime mapping");
                mapping = TestPremain.SRGV2;
			}
        }
            
        return mappings.get(mapping);
    }

    public Iterable<MappingSet> getAllMappings() {
        return this.mappings.values();
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
