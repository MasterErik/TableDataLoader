package su.erik.tabledataloader.importer.loader;

import com.puls.centralpricing.common.exception.StandardFault;
import su.erik.tabledataloader.LoaderRegistry;
import su.erik.tabledataloader.archive.AbstractIterator;
import su.erik.tabledataloader.archive.EntryModel;
import su.erik.tabledataloader.archive.ZipArchiveIterator;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.model.ResultDTO;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ZipFileLoader implements FileLoader {

    private final Class<?> dtoClass;
    private final ImportMapper<?> mapper;
    private final Map<String, Object> customFilters;

    public ZipFileLoader(Class<?> dtoClass, ImportMapper<?> mapper, Map<String, Object> customFilters) {
        this.dtoClass = dtoClass;
        this.mapper = mapper;
        this.customFilters = customFilters;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ResultDTO importFile(InputStream inputStream, String name, long size, String entity, Long userId) {
        long totalCount = 0;
        long lastUploadId = 0;

        LoaderRegistry registry = (LoaderRegistry) customFilters.get("loaderRegistry");
        if (registry == null) {
            throw new IllegalStateException("LoaderRegistry is missing in context (customFilters)");
        }

        try (AbstractIterator iterator = new ZipArchiveIterator(inputStream)) {
            while (iterator.hasNext()) {
                EntryModel entry = iterator.next();

                String entryName = entry.name();
                if (entryName.endsWith("/") || entryName.endsWith("\\")) {
                    continue;
                }

                String extension = getExtension(entryName);

                if (registry.getLoaderClass(extension) != null) {
                    FileLoader loader = registry.createLoader(
                            extension,
                            (Class) dtoClass,
                            (ImportMapper) mapper,
                            customFilters
                    );

                    ResultDTO entryResult = loader.importFile(entry.content(), entryName, entry.size(), entity, userId);
                    totalCount += entryResult.count();
                    if (entryResult.uploadId() != null && entryResult.uploadId() > 0) {
                        lastUploadId = entryResult.uploadId();
                    }
                }
            }
        } catch (IOException exception) {
            throw new StandardFault(exception);
        }

        return new ResultDTO(lastUploadId, totalCount);
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex >= 0) ? fileName.substring(dotIndex + 1) : "";
    }

    private static class ShieldedInputStream extends FilterInputStream {
        public ShieldedInputStream(InputStream inputStream) { super(inputStream); }
        @Override public void close() throws IOException { /* no-op */ }
    }
}
