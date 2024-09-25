package nl.theepicblock.mctestinjector;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

        try {
            Class<?> clazz = NilAgent.class;
            Field mappingsField = clazz.getDeclaredField("modMappings");
            mappingsField.setAccessible(true);
            Map<String, Map<String, MappingSet>> allMappings = (Map<String, Map<String, MappingSet>>)mappingsField.get(null);

            this.mappings = allMappings.get(this.nilmodid);
        } catch (Exception e) {
            TestPremain.log.warn("Failed to save mappings from being cleared", e);
        }
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
            if (mapping != null) {
                hackSetMappings(mapping);
            }
        }
            
        return mappings.get(mapping);
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
    
    /**
     * This won't do anything for the bulk of Nil's remapping, but it 
     * helps for {@link MappingsDetectingTransformer}s
     */
    @SuppressWarnings("unchecked")
    private void hackSetMappings(String newMappingName) {
        this.mapping = newMappingName;
        // String current = NilAgent.getActiveMappingId(nilmodid);
        // if (Objects.equals(current, newMappingName)) {
        //     TestPremain.log.debug("Already this mapping, hack init not needed");
        // }

        // try {
        //     Class<?> clazz = NilAgent.class;
        //     Field mappingsIdField = clazz.getDeclaredField("activeModMappings");
        //     mappingsIdField.setAccessible(true);
        //     Field mappingsField = clazz.getDeclaredField("modMappings");
        //     mappingsField.setAccessible(true);

        //     Map<String, String> mappingsIds = (Map<String, String>)mappingsIdField.get(null);
        //     Map<String, Map<String, MappingSet>> allMappings = (Map<String, Map<String, MappingSet>>)mappingsField.get(null);
        //     allMappings.put(nilmodid, this.mappings);
        //     mappingsIds.put(nilmodid, newMappingName);
        // } catch (Exception e) {
        //     TestPremain.log.warn("Failed to hack in new mappings", e);;
        // }
    }
}
