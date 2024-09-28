package nl.theepicblock.mctestinjector.support.mappings;

import net.fabricmc.loader.api.FabricLoader;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nilloader.impl.lib.lorenz.MappingSet;

import java.util.Optional;

public class FloaderMapper implements Mapper {
    private final String mappingSetName;

    public FloaderMapper(String mappingSetName) {
        this.mappingSetName = mappingSetName;
    }

    @Override
    public String mapClassname(String original) {
        return FabricLoader
                .getInstance()
                .getMappingResolver()
                .mapClassName(mappingSetName, original);
    }

    @Override
    public MethodSignature mapMethod(String clazz, MethodSignature sig) {
        String name = FabricLoader
                .getInstance()
                .getMappingResolver()
                .mapMethodName(mappingSetName, clazz, sig.getName(), sig.getDescriptor().toString());
        return MethodSignature.of(name, sig.getDescriptor().toString());
    }

    @Override
    public Optional<MappingSet> asMappingSet() {
        return Optional.empty();
    }

    public static String getRuntime() {
        return FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
    }
}
