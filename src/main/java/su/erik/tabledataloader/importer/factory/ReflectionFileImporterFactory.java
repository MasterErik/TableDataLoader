package su.erik.tabledataloader.importer.factory;

import su.erik.tabledataloader.importer.FileImporter;
import su.erik.tabledataloader.importer.ImportMapper;
import com.puls.centralpricing.common.exception.StandardFault;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * Реализация фабрики импортеров через Reflection.
 * Создает экземпляр указанного класса, ожидая конструктор с параметрами:
 * (Class dtoClass, ImportMapper mapper, Map customFilters)
 */
public class ReflectionFileImporterFactory implements FileImporterFactory {

    @Override
    public <T> FileImporter createImporter(
            Class<? extends FileImporter> importerClass,
            Class<T> importDTOClass,
            ImportMapper<T> importMapper,
            Map<String, Object> customFilters) {
        try {
            // Ищем конструктор (Class, ImportMapper, Map)
            Constructor<? extends FileImporter> constructor = importerClass.getConstructor(Class.class, ImportMapper.class, Map.class);
            return constructor.newInstance(importDTOClass, importMapper, customFilters);
        } catch (NoSuchMethodException e) {
            // Если стандартный конструктор не найден, пробуем пустой (для простых импортеров)
            try {
                return importerClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new StandardFault(ex);
            }
        } catch (Exception e) {
            throw new StandardFault(e);
        }
    }
}
