package su.erik.tabledataloader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import su.erik.tabledataloader.importer.txt.TxtImporterDescriptor;
import su.erik.tabledataloader.mocks.MockMapParamProvider;
import su.erik.tabledataloader.spi.*;

import java.util.ServiceLoader;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceProviderTest {

    private static Stream<Arguments> providerDefinitions() {
        return Stream.of(
                Arguments.of(MapParamProvider.class, MockMapParamProvider.class),
                Arguments.of(LoaderDescriptor.class, TxtImporterDescriptor.class),
                Arguments.of(LoaderDescriptor.class, CsvLoaderDescriptor.class),
                Arguments.of(LoaderDescriptor.class, ZipLoaderDescriptor.class),
                Arguments.of(LoaderDescriptor.class, CsvExporterDescriptor.class)
        );
    }

    @ParameterizedTest
    @MethodSource("providerDefinitions")
    void checkProviderLoaded(Class<?> serviceInterface, Class<?> implClass) {
        checkProviderTypeSafe(serviceInterface, implClass);
    }

    @SuppressWarnings("unchecked")
    private <T> void checkProviderTypeSafe(Class<?> serviceInterface, Class<?> implClass) {
        Class<T> typedService = (Class<T>) serviceInterface;
        Class<? extends T> typedImpl = (Class<? extends T>) implClass;
        ServiceLoader<T> loader = ServiceLoader.load(typedService);

        boolean isProviderFound = loader.stream()
                .map(ServiceLoader.Provider::type)
                .anyMatch(loadedClass -> loadedClass.equals(typedImpl));

        assertTrue(isProviderFound, () -> String.format(
                "Implementation '%s' not found among loaded providers for '%s'. %n Available implementations: [%s]",
                typedImpl.getSimpleName(), typedService.getSimpleName(), getLoadedImplementations(loader)
        ));
    }

    private <T> String getLoadedImplementations(ServiceLoader<T> loader) {
        return loader.stream()
                .map(p -> p.type().getSimpleName())
                .reduce((a, b) -> a + ", " + b)
                .orElse("empty");
    }
}