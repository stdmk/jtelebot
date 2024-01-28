package org.telegram.bot.utils;

import lombok.EqualsAndHashCode;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

@EqualsAndHashCode(callSuper = false)
public class MultipartInputStreamFileResource extends InputStreamResource {

    private final String filename;

    public MultipartInputStreamFileResource(InputStream inputStream, String filename) {
        super(inputStream);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long contentLength() throws IOException {
        return -1;
    }
}
