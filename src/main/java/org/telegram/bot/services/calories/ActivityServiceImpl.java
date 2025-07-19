package org.telegram.bot.services.calories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.calories.Activity;
import org.telegram.bot.repositories.calories.ActivityRepository;

@RequiredArgsConstructor
@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;

    @Override
    public Activity save(Activity activity) {
        return activityRepository.save(activity);
    }

    @Override
    public Activity get(long activityId) {
        return activityRepository.findById(activityId).orElse(null);
    }

    @Override
    public void remove(Activity activity) {
        activityRepository.delete(activity);
    }
}
