package su.erik.tabledataloader.importer;

import com.glaforge.i18n.io.CharsetToolkit;
import com.puls.centralpricing.common.exception.StandardFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.erik.tabledataloader.config.Constant;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class EncodingDetector {

    private static final Logger log = LoggerFactory.getLogger(EncodingDetector.class);
    private static final int BOM_SIZE = 4096; // Читаем 4KB для анализа

    public static Charset detectCharset(InputStream inputStream) throws IOException {
        if (!inputStream.markSupported()) {
            throw new IOException("InputStream must support mark/reset for encoding detection");
        }

        inputStream.mark(BOM_SIZE);
        byte[] buffer = new byte[BOM_SIZE];
        int read = inputStream.read(buffer);
        inputStream.reset();

        if (read == -1) {
            return StandardCharsets.UTF_8; // Пустой файл
        }

        // Обрезаем буфер до реально прочитанного размера, если файл меньше 4KB
        byte[] actualBytes = buffer;
        if (read < BOM_SIZE) {
            actualBytes = new byte[read];
            System.arraycopy(buffer, 0, actualBytes, 0, read);
        }

        CharsetToolkit toolkit = new CharsetToolkit(actualBytes);
        toolkit.setDefaultCharset(Constant.WINDOWS_1251_CHARSET); // Используем CP1251 как дефолт для России

        // guessEncoding() может быть дорогим, но мы ограничили его 4KB
        Charset charset = toolkit.guessEncoding();
        log.debug("Detected encoding: {}", charset);
        return charset;
    }

    public static InputStreamReader getReader(InputStream inputStream) {
        try {
            // Оборачиваем в BufferedInputStream для поддержки mark/reset
            BufferedInputStream bufferedInputStream;
            if (inputStream instanceof BufferedInputStream) {
                bufferedInputStream = (BufferedInputStream) inputStream;
            } else {
                bufferedInputStream = new BufferedInputStream(inputStream);
            }

            Charset charset = detectCharset(bufferedInputStream);
            return new InputStreamReader(bufferedInputStream, charset);
        } catch (IOException e) {
            throw new StandardFault(e);
        }
    }
}
