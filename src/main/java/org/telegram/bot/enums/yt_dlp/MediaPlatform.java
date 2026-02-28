package org.telegram.bot.enums.yt_dlp;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum MediaPlatform {

    YOUTUBE(Set.of("youtube.com", "youtu.be"), false),
    TIKTOK(Set.of("tiktok.com"), true),
    INSTAGRAM(Set.of("instagram.com"), true),
    FACEBOOK(Set.of("facebook.com", "fb.watch"), true),
    X(Set.of("twitter.com", "x.com"), true),
    VK(Set.of("vk.com", "vkvideo.ru"), false),
    REDDIT(Set.of("reddit.com", "redd.it"), true),
    VIMEO(Set.of("vimeo.com"), false),
    DAILYMOTION(Set.of("dailymotion.com", "dai.ly"), false),
    TWITCH(Set.of("twitch.tv"), false),
    RUMBLE(Set.of("rumble.com"), false),
    PINTEREST(Set.of("pinterest.com"), true),
    SOUNDCLOUD(Set.of("soundcloud.com"), false),
    BANDCAMP(Set.of("bandcamp.com"), false),
    OK(Set.of("ok.ru", "odnoklassniki.ru"), false);

    private final Set<String> domains;
    private final boolean needsUserAgent;

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