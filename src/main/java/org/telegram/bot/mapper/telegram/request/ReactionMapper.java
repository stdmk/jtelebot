package org.telegram.bot.mapper.telegram.request;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.Reactions;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionType;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeCustomEmoji;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeEmoji;

import java.util.List;

@Component
public class ReactionMapper {

    public Reactions toReactions(List<ReactionType> oldReactions, List<ReactionType> newReactions) {
        Reactions reactions = new Reactions();

        for (ReactionType oldReaction : oldReactions) {
            String reactionType = oldReaction.getType();
            if (ReactionType.EMOJI_TYPE.equals(reactionType)) {
                reactions.getOldEmojis().add(((ReactionTypeEmoji) oldReaction).getEmoji());
            } else if (ReactionType.CUSTOM_EMOJI_TYPE.equals(reactionType)) {
                reactions.getOldCustomEmojisIds().add(((ReactionTypeCustomEmoji) oldReaction).getCustomEmojiId());
            } else if (ReactionType.PAID_TYPE.equals(reactionType)) {
                // nothing
            }
        }

        for (ReactionType newReaction : newReactions) {
            String reactionType = newReaction.getType();
            if (ReactionType.EMOJI_TYPE.equals(reactionType)) {
                reactions.getNewEmojis().add(((ReactionTypeEmoji) newReaction).getEmoji());
            } else if (ReactionType.CUSTOM_EMOJI_TYPE.equals(reactionType)) {
                reactions.getNewCustomEmojisIds().add(((ReactionTypeCustomEmoji) newReaction).getCustomEmojiId());
            } else if (ReactionType.PAID_TYPE.equals(reactionType)) {
                // nothing
            }
        }

        return reactions;
    }

}
