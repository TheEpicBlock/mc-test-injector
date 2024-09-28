package nl.theepicblock.mctestinjector.support;

import nilloader.api.NonLoadingClassWriter;
import nilloader.api.lib.asm.ClassReader;
import nilloader.api.lib.asm.ClassWriter;
import nilloader.api.lib.asm.Opcodes;
import nilloader.api.lib.asm.Type;
import nilloader.api.lib.asm.tree.*;

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

    public static <T> T run(ClassLoader cl, Method m, Object... ctx) {
        try {
            Class<?> toRunC = m.getDeclaringClass();
            ClassReader reader = new ClassReader(getClassBytes(toRunC));
            ClassNode n = new ClassNode();
            reader.accept(n, 0);
            MethodNode mNode = n.methods.stream()
                    .filter(mn -> mn.name.equals(m.getName()))
                    .findAny()
                    .orElseThrow(NullPointerException::new);
            mNode.access |= Opcodes.ACC_PUBLIC;
            mNode.access &= ~Opcodes.ACC_PRIVATE;
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new IllegalArgumentException();
            }
            ClassLoader ncl = new MiniClassloader(cl, ClassloadersSuck.class.getClassLoader(), mNode);
            Class<?> runner = ncl.loadClass("nl.theepicblock.HackClass");

            Object[] proxiedObjects = new Object[ctx.length];
            for (int i = 0; i < ctx.length; i++) {
                proxiedObjects[i] = proxy(ctx[i], ncl.loadClass(m.getParameterTypes()[i].getName()));
            }

            Object result = runner.getMethods()[0].invoke(null, proxiedObjects);

            return (T)proxy(result, m.getReturnType());
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T proxy(Object o, Class<T> targetInterface) {
        if (targetInterface.isAssignableFrom(o.getClass())) {
            // It's already fine
            return (T)o;
        }
        try {
            ClassLoader cl = new ProxyClassloader(targetInterface.getClassLoader(), targetInterface);
            Class<?> proxyClass = cl.loadClass("nl.theepicblock.Proxy");
            Constructor<?> c = proxyClass.getConstructor(Object.class);
            return (T)c.newInstance(o);
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException |
                 NoSuchMethodException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private static class MiniClassloader extends ClassLoader {
        private Class<?> hackClazz;
        private ClassLoader otherParent;
        private final MethodNode toCreate;

        protected MiniClassloader(ClassLoader parent, ClassLoader otherParent, MethodNode toCreate) {
            super(parent);
            this.otherParent = otherParent;
            this.toCreate = toCreate;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("nl.theepicblock.mctestinjector.support.Mapper")) {
                return otherParent.loadClass(name);
            }
            return super.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if ("nl.theepicblock.HackClass".equals(name)) {
                if (hackClazz == null) {
                    hackClazz = getHackClazz();
                }
                return hackClazz;
            }
            if (name.startsWith("nl.theepicblock.mctestinjector")) {
                return otherParent.loadClass(name);
            }
            return super.findClass(name);
        }

        private Class<?> getHackClazz() {
            ClassNode clazz = new ClassNode();
            clazz.version = Opcodes.V1_8;
            clazz.access = Opcodes.ACC_PUBLIC;
            clazz.name = "nl/theepicblock/HackClass";
            clazz.superName = "java/lang/Object";

            clazz.methods.add(toCreate);

            ClassWriter writer = new NonLoadingClassWriter(this, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            clazz.accept(writer);
            byte[] result = writer.toByteArray();
            return defineClass("nl.theepicblock.HackClass", result, 0, result.length);
        }
    }

    private static class ProxyClassloader extends ClassLoader {
        private Class<?> proxyClazz;
        private final Class<?> proxyTemplate;

        public ProxyClassloader(ClassLoader parent, Class<?> proxyTemplate) {
            super(parent);
            this.proxyTemplate = proxyTemplate;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if ("nl.theepicblock.Proxy".equals(name)) {
                if (proxyClazz == null) {
                    proxyClazz = getProxyClazz();
                }
                return proxyClazz;
            }
            return super.findClass(name);
        }

        private Class<?> getProxyClazz() {
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

            ClassWriter writer = new NonLoadingClassWriter(this, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            clazz.accept(writer);
            byte[] result = writer.toByteArray();
            return defineClass("nl.theepicblock.Proxy", result, 0, result.length);
        }
    }
}
