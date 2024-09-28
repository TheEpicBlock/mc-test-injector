package nl.theepicblock.mctestinjector.support.mappings;

import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nilloader.impl.lib.lorenz.MappingSet;

import java.util.Optional;

public class ChainMapper implements Mapper {
    private final Mapper left;
    private final Mapper right;

    public ChainMapper(Mapper left, Mapper right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String mapClassname(String original) {
        return right.mapClassname(left.mapClassname(original));
    }

    @Override
    public MethodSignature mapMethod(String clazz, MethodSignature sig) {
        return right.mapMethod(left.mapClassname(clazz), left.mapMethod(clazz, sig));
    }

    @Override
    public Optional<MappingSet> asMappingSet() {
        return Optional.empty();
    }
}
