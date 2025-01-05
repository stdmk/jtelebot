package org.telegram.bot.providers.ffmpeg;

import org.telegram.bot.exception.ffmpeg.FfmpegException;

import java.io.File;

public interface FfmpegProvider {
    File getVideo(String url, String duration) throws FfmpegException;
}
