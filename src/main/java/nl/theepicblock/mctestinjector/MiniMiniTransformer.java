package nl.theepicblock.mctestinjector;

import java.util.Objects;

import nilloader.api.ASMTransformer;
import nilloader.api.lib.asm.Opcodes;
import nilloader.api.lib.asm.commons.Method;
import nilloader.api.lib.asm.tree.ClassNode;
import nilloader.api.lib.asm.tree.MethodInsnNode;
import nilloader.api.lib.asm.tree.MethodNode;
import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.model.MethodMapping;

public class MiniMiniTransformer implements ASMTransformer {
    private final LateMappingsDetector mappingsProvider;
    private final String classToTransform;
    private final String methodToTransform;
    private final String methodDescToTransform;
    private final String methodToInject;
    private final String classOfMethodToInject;
    private final String descOfMethodToInject;

    public MiniMiniTransformer(LateMappingsDetector mappingsDetector, String classToTransform, String methodToTransform, String methodDescToTransform, String methodToInject, String classOfMethodToInject, String descOfMethodToInject) {
        this.mappingsProvider = mappingsDetector;
        this.classToTransform = classToTransform;
        this.methodToTransform = methodToTransform;
        this.methodToInject = methodToInject;
        this.methodDescToTransform = methodDescToTransform;
        this.classOfMethodToInject = classOfMethodToInject;
        this.descOfMethodToInject = descOfMethodToInject;
        if (!Objects.equals(descOfMethodToInject, "()V")) {
            TestPremain.log.error("MiniMiniTransformer only supports injecting void functions, not "+descOfMethodToInject);
        }
    }

    @Override
    public boolean transform(ClassLoader loader, ClassNode clazz) {
        MappingSet mapping = mappingsProvider.detect(clazz);
        MethodMapping method = mapping.getClassMapping(classToTransform).get().getOrCreateMethodMapping(methodToTransform, methodDescToTransform);
        for (MethodNode m : clazz.methods) {
            if (m.name.equals(method.getDeobfuscatedName()) && m.desc.equals(method.getDeobfuscatedDescriptor())) {
                m.instructions.insert(
                    new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        classOfMethodToInject,
                        methodToInject,
                        descOfMethodToInject
                    )
                );
            }
        }
        return false;
    }

    @Override
    public boolean canTransform(ClassLoader loader, String className) {
        return (classToTransform.equals(className));
    }
    
}
