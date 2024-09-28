package nl.theepicblock.mctestinjector.support.mappings;

import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nilloader.impl.lib.lorenz.MappingSet;

import java.util.Optional;

public interface Mapper {
    String mapClassname(String original);

    MethodSignature mapMethod(String clazz, MethodSignature sig);

    Optional<MappingSet> asMappingSet();
}
