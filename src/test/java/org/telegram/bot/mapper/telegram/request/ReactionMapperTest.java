package org.telegram.bot.mapper.telegram.request;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.model.request.Reactions;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionType;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeCustomEmoji;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeEmoji;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypePaid;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ReactionMapperTest {

    private final ReactionMapper reactionMapper = new ReactionMapper();

    @Test
    void toReactionsTest() {
        ReactionTypeEmoji oldEmoji = new ReactionTypeEmoji(ReactionType.EMOJI_TYPE, "emoji1");
        ReactionTypeCustomEmoji oldCustomEmoji = new ReactionTypeCustomEmoji(ReactionType.CUSTOM_EMOJI_TYPE, "customEmojiId1");
        ReactionTypePaid oldPaid = new ReactionTypePaid(ReactionType.PAID_TYPE);

        ReactionTypeEmoji newEmoji = new ReactionTypeEmoji(ReactionType.EMOJI_TYPE, "emoji2");
        ReactionTypeCustomEmoji newCustomEmoji = new ReactionTypeCustomEmoji(ReactionType.CUSTOM_EMOJI_TYPE, "customEmojiId2");
        ReactionTypePaid newPaid = new ReactionTypePaid(ReactionType.PAID_TYPE);

        List<ReactionType> oldReactions = List.of(oldEmoji, oldCustomEmoji, oldPaid, mock(ReactionType.class));
        List<ReactionType> newReactions = List.of(newEmoji, newCustomEmoji, newPaid, mock(ReactionType.class));

        Reactions reactions = reactionMapper.toReactions(oldReactions, newReactions);

        assertNotNull(reactions);

        List<String> oldEmojis = reactions.getOldEmojis();
        assertEquals(1, oldEmojis.size());
        assertEquals(oldEmoji.getEmoji(), oldEmojis.get(0));

        List<String> oldCustomEmojis = reactions.getOldCustomEmojisIds();
        assertEquals(1, oldCustomEmojis.size());
        assertEquals(oldCustomEmoji.getCustomEmojiId(), oldCustomEmojis.get(0));

        List<String> newEmojis = reactions.getNewEmojis();
        assertEquals(1, newEmojis.size());
        assertEquals(newEmoji.getEmoji(), newEmojis.get(0));

        List<String> newCustomEmojis = reactions.getNewCustomEmojisIds();
        assertEquals(1, newCustomEmojis.size());
        assertEquals(newCustomEmoji.getCustomEmojiId(), newCustomEmojis.get(0));
    }

}