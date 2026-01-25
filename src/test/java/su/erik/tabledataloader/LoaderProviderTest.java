package su.erik.tabledataloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.mocks.MockMapParamProvider;
import su.erik.tabledataloader.spi.MapParamProvider;

import java.util.Optional;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoaderProviderTest {

    @Test
    @DisplayName("SPI: Проверка загрузки MapParamProvider")
    void testMapParamProviderLoaded() {
        checkProvider(MapParamProvider.class, MockMapParamProvider.class);
    }

    private <T> void checkProvider(Class<T> serviceInterface, Class<? extends T> expectedImplClass) {
        ServiceLoader<T> loader = ServiceLoader.load(serviceInterface);
        Optional<T> providerOpt = loader.findFirst();

        assertTrue(providerOpt.isPresent(),
                () -> "Провайдер для " + serviceInterface.getName() + " не найден. " +
                        "Проверьте файл в src/test/resources/META-INF/services/");

        T provider = providerOpt.get();
        assertEquals(expectedImplClass, provider.getClass(),
                () -> "Загружена неверная реализация для " + serviceInterface.getSimpleName());
    }
}