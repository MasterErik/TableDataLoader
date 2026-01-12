package su.erik.tabledataloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.exporter.factory.FileExporterFactory;
import su.erik.tabledataloader.importer.factory.FileImporterFactory;
import su.erik.tabledataloader.mocks.MockFileExporterFactory;
import su.erik.tabledataloader.mocks.MockFileImporterFactory;
import su.erik.tabledataloader.mocks.MockMapParamProvider;
import su.erik.tabledataloader.spi.MapParamProvider;

import java.util.Optional;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

    class LoaderProviderTest {

        @Test
        @DisplayName("SPI: Проверка загрузки MapParamProvider")
        void testMapParamProviderLoaded() {
            checkProvider(MapParamProvider.class, MockMapParamProvider.class);
        }

        @Test
        @DisplayName("SPI: Проверка загрузки FileImporterFactory")
        void testFileImporterFactoryLoaded() {
            checkProvider(FileImporterFactory.class, MockFileImporterFactory.class);
        }

        @Test
        @DisplayName("SPI: Проверка загрузки FileExporterFactory")
        void testFileExporterFactoryLoaded() {
            checkProvider(FileExporterFactory.class, MockFileExporterFactory.class);
        }

        /**
         * Универсальный метод проверки провайдера.
         *
         * @param serviceInterface Интерфейс сервиса (SPI).
         * @param expectedImplClass Ожидаемый класс реализации (Mock).
         */
        private <T> void checkProvider(Class<T> serviceInterface, Class<? extends T> expectedImplClass) {
            // 1. Загружаем сервис
            ServiceLoader<T> loader = ServiceLoader.load(serviceInterface);
            Optional<T> providerOpt = loader.findFirst();

            // 2. Проверяем, что провайдер найден
            assertTrue(providerOpt.isPresent(),
                    () -> "Провайдер для " + serviceInterface.getName() + " не найден. " +
                            "Проверьте файл в src/test/resources/META-INF/services/");

            // 3. Проверяем, что загрузился именно наш Mock (а не что-то другое случайно)
            T provider = providerOpt.get();
            assertEquals(expectedImplClass, provider.getClass(),
                    () -> "Загружена неверная реализация для " + serviceInterface.getSimpleName());

            System.out.println("SUCCESS: " + serviceInterface.getSimpleName() + " -> " + provider.getClass().getName());
        }

}
