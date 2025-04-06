package org.telegram.bot;

import org.telegram.bot.services.InternationalizationService;

import java.util.Set;

import static org.mockito.Mockito.when;

public class UnitUtils {

    public static void addLengthUnitTranslations(InternationalizationService internationalizationService) {
        when(internationalizationService.getAllTranslations("command.converter.length.fm")).thenReturn(Set.of("fm"));
        when(internationalizationService.getAllTranslations("command.converter.length.pm")).thenReturn(Set.of("pm"));
        when(internationalizationService.getAllTranslations("command.converter.length.nm")).thenReturn(Set.of("nm"));
        when(internationalizationService.getAllTranslations("command.converter.length.mk")).thenReturn(Set.of("mk"));
        when(internationalizationService.getAllTranslations("command.converter.length.mm")).thenReturn(Set.of("mm"));
        when(internationalizationService.getAllTranslations("command.converter.length.cm")).thenReturn(Set.of("cm"));
        when(internationalizationService.getAllTranslations("command.converter.length.dm")).thenReturn(Set.of("dm"));
        when(internationalizationService.getAllTranslations("command.converter.length.m")).thenReturn(Set.of("m"));
        when(internationalizationService.getAllTranslations("command.converter.length.km")).thenReturn(Set.of("km"));
        when(internationalizationService.getAllTranslations("command.converter.length.mi")).thenReturn(Set.of("mi"));
        when(internationalizationService.getAllTranslations("command.converter.length.yd")).thenReturn(Set.of("yd"));
        when(internationalizationService.getAllTranslations("command.converter.length.ft")).thenReturn(Set.of("ft"));
        when(internationalizationService.getAllTranslations("command.converter.length.nch")).thenReturn(Set.of("inch"));
        when(internationalizationService.getAllTranslations("command.converter.length.mn")).thenReturn(Set.of("mn"));
    }

    public static void addTimeUnitTranslations(InternationalizationService internationalizationService) {
        when(internationalizationService.getAllTranslations("command.converter.time.fs")).thenReturn(Set.of("fs"));
        when(internationalizationService.getAllTranslations("command.converter.time.ps")).thenReturn(Set.of("ps"));
        when(internationalizationService.getAllTranslations("command.converter.time.ns")).thenReturn(Set.of("ns"));
        when(internationalizationService.getAllTranslations("command.converter.time.mks")).thenReturn(Set.of("mks"));
        when(internationalizationService.getAllTranslations("command.converter.time.ms")).thenReturn(Set.of("ms"));
        when(internationalizationService.getAllTranslations("command.converter.time.cs")).thenReturn(Set.of("cs"));
        when(internationalizationService.getAllTranslations("command.converter.time.s")).thenReturn(Set.of("s"));
        when(internationalizationService.getAllTranslations("command.converter.time.m")).thenReturn(Set.of("m"));
        when(internationalizationService.getAllTranslations("command.converter.time.h")).thenReturn(Set.of("h"));
        when(internationalizationService.getAllTranslations("command.converter.time.d")).thenReturn(Set.of("d"));
        when(internationalizationService.getAllTranslations("command.converter.time.y")).thenReturn(Set.of("y"));
        when(internationalizationService.getAllTranslations("command.converter.time.c")).thenReturn(Set.of("c"));
    }

}
