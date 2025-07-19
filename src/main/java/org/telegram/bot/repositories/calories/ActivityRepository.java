package org.telegram.bot.repositories.calories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.calories.Activity;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
}
