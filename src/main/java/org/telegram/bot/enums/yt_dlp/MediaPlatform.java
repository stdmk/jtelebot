package org.telegram.bot.enums.yt_dlp;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum MediaPlatform {

    YOUTUBE(Set.of("youtube.com", "youtu.be"), false, Set.of(MediaType.VIDEO, MediaType.AUDIO)),
    TIKTOK(Set.of("tiktok.com"), true, Set.of(MediaType.VIDEO)),
    INSTAGRAM(Set.of("instagram.com"), true, Set.of(MediaType.VIDEO)),
    FACEBOOK(Set.of("facebook.com", "fb.watch"), true, Set.of(MediaType.VIDEO)),
    X(Set.of("twitter.com", "x.com"), true, Set.of(MediaType.VIDEO)),
    VK(Set.of("vk.com", "vkvideo.ru"), false, Set.of(MediaType.VIDEO, MediaType.AUDIO)),
    REDDIT(Set.of("reddit.com", "redd.it"), true, Set.of(MediaType.VIDEO)),
    VIMEO(Set.of("vimeo.com"), false, Set.of(MediaType.VIDEO)),
    DAILYMOTION(Set.of("dailymotion.com", "dai.ly"), false, Set.of(MediaType.VIDEO)),
    TWITCH(Set.of("twitch.tv"), false, Set.of(MediaType.VIDEO)),
    RUMBLE(Set.of("rumble.com"), false, Set.of(MediaType.VIDEO)),
    PINTEREST(Set.of("pinterest.com"), true, Set.of(MediaType.VIDEO)),
    SOUNDCLOUD(Set.of("soundcloud.com"), false, Set.of(MediaType.AUDIO)),
    BANDCAMP(Set.of("bandcamp.com"), false, Set.of(MediaType.AUDIO)),
    OK(Set.of("ok.ru", "odnoklassniki.ru"), false, Set.of(MediaType.VIDEO)),
    ;

    private final Set<String> domains;
    private final boolean needsUserAgent;
    private final Set<MediaType> supportedMediaTypes;

    @Nullable
    public static MediaPlatform getByUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(mediaPlatform -> mediaPlatform.getDomains().stream().anyMatch(url::contains))
                .findFirst()
                .orElse(null);
    }

}