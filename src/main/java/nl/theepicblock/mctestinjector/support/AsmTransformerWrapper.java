package nl.theepicblock.mctestinjector.support;

import nilloader.api.ASMTransformer;
import nilloader.api.ClassTransformer;
import nilloader.api.lib.asm.ClassReader;
import nilloader.api.lib.asm.ClassWriter;
import nilloader.api.lib.asm.tree.ClassNode;
import nilloader.api.NonLoadingClassWriter;

/**
 * Asm starts crashing as soon as the classfile is even slightly out of data. So this
 * fakes it by temporarily setting the version to be an earlier one.
 */
public class AsmTransformerWrapper implements ClassTransformer {
    private final ASMTransformer inner;

    public AsmTransformerWrapper(ASMTransformer inner) {
        this.inner = inner;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, byte[] originalData) {
		if (!inner.canTransform(loader, className)) return originalData;

        // Trick ASM into thinking this file is an earlier version
        byte originalVersion = originalData[7];
        originalData[7] = 61;
        
        // Regular asm reading code
		ClassReader reader = new ClassReader(originalData);
		ClassNode clazz = new ClassNode();
		reader.accept(clazz, 0);
		
		boolean frames = inner.transform(loader, clazz);
		
		int flags = ClassWriter.COMPUTE_MAXS;
		if (frames) {
			flags |= ClassWriter.COMPUTE_FRAMES;
		}
		ClassWriter writer = new NonLoadingClassWriter(loader, flags);
		clazz.accept(writer);
		byte[] result = writer.toByteArray();

        // Undo our hack
        result[7] = originalVersion;
        return result;
    }
	
	@Override @Deprecated
	public byte[] transform(String className, byte[] originalData) {
		return transform(ClassLoader.getSystemClassLoader(), className, originalData);
	}
}
