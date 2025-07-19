package org.telegram.bot.services.calories;

import org.telegram.bot.domain.entities.calories.Activity;

public interface ActivityService {
    Activity save(Activity activity);
    Activity get(long activityId);
    void remove(Activity activity);
}
