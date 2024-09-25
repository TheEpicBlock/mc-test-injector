package nl.theepicblock.mctestinjector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import nilloader.api.ASMTransformer;
import nilloader.api.lib.asm.Opcodes;
import nilloader.api.lib.asm.tree.ClassNode;
import nilloader.api.lib.asm.tree.MethodInsnNode;
import nilloader.api.lib.asm.tree.MethodNode;
import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.MethodMapping;

public class MiniMiniTransformer implements ASMTransformer {
    private final LateMappingsDetector mappingsProvider;
    private final String targetClass;
    private final Set<String> classesToCheck;
    private MappingSet mappings;

    public MiniMiniTransformer(LateMappingsDetector mappingsDetector) {
        this.mappingsProvider = mappingsDetector;
        this.classesToCheck = new HashSet<>();

        Patch.Class classAnn = getClass().getAnnotation(Patch.Class.class);
        this.targetClass = classAnn.value().replace('.', '/');
        mappingsProvider.getAllMappings().forEach(m -> {
            m.getClassMapping(this.targetClass).ifPresent(mappedClass -> {
                classesToCheck.add(mappedClass.getFullDeobfuscatedName());
            });
        });
    }

    @Override
    public boolean transform(ClassLoader loader, ClassNode clazz) {
        // Resolve mappings, note that this is done lazily
        // to ensure the mappings detector can inspect the class
        this.mappings = mappingsProvider.detect(clazz);
        Optional<? extends ClassMapping<?,?>> mapping =  this.mappings.getClassMapping(this.targetClass);

        if (!mapping.isPresent()) {
            TestPremain.log.warn("No mappings present for {}, no remapping will be done", this.targetClass);
        }

        boolean controlFlow = false;
        // Iterate through all our annotations
        for (final Method m : getClass().getMethods()) {
            boolean isOptional = m.getAnnotationsByType(Patch.Method.Optional.class).length > 0;
            controlFlow |= m.getAnnotationsByType(Patch.Method.AffectsControlFlow.class).length > 0;

            for (final Patch.Method a : m.getAnnotationsByType(Patch.Method.class)) {
                MethodSignature sig = MethodSignature.of(a.value());
                MethodSignature method = mapping
                        .flatMap(cm -> cm.getMethodMapping(sig))
                        .map(MethodMapping::getDeobfuscatedSignature)
                        .orElse(sig);
                TestPremain.log.info("Trying to inject into {}, which was remapped from {}", method.toJvmsIdentifier(), sig.toJvmsIdentifier());

                // Try to find the correct method
                boolean found = false;
                for (MethodNode methodNode : clazz.methods) {
                    if (method.equals(MethodSignature.of(methodNode.name, methodNode.desc))) {
                        // Found! Let's inject
                        found = true;
                        PatchContext ctx = NilHacks.createPatchCtx(methodNode, Optional.of(this.mappings));
                        try {
                            m.invoke(this, ctx);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        NilHacks.finishPatchCtx(ctx);
                        break;
                    }
                }

                if (!found && !isOptional) {
                    TestPremain.log.error(
                            "{} tried to inject into {}, but it couldn't be found!! The following methods exist:",
                            getClass().getSimpleName(),
                            method.toJvmsIdentifier());
                    for (MethodNode methodNode : clazz.methods) {
                        TestPremain.log.error("{}{}", methodNode.name, methodNode.desc);
                    }
                }
            }
        }

        return controlFlow;
    }

    @Override
    public boolean canTransform(ClassLoader loader, String className) {
        return classesToCheck.contains(className.replace('.', '/'));
    }
    
    protected final MethodInsnNode INVOKESTATIC(String owner, String name, String desc) {
        Optional<? extends ClassMapping<?,?>> mapping =  this.mappings.getClassMapping(owner);
        String remappedOwner = mapping.map(cm -> cm.getFullDeobfuscatedName()).orElse(owner);
        MethodSignature sig = MethodSignature.of(name, desc);
        MethodSignature remappedSig = mapping
                .flatMap(cm -> cm.getMethodMapping(sig))
                .map(mm -> mm.getDeobfuscatedSignature())
                .orElse(sig);
		return new MethodInsnNode(Opcodes.INVOKESTATIC, remappedOwner, remappedSig.getName(), remappedSig.getDescriptor().toString());
	}
}
