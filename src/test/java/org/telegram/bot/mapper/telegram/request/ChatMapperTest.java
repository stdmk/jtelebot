package org.telegram.bot.mapper.telegram.request;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.telegram.bot.domain.entities.Chat;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMapperTest {

    private final ChatMapper chatMapper = new ChatMapper();

    @ParameterizedTest
    @MethodSource("provideChats")
    void toChat(org.telegram.telegrambots.meta.api.objects.chat.Chat telegramChat, String expectedName) {
        Chat chat = chatMapper.toChat(telegramChat);

        assertEquals(telegramChat.getId(), chat.getChatId());
        assertEquals(expectedName, chat.getName());
    }

    private static Stream<Arguments> provideChats() {
        org.telegram.telegrambots.meta.api.objects.chat.Chat groupChatWithoutTitle = new org.telegram.telegrambots.meta.api.objects.chat.Chat(-1L, "type");

        org.telegram.telegrambots.meta.api.objects.chat.Chat groupChat = new org.telegram.telegrambots.meta.api.objects.chat.Chat(-1L, "type");
        groupChat.setTitle("title");

        org.telegram.telegrambots.meta.api.objects.chat.Chat privateChatWithoutUsername = new org.telegram.telegrambots.meta.api.objects.chat.Chat(1L, "type");
        privateChatWithoutUsername.setFirstName("firstName");

        org.telegram.telegrambots.meta.api.objects.chat.Chat privateChat = new org.telegram.telegrambots.meta.api.objects.chat.Chat(1L, "type");
        privateChat.setUserName("username");

        return Stream.of(
                Arguments.of(groupChatWithoutTitle, ""),
                Arguments.of(groupChat, groupChat.getTitle()),
                Arguments.of(privateChatWithoutUsername, privateChatWithoutUsername.getFirstName()),
                Arguments.of(privateChat, privateChat.getUserName())
        );
    }

}