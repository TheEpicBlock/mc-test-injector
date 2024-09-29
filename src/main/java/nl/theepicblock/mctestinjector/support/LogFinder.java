package nl.theepicblock.mctestinjector.support;

import nilloader.api.NilLogger;
import nilloader.impl.log.NilLogImpl;
import nilloader.impl.log.Slf4jLogImpl;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import static nl.theepicblock.mctestinjector.support.Util.classDefined;
import static nl.theepicblock.mctestinjector.support.Util.classExists;

public class LogFinder {
    private final String name;
    private NilLogger internal;
    private boolean isChecking = false;
    private boolean gaveUp = false;

    public LogFinder(String name) {
        this.name = name;
    }

    public NilLogger get() {
        if (internal == null) {
            internal = NilLogger.get(name);
            return internal;
        }
        if (Objects.equals(internal.getImplementationName(), "System.out") && !isChecking && !gaveUp) {
            isChecking = true; // Prevent stack overflows is we have to log something whilst we're checking
            // Try to find a better logger
            Optional<ClassLoader> slf4j = ClassloadersSuck.findClassloaders()
                    .filter(cl -> classExists("org.slf4j.Logger", cl))
                    .findAny();
            slf4j.ifPresent(cl -> {
                try {
                    // We want to work in the environment that has slf4j, so let's create a classloader there
                    ClassloadersSuck.InjectableClassloader ncl = new ClassloadersSuck.InjectableClassloader(cl);
                    // Copy over the classes we need
                    ncl.injectClass(NilLogImpl.class);
                    ncl.injectClass(Slf4jLogImpl.class);
                    // Run our `getSlf4jLogger` function
                    Method m = ClassloadersSuck.createCopyOfMethod(ncl, ClassloadersSuck.get(LogFinder.class, "getSlf4jLogger"));
                    NilLogImpl result = ClassloadersSuck.proxy(m.invoke(null, this.name), NilLogImpl.class);
                    this.internal = new NilLogger(result);
                } catch (Throwable t) {
                    this.internal.debug("Couldn't get a better logger", t);
                    gaveUp = true;
                }
            });
            if (!slf4j.isPresent()) {
                this.internal.info("Couldn't find slf4j");
            }
            isChecking = false;
        }

        return this.internal;
    }

    public static Object getSlf4jLogger(String name) {
        return new Slf4jLogImpl(name);
    }
}
