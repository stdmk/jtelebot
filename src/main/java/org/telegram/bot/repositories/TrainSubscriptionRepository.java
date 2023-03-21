package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TrainSubscription;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface TrainSubscriptionRepository extends JpaRepository<TrainSubscription, Long> {
    TrainSubscription findByIdAndUser(Long id, User user);
    List<TrainSubscription> findByUserAndActive(User user, Boolean active);
}
