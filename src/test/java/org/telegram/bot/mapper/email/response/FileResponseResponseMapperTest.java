package org.telegram.bot.mapper.email.response;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileResponseResponseMapperTest {

    private final FileResponseResponseMapper fileResponseResponseMapper = new FileResponseResponseMapper();

    @Test
    void getMappingClassTest() {
        Class<FileResponse> expected = FileResponse.class;
        Class<? extends BotResponse> actual = fileResponseResponseMapper.getMappingClass();
        assertEquals(expected, actual);
    }

    @Test
    void mapTest() {
        final List<File> files = new ArrayList<>();
        final String text = "text";
        FileResponse fileResponse = new FileResponse()
                .setText(text)
                .addFiles(files);

        EmailResponse emailResponse = fileResponseResponseMapper.map(fileResponse);

        assertNotNull(emailResponse);
        assertEquals(files, emailResponse.getAttachments());
        assertEquals(text, emailResponse.getText());
    }

}