package org.telegram.bot.domain.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.entities.CustomReactionDayStats;
import org.telegram.bot.domain.entities.CustomReactionMonthStats;
import org.telegram.bot.domain.entities.CustomReactionStats;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class CustomerReactionsStats {
    List<CustomReactionStats> reactionStatsList;
    List<CustomReactionMonthStats> reactionMonthStatsList;
    List<CustomReactionDayStats> reactionDayStatsList;
}
