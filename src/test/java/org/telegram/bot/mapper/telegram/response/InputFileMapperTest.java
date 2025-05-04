package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.InputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InputFileMapperTest {

    @Mock
    private BotStats botStats;

    @InjectMocks
    private InputFileMapper inputFileMapper;

    @Test
    void toInputUnknownFileTest() {
        File file = Mockito.mock(File.class);

        IllegalArgumentException illegalArgumentException = assertThrows((IllegalArgumentException.class), () -> inputFileMapper.toInputFile(file));

        assertTrue(illegalArgumentException.getMessage().startsWith("Unknown type of File: "));
        verify(botStats).incrementErrors(file, "unable to map file");
    }

    @ParameterizedTest
    @MethodSource("provideFiles")
    void toInputFileByFileIdTest(File file, String expectedAttachName, String expectedFileName) {
        InputFile inputFile = inputFileMapper.toInputFile(file);

        assertNotNull(inputFile);
        assertEquals(expectedAttachName, inputFile.getAttachName());
        assertEquals(expectedFileName, inputFile.getMediaName());
    }

    private static Stream<Arguments> provideFiles() {
        java.io.File diskFile = mock(java.io.File.class);
        when(diskFile.getName()).thenReturn("name");

        return Stream.of(
                Arguments.of(new File("fileId"), "fileId", null),
                Arguments.of(new File(FileType.FILE, "url"), "url", null),
                Arguments.of(new File(FileType.FILE, mock(InputStream.class), "name"), "attach://name", "name"),
                Arguments.of(new File(FileType.FILE, diskFile), "attach://name", "name")
        );
    }

}