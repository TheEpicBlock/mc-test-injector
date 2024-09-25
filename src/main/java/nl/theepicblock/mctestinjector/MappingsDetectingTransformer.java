package nl.theepicblock.mctestinjector;

import java.lang.reflect.Field;
import java.util.Optional;

import nilloader.NilAgent;
import nilloader.api.lib.asm.tree.ClassNode;
import nilloader.api.lib.mini.MiniTransformer;

public abstract class MappingsDetectingTransformer extends MiniTransformer {
    private boolean patchedSelf = false; 
    private LateMappingsDetector detector;

    public MappingsDetectingTransformer(LateMappingsDetector detector) {
        this.detector = detector;
    }

	@Override
	public boolean modifyClassStructure(ClassNode clazz) {
        if (!patchedSelf) {
            // hackOwnMappings(detector.detect(clazz));
        }
        return super.modifyClassStructure(clazz);
    }

    /**
     * Sets our own mappings
     */
    private void hackOwnMappings(String newMappingName) {
        try {
            Class<?> clazz = MiniTransformer.class;
            Field mappingsField = clazz.getDeclaredField("mappings");
            mappingsField.setAccessible(true);
            mappingsField.set(this, Optional.of(NilAgent.getActiveMappings(detector.nilmodid)));
        } catch (Exception e) {
            TestPremain.log.warn("Failed to hack own mappings", e);;
        }
    }
}
