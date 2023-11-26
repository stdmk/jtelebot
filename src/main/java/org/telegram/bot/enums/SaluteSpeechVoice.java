package org.telegram.bot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum SaluteSpeechVoice {

    NEC("Наталья", "Nec", "ru", false),
    BYS("Борис", "Bys", "ru", true),
    MAY("Марфа", "May", "ru", false),
    TUR("Тарас", "Tur", "ru", false),
    OST("Александра", "Ost", "ru", false),
    PON("Сергей", "Pon", "ru", false),
    KIN("Kira", "Kin", "en", true),
    ;

    private final String name;
    private final String code;
    private final String langCode;
    private final boolean defaultForLanguage;

    @Nullable
    public static SaluteSpeechVoice getByName(String name) {
        return Arrays.stream(SaluteSpeechVoice.values())
                .filter(saluteSpeechVoice -> saluteSpeechVoice.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static SaluteSpeechVoice getByLangCode(String langCode) {
        return Arrays.stream(SaluteSpeechVoice.values())
                .filter(SaluteSpeechVoice::isDefaultForLanguage)
                .filter(saluteSpeechVoice -> saluteSpeechVoice.getLangCode().equalsIgnoreCase(langCode))
                .findFirst()
                .orElse(null);
    }
}
