package org.telegram.bot.domain.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.utils.TextUtils;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class Message {
    private Chat chat;
    private User user;
    private Integer messageId;
    private Message replyToMessage;
    private String text;
    private LocalDateTime dateTime;
    private LocalDateTime editDateTime;
    private MessageKind messageKind;
    private MessageContentType messageContentType;
    private List<Attachment> attachments;
    private Reactions reactions;

    @JsonIgnore
    public Long getChatId() {
        return this.chat.getChatId();
    }

    @JsonIgnore
    public boolean isGroupChat() {
        return this.getChat().getChatId() < 0;
    }

    public boolean hasText() {
        return this.text != null && !text.isEmpty();
    }

    public boolean hasCommandArgument() {
        return getCommandArgument() != null;
    }

    @JsonIgnore
    public String getCommandArgument() {
        return TextUtils.cutCommandInText(this.text);
    }

    public boolean hasAttachment() {
        return this.attachments != null && !this.attachments.isEmpty();
    }

    public boolean hasReplyToMessage() {
        return this.replyToMessage != null;
    }

    public boolean hasReactions() {
        return this.reactions != null;
    }

    @JsonIgnore
    public boolean isCallback() {
        return MessageKind.CALLBACK.equals(this.messageKind);
    }

    @JsonIgnore
    public boolean isEditMessage() {
        return MessageKind.EDIT.equals(this.messageKind);
    }

}
