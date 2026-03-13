package org.telegram.bot.providers.media;

import com.drew.lang.annotations.NotNull;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.enums.yt_dlp.MediaPlatform;
import org.telegram.bot.exception.youtube.YtDlpException;

public interface YtDlpProvider {

    File getVideo(@NotNull MediaPlatform mediaPlatform, @NotNull String url) throws YtDlpException;

    File getAudio(@NotNull MediaPlatform mediaPlatform, @NotNull String url) throws YtDlpException;

}
