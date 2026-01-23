package su.erik.tabledataloader;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.Param;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.dto.UploadDTO;

import java.util.List;
import java.util.Map;

public interface DynamicImportTestMapper extends ImportMapper<DynamicImportTestDTO> {

    @Override
    void insertHeader(UploadDTO upload);

    @Override
    void createTempTable(@Param("allColumnHeaderList") List<String> allColumnHeaderList, @Param("tempTableName") String tempTableName);

    @Override
    void insert(@Param("customFilters") Map<String, Object> customFilters);

    @Override
    void delete(long uploadId);

    @Override
    void finish(@Param("customFilters") Map<String, Object> customFilters);

    @SuppressWarnings("MybatisMapperMethodInspection")
    @Flush
    @Override
    void flush();

    // Дополнительный метод для проверки
    List<Map<String, Object>> selectAll(@Param("tableName") String tableName);

    List<Map<String, Object>> selectResult(@Param("uploadId") long uploadId, @Param("tableName") String tableName);
}