package nl.theepicblock.mctestinjector.support;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import nilloader.api.ASMTransformer;
import nilloader.api.NilLoader;
import nilloader.api.lib.asm.Opcodes;
import nilloader.api.lib.asm.tree.*;
import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nl.theepicblock.mctestinjector.TestPremain;
import nl.theepicblock.mctestinjector.support.mappings.LateMappingsDetector;
import nl.theepicblock.mctestinjector.support.mappings.Mapper;

public class MiniMiniTransformer implements ASMTransformer {
    private final LateMappingsDetector mappingsProvider;
    private final String targetClass;
    private final Set<String> classesToCheck;
    private Mapper mappings;

    private ClassNode current;

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
        current = clazz;
        // Resolve mappings, note that this is done lazily
        // to ensure the mappings detector can inspect the class
        this.mappings = mappingsProvider.detect(clazz);
        if (this.mappings == null) {
            throw new RuntimeException("No valid mappings were detected. Can't inject");
        }

        String clazzName =  this.mappings.mapClassname(this.targetClass);

        boolean controlFlow = false;
        // Iterate through all our annotations
        for (final Method m : getClass().getMethods()) {
            boolean isOptional = m.getAnnotationsByType(Patch.Method.Optional.class).length > 0;
            controlFlow |= m.getAnnotationsByType(Patch.Method.AffectsControlFlow.class).length > 0;

            for (final Patch.Method a : m.getAnnotationsByType(Patch.Method.class)) {
                MethodSignature sig = MethodSignature.of(a.value());
                MethodSignature method = mappings.mapMethod(this.targetClass, sig);
                TestPremain.log.get().debug("Trying to inject into {}, which was remapped from {}", method.toJvmsIdentifier(), sig.toJvmsIdentifier());

                // Try to find the correct method
                boolean found = false;
                for (MethodNode methodNode : clazz.methods) {
                    if (method.equals(MethodSignature.of(methodNode.name, methodNode.desc))) {
                        // Found! Let's inject
                        found = true;
                        PatchContext ctx = NilHacks.createPatchCtx(methodNode, this.mappings.asMappingSet());
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
                    TestPremain.log.get().error(
                            "{} tried to inject into {}, but it couldn't be found!! The following methods exist:",
                            getClass().getSimpleName(),
                            method.toJvmsIdentifier());
                    for (MethodNode methodNode : clazz.methods) {
                        TestPremain.log.get().error("{}{}", methodNode.name, methodNode.desc);
                    }
                }
            }
        }

        current = null;
        return controlFlow;
    }

    @Override
    public boolean canTransform(ClassLoader loader, String className) {
        return classesToCheck.contains(className.replace('.', '/'));
    }

    /**
     * Inserts the equivalent to: {@code ctx.add(INVOKESTATIC(owner, name, desc)},
     * if you were using {@link nilloader.api.lib.mini.MiniTransformer}.
     * <p>
     * This method does have a few notable differences. It will inject a call that does not care
     * about classloader boundaries. This fixes issues on e.g. NeoForge, but it also means that this
     * <b>should not be used to invoke methods that aren't part of the nilmod</b>.
     */
    protected final void injectInvoke(PatchContext ctx, Class<?> owner, String name) throws IOException {
        // We spawn a thread.
        // This threads stores our desired classloader as its context. That's its only function.
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {}
            }
        });
        t.setContextClassLoader(this.getClass().getClassLoader());
        int id = new Random().nextInt();
        t.setName("mc-test-injector, stupid hacky thread "+id);
        t.start();

        MethodNode injectorMethod = ClassloadersSuck.getDefinition(ClassloadersSuck.get(MiniMiniTransformer.class, "injectInvokeInner"));
        injectorMethod.name = "invokerProxy"+id;
        current.methods.add(injectorMethod);

        ctx.add(
                new LdcInsnNode(current.name.replace("/", ".")),
                new LdcInsnNode(owner.getName()),
                new LdcInsnNode(name),
                new LdcInsnNode(id)
        );
        ctx.add(
            new MethodInsnNode(Opcodes.INVOKESTATIC, current.name, injectorMethod.name, injectorMethod.desc)
        );
    }

    /**
     * Never invoked directly. Instead, this method's bytecode is copied over
     * into the target class.
     */
    private static void injectInvokeInner(String selfName, String clazzName, String name, int id) {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        Thread found = null;
        for (Thread t : threads) {
            if (t.getName().equals("mc-test-injector, stupid hacky thread "+id)) {
                found = t;
            }
        }
        try {
            ClassLoader loader = found.getContextClassLoader();
            Class<?> clazz = loader.loadClass("nl.theepicblock.mctestinjector.support.MiniMiniTransformer");
            Method f = null;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals("bootstrapAndRun")) {
                    f = m;
                }
            }
            ClassLoader selfLoader = Class.forName(selfName).getClassLoader();
            f.invoke(null, selfLoader, clazzName, name);
            found.stop();
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Never invoked directly, invoked by injectInvokeInner
     */
    public static void bootstrapAndRun(ClassLoader l, String clazzName, String name) {
        ClassLoader cl = new ClassloadersSuck.MultiParentClassLoader(new ClassLoader[] {
                MiniMiniTransformer.class.getClassLoader(),
                NilLoader.class.getClassLoader(),
                l
        });
        try {
            Class<?> clazz = cl.loadClass(clazzName);
            Method m = ClassloadersSuck.get(clazz, name);
            m.invoke(null);
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
