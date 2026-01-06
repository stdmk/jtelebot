package org.telegram.bot.domain.model.response;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class File {
    
    public File(String fileId) {
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
        this.fileId = null;
        this.fileType = fileType;
        this.url = url;
        this.diskFile = null;
        this.bytes = null;
        this.name = null;
        this.text = null;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, String url, FileSettings fileSettings) {
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
        this.fileId = null;
        this.fileType = fileType;
        this.url = null;
        this.diskFile = null;
        this.bytes = bytes;
        this.name = name;
        this.text = text;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, String url, String name) {
        this.fileId = null;
        this.fileType = fileType;
        this.url = url;
        this.diskFile = null;
        this.bytes = null;
        this.name = name;
        this.text = null;
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
