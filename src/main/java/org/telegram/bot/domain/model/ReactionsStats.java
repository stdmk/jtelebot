package org.telegram.bot.domain.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.entities.ReactionDayStats;
import org.telegram.bot.domain.entities.ReactionMonthStats;
import org.telegram.bot.domain.entities.ReactionStats;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class ReactionsStats {
    List<ReactionStats> reactionStatsList;
    List<ReactionMonthStats> reactionMonthStatsList;
    List<ReactionDayStats> reactionDayStatsList;
}
