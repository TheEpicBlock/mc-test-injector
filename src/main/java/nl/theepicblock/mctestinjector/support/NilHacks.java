package nl.theepicblock.mctestinjector.support;

import nilloader.NilAgent;
import nilloader.api.lib.asm.tree.MethodNode;
import nilloader.api.lib.mini.PatchContext;
import nilloader.impl.lib.lorenz.MappingSet;
import nl.theepicblock.mctestinjector.TestPremain;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Any and all hacks into nilloader's internals
 */
public class NilHacks {
    public static PatchContext createPatchCtx(MethodNode method, Optional<MappingSet> mappings) {
        try {
            Class<PatchContext> clazz = PatchContext.class;
            Constructor<PatchContext> constructor = clazz.getDeclaredConstructor(MethodNode.class, Optional.class);
            constructor.setAccessible(true);
            return constructor.newInstance(method, mappings);
        } catch (Exception e) {
            TestPremain.log.error("Failed to reflectively create a patch ctx", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Call the finish method on patch ctx
     */
    public static void finishPatchCtx(PatchContext ctx) {
        try {
            Class<PatchContext> clazz = PatchContext.class;
            Method m = clazz.getDeclaredMethod("finish");
            m.setAccessible(true);
            m.invoke(ctx);
        } catch (Exception e) {
            TestPremain.log.error("Failed to reflectively invoke PatchContext#finish", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Nilloader will usually throw away all mappings that were not selected.
     * If you call this method before your preinit ends, you can save them
     */
    public static Map<String, MappingSet> getAllMappingsForMod(String nilmodid) {
        try {
            Class<?> clazz = NilAgent.class;
            Field mappingsField = clazz.getDeclaredField("modMappings");
            mappingsField.setAccessible(true);
            Map<String,Map<String, MappingSet>> allMappings = (Map<String, Map<String, MappingSet>>)mappingsField.get(null);

            return allMappings.get(nilmodid);
        } catch (Exception e) {
            TestPremain.log.warn("Failed to save mappings from being cleared", e);
            throw new RuntimeException(e);
        }
    }
}
