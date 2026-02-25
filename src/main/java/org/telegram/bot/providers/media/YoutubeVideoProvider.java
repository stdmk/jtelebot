package org.telegram.bot.providers.media;

import org.telegram.bot.exception.youtube.YoutubeDownloadException;

import java.io.File;

public interface YoutubeVideoProvider {

    File getVideo(String url) throws YoutubeDownloadException;

}
