package nl.theepicblock.mctestinjector.support;

import nilloader.api.NonLoadingClassWriter;
import nilloader.api.lib.asm.ClassReader;
import nilloader.api.lib.asm.ClassWriter;
import nilloader.api.lib.asm.Opcodes;
import nilloader.api.lib.asm.Type;
import nilloader.api.lib.asm.tree.*;
import nl.theepicblock.mctestinjector.TestPremain;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

public class ClassloadersSuck {
    public static Stream<ClassLoader> findClassloaders() {
        return Thread.getAllStackTraces()
                .keySet()
                .stream()
                .map(Thread::getContextClassLoader);
    }

    public static Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream getClassBytes(Class<?> clazz) {
        return clazz.getClassLoader().getResourceAsStream(clazz.getName().replace(".", "/")+".class");
    }

    public static Method get(Class<?> definer, String name) {
        return Arrays.stream(definer.getDeclaredMethods()).filter(m -> m.getName().equals(name)).findAny().orElseThrow(NullPointerException::new);
    }

    /**
     * Runs a method in the context of a certain classloader.
     * @param cl The classloader to run the method in
     * @param m The method to run
     * @param ctx parameters for the method
     * @return
     */
    public static <T> T run(ClassLoader cl, Method m, Object... ctx) {
        try {
            // Find the class containing our method
            Class<?> toRunC = m.getDeclaringClass();
            ClassReader reader = new ClassReader(getClassBytes(toRunC));
            ClassNode n = new ClassNode();
            reader.accept(n, 0);

            // Get the bytecode of the method we want to run
            MethodNode mNode = n.methods.stream()
                    .filter(mn -> mn.name.equals(m.getName()))
                    .findAny()
                    .orElseThrow(NullPointerException::new);
            mNode.access |= Opcodes.ACC_PUBLIC;
            mNode.access &= ~Opcodes.ACC_PRIVATE;
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new IllegalArgumentException();
            }

            // Create a new class containing a copy of that method
            ClassNode runnerDefinition = new ClassNode();
            runnerDefinition.version = Opcodes.V1_8;
            runnerDefinition.access = Opcodes.ACC_PUBLIC;
            runnerDefinition.name = "nl/theepicblock/HackClass";
            runnerDefinition.superName = "java/lang/Object";

            runnerDefinition.methods.add(mNode);

            // Create a classloader as a child of the loader we want to inject in to
            // and load our runner class in there
            InjectableClassloader ncl = new InjectableClassloader(cl);
            Class<?> runner = ncl.injectClass(runnerDefinition);

            // Proxy objects if needed, so that the types are compatible in our target loader
            Object[] proxiedObjects = new Object[ctx.length];
            for (int i = 0; i < ctx.length; i++) {
                proxiedObjects[i] = proxy(ctx[i], ncl.loadClass(m.getParameterTypes()[i].getName()));
            }

            // Run the method
            Object result = runner.getMethods()[0].invoke(null, proxiedObjects);

            // Return the result, proxied if needed so it's compatible with our current loader
            return (T)proxy(result, m.getReturnType());
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException |
                 IOException e) {
            TestPremain.log.warn("a",e.getCause());
            throw new RuntimeException(e);
        }
    }

    public static <T> T proxy(Object o, Class<T> targetInterface) {
        if (targetInterface.isAssignableFrom(o.getClass())) {
            // It's already fine
            return (T)o;
        }
        try {
            ClassLoader targetClassloader = targetInterface.getClassLoader();
            InjectableClassloader ncl;
            if (targetClassloader instanceof InjectableClassloader) {
                ncl = (InjectableClassloader)targetClassloader;
            } else {
                ncl = new InjectableClassloader(targetClassloader);
            }

            Class<?> proxyClass = ncl.injectClass(createProxyClass(targetInterface));
            Constructor<?> c = proxyClass.getConstructor(Object.class);
            return (T)c.newInstance(o);
        } catch (InvocationTargetException | IllegalAccessException |
                 NoSuchMethodException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassNode createProxyClass(Class<?> proxyTemplate) {
        ClassNode clazz = new ClassNode();
        clazz.version = Opcodes.V1_8;
        clazz.access = Opcodes.ACC_PUBLIC;
        clazz.name = "nl/theepicblock/Proxy";
        clazz.superName = "java/lang/Object";
        clazz.interfaces.add(proxyTemplate.getName().replace(".", "/"));
        clazz.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "inner", "Ljava/lang/Object;", null, null));

        MethodNode initializer = new MethodNode();
        initializer.name = "<init>";
        initializer.desc = "(Ljava/lang/Object;)V";
        initializer.access = Opcodes.ACC_PUBLIC;
        initializer.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        initializer.instructions.add(new InsnNode(Opcodes.DUP));
        initializer.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));
        initializer.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        initializer.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, "nl/theepicblock/Proxy", "inner", "Ljava/lang/Object;"));
        initializer.instructions.add(new InsnNode(Opcodes.RETURN));
        clazz.methods.add(initializer);

        for (Method m : proxyTemplate.getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            MethodNode mn = new MethodNode();
            mn.name = m.getName();
            mn.desc = Type.getMethodDescriptor(m);
            mn.access = m.getModifiers();
            mn.access &= ~Opcodes.ACC_ABSTRACT;
            mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            mn.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "nl/theepicblock/Proxy", "inner", "Ljava/lang/Object;"));
            mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;"));
            mn.instructions.add(new LdcInsnNode(m.getName()));
            mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ClassloadersSuck.class.getName().replace(".", "/"), "get", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Method;"));
            mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            mn.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "nl/theepicblock/Proxy", "inner", "Ljava/lang/Object;"));
            mn.instructions.add(new LdcInsnNode(m.getParameterCount()));
            mn.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
            for (int i = 0; i < m.getParameterCount(); i++) {
                mn.instructions.add(new InsnNode(Opcodes.DUP));
                mn.instructions.add(new LdcInsnNode(i));
                mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, i+1));
                mn.instructions.add(new InsnNode(Opcodes.AASTORE));
            }
            mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method","invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"));
            mn.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, m.getReturnType().getName().replace(".", "/")));
            mn.instructions.add(new InsnNode(Opcodes.ARETURN));
            clazz.methods.add(mn);
        }
        return clazz;
    }

    private static class InjectableClassloader extends ClassLoader {
        protected InjectableClassloader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> injectClass(ClassNode clazz) {
            ClassWriter writer = new NonLoadingClassWriter(this, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            clazz.accept(writer);
            return injectClass(clazz.name.replace("/", "."), writer.toByteArray());
        }

        public Class<?> injectClass(String name, byte[] bytecode) {
            return this.defineClass(name, bytecode, 0, bytecode.length);
        }
    }
}
