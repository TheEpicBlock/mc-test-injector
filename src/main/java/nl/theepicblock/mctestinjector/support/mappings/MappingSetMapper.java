package nl.theepicblock.mctestinjector.support.mappings;

import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.model.Mapping;
import nilloader.impl.lib.lorenz.model.MethodMapping;

import java.util.Optional;

public class MappingSetMapper implements Mapper {
    private final MappingSet set;

    public MappingSetMapper(MappingSet set) {
        this.set = set;
    }

    @Override
    public String mapClassname(String original) {
        return set.getClassMapping(original)
                .map(Mapping::getFullDeobfuscatedName)
                .orElse(original);
    }

    @Override
    public MethodSignature mapMethod(String clazz, MethodSignature sig) {
        return set.getClassMapping(clazz)
                .flatMap(cm -> cm.getMethodMapping(sig))
                .map(MethodMapping::getDeobfuscatedSignature)
                .orElse(sig);
    }

    @Override
    public Optional<MappingSet> asMappingSet() {
        return Optional.of(set);
    }
}
