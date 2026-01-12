package su.erik.tabledataloader.utils;

import org.springframework.web.multipart.MultipartFile;
import su.erik.tabledataloader.dto.InputFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Адаптер, позволяющий использовать Spring MultipartFile внутри TableDataLoader.
 */
public class SpringInputFileAdapter implements InputFile {
    private final MultipartFile springFile;

    public SpringInputFileAdapter(MultipartFile springFile) {
        this.springFile = springFile;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return springFile.getInputStream();
    }

    @Override
    public String getOriginalFilename() {
        return springFile.getOriginalFilename();
    }

    @Override
    public long getSize() {
        return springFile.getSize();
    }
}
