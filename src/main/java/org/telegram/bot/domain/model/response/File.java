package org.telegram.bot.domain.model.response;

import lombok.Getter;

import java.io.InputStream;

@Getter
public class File {
    
    public File(String fileId) {
        this.fileId = fileId;
        this.fileType = FileType.FILE;
        this.url = null;
        this.diskFile = null;
        this.inputStream = null;
        this.name = null;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, String url) {
        this.fileId = null;
        this.fileType = fileType;
        this.url = url;
        this.diskFile = null;
        this.inputStream = null;
        this.name = null;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, String url, FileSettings fileSettings) {
        this.fileId = null;
        this.fileType = fileType;
        this.url = url;
        this.diskFile = null;
        this.inputStream = null;
        this.name = null;
        this.fileSettings = fileSettings;
    }

    public File(FileType fileType, String url, String name, FileSettings fileSettings) {
        this.fileId = null;
        this.fileType = fileType;
        this.url = url;
        this.diskFile = null;
        this.inputStream = null;
        this.name = name;
        this.fileSettings = fileSettings;
    }

    public File(FileType fileType, java.io.File diskFile) {
        this.fileId = null;
        this.fileType = fileType;
        this.url = null;
        this.diskFile = diskFile;
        this.inputStream = null;
        this.name = null;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, java.io.File diskFile, FileSettings fileSettings) {
        this.fileId = null;
        this.fileType = fileType;
        this.url = null;
        this.diskFile = diskFile;
        this.inputStream = null;
        this.name = null;
        this.fileSettings = fileSettings;
    }

    public File(FileType fileType, InputStream inputStream, String name) {
        this.fileId = null;
        this.fileType = fileType;
        this.url = null;
        this.diskFile = null;
        this.inputStream = inputStream;
        this.name = name;
        this.fileSettings = new FileSettings();
    }

    public File(FileType fileType, InputStream inputStream, String name, FileSettings fileSettings) {
        this.fileId = null;
        this.fileType = fileType;
        this.url = null;
        this.diskFile = null;
        this.inputStream = inputStream;
        this.name = name;
        this.fileSettings = fileSettings;
    }

    public File(FileType fileType, String url, String name) {
        this.fileId = null;
        this.fileType = fileType;
        this.url = url;
        this.diskFile = null;
        this.inputStream = null;
        this.name = name;
        this.fileSettings = new FileSettings();
    }

    private final String fileId;
    private final FileType fileType;
    private final String url;
    private final java.io.File diskFile;
    private final InputStream inputStream;
    private final String name;
    private final FileSettings fileSettings;
}
