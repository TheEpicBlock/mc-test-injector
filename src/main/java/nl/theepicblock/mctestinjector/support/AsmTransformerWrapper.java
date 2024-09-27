package nl.theepicblock.mctestinjector.support;

import nilloader.api.ASMTransformer;
import nilloader.api.ClassTransformer;
import nilloader.api.lib.asm.ClassReader;
import nilloader.api.lib.asm.ClassWriter;
import nilloader.api.lib.asm.Opcodes;
import nilloader.api.lib.asm.tree.ClassNode;
import nilloader.api.NonLoadingClassWriter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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

		int originalVersion = (originalData[6] << 8) | originalData[7];

		// Make this class file appear like an earlier version
		originalData[6] = (byte)(maxAsmVersion() >> 8);
		originalData[7] = (byte)(maxAsmVersion() & 0xFF);
        
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
		originalData[6] = (byte)(originalVersion >> 8);
		originalData[7] = (byte)(originalVersion & 0xFF);
        return result;
    }
	
	@Override @Deprecated
	public byte[] transform(String className, byte[] originalData) {
		return transform(ClassLoader.getSystemClassLoader(), className, originalData);
	}

	private static int maxAsmVersion = 0;

	private static int maxAsmVersion() {
		// Automatically detect which version asm supports
		if (maxAsmVersion == 0) {
			try {
				for (Field f : Opcodes.class.getFields()) {
					if (f.getName().startsWith("V") &&
							f.getType() == int.class &&
							Modifier.isStatic(f.getModifiers()) &&
							Modifier.isPublic(f.getModifiers())) {
						int value = (int)f.get(null);
						value &= 0xFFFF; // We only care about the major version
						if (value > maxAsmVersion) maxAsmVersion = value;
					}
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		return maxAsmVersion;
	}
}
