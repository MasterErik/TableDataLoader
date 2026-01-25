package su.erik.tabledataloader.importer.csv;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.exceptions.CsvBeanIntrospectionException;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.annotation.ImporterBindByIndex;
import su.erik.tabledataloader.importer.model.ImportDynamic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamicColumnMappingStrategy<T> extends ColumnPositionMappingStrategy<T> {

    private int startsFromIndex;
    private final Map<Integer, Method> indexMethods = new HashMap<>();
    private Constructor<? extends T> constructor;
    private ImportMapper<T> importMapper;
    private Map<String, Object> customFilters;

    public void setImportMapper(ImportMapper<T> m) { this.importMapper = m; }
    public void setCustomFilters(Map<String, Object> f) { this.customFilters = f; }

    public void setProcessedHeader(String[] header) {
        if (header == null) return;
        List<String> dynamicHeaders = new ArrayList<>();
        for (int i = startsFromIndex; i < header.length; i++) {
            dynamicHeaders.add(header[i] != null ? header[i].trim() : "");
        }
        if (customFilters != null) {
            customFilters.put("dynamicHeaders", dynamicHeaders);
            if (importMapper != null) {
                String t = (String) customFilters.get("tempTableName");
                if (t == null) t = "temp_" + System.currentTimeMillis();
                importMapper.createTempTable(dynamicHeaders, t);
            }
        }
    }

    @Override
    public T populateNewBean(String[] line) throws CsvBeanIntrospectionException {
        if (line == null || line.length == 0) return null;
        try {
            int dynamicSize = Math.max(0, line.length - startsFromIndex);
            T bean = constructor.newInstance(dynamicSize);
            for (int i = 0; i < line.length; i++) {
                if (i < startsFromIndex) {
                    Method m = indexMethods.get(i);
                    if (m != null) m.invoke(bean, line[i]);
                } else if (bean instanceof ImportDynamic) {
                    ((ImportDynamic<?>) bean).setterDynamicValue(line[i], i - startsFromIndex);
                }
            }
            return bean;
        } catch (Exception e) {
            throw new CsvBeanIntrospectionException(e.getMessage());
        }
    }

    @Override
    public void setType(Class<? extends T> type) throws CsvBeanIntrospectionException {
        super.setType(type);
        DynamicColumn ann = type.getAnnotation(DynamicColumn.class);
        if (ann != null) {
            this.startsFromIndex = ann.startsFromIndex();
            try { this.constructor = type.getConstructor(Integer.class); }
            catch (Exception e) { try { this.constructor = type.getConstructor(int.class); } catch (Exception _) {} }
            for (Method m : type.getMethods()) {
                if (m.isAnnotationPresent(ImporterBindByIndex.class)) {
                    indexMethods.put(m.getAnnotation(ImporterBindByIndex.class).value(), m);
                }
            }
        }
    }
}