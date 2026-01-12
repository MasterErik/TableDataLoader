package su.erik.tabledataloader.mocks;

import su.erik.tabledataloader.exporter.ExportedFile;
import su.erik.tabledataloader.exporter.FileExporter;
import su.erik.tabledataloader.exporter.factory.FileExporterFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class MockFileExporterFactory implements FileExporterFactory {

    @Override
    public FileExporter createExporter(Iterable<?> data, Class<? extends FileExporter> viewClass) {
        return new MockExporter();
    }

    @Override
    public <T> FileExporter createExporter(Iterable<T> data, Class<? extends FileExporter> viewClass, Consumer<T> consumer) {
        return new MockExporter();
    }

    // Внутренний класс экспортера
    static class MockExporter implements FileExporter {
        @Override
        public ExportedFile export() {
            return new ExportedFile() {
                final byte[] content = "mock_report_content".getBytes(StandardCharsets.UTF_8);
                @Override
                public InputStream getInputStream() { return new ByteArrayInputStream(content); }
                @Override
                public long contentLength() { return content.length; }
            };
        }

        @Override
        public String getFullFileName(String baseFileName) {
            return baseFileName + ".mock";
        }
    }
}
