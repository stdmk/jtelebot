package org.telegram.bot.mapper.telegram.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.telegrambots.meta.api.methods.ParseMode;

@Component
public class ParseModeMapper {

    public String toParseMode(FormattingStyle formattingStyle) {
        if (FormattingStyle.HTML.equals(formattingStyle)) {
            return ParseMode.HTML;
        } else if (FormattingStyle.MARKDOWN.equals(formattingStyle)) {
            return ParseMode.MARKDOWN;
        } else if (FormattingStyle.MARKDOWN2.equals(formattingStyle)) {
            return ParseMode.MARKDOWNV2;
        } else {
            throw new IllegalArgumentException("Unknown FormattingStyle: " + formattingStyle);
        }
    }

}
