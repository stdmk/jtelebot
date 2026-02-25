package org.telegram.bot.domain.model.response;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class File {
    
    public File(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("Empty fileId");
        }

        this.fileId = fileId;
        this.fileType = FileType.FILE;
        this.url = null;
        this.diskFile = null;
        this.bytes = null;
        this.name = null;
        this.text = null;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Empty url");
        }

        this.fileId = null;
        this.fileType = fileType;
        this.url = url;
        this.diskFile = null;
        this.bytes = null;
        this.name = null;
        this.text = null;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, String url, String name) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Empty url");
        }

        this.fileId = null;
        this.fileType = fileType;
        this.url = url;
        this.diskFile = null;
        this.bytes = null;
        this.name = name;
        this.text = null;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, String url, FileSettings fileSettings) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Empty url");
        }

        this.fileId = null;
        this.fileType = fileType;
        this.url = url;
        this.diskFile = null;
        this.bytes = null;
        this.name = null;
        this.text = null;
        this.fileSettings = fileSettings;
    }

    public File(FileType fileType, String url, String name, FileSettings fileSettings) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Empty url");
        }

        this.fileId = null;
        this.fileType = fileType;
        this.url = url;
        this.diskFile = null;
        this.bytes = null;
        this.name = name;
        this.text = null;
        this.fileSettings = fileSettings;
    }

    public File(FileType fileType, java.io.File diskFile) {
        if (diskFile == null || !diskFile.exists()) {
            throw new IllegalArgumentException("Not existence disk file");
        }

        this.fileId = null;
        this.fileType = fileType;
        this.url = null;
        this.diskFile = diskFile;
        this.bytes = null;
        this.name = null;
        this.text = null;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, java.io.File diskFile, FileSettings fileSettings) {
        if (diskFile == null || !diskFile.exists()) {
            throw new IllegalArgumentException("Not existence disk file");
        }

        this.fileId = null;
        this.fileType = fileType;
        this.url = null;
        this.diskFile = diskFile;
        this.bytes = null;
        this.name = null;
        this.text = null;
        this.fileSettings = fileSettings;
    }

    public File(FileType fileType, byte[] bytes, String name) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Empty bytes");
        }

        this.fileId = null;
        this.fileType = fileType;
        this.url = null;
        this.diskFile = null;
        this.bytes = bytes;
        this.name = name;
        this.text = null;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, byte[] bytes, String name, String text) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Empty bytes");
        }

        this.fileId = null;
        this.fileType = fileType;
        this.url = null;
        this.diskFile = null;
        this.bytes = bytes;
        this.name = name;
        this.text = text;
        this.fileSettings = new FileSettings();
    }

    private final String fileId;
    private final FileType fileType;
    private final String url;
    private final java.io.File diskFile;
    private final byte[] bytes;
    private final String name;
    private final String text;
    private final FileSettings fileSettings;
}
