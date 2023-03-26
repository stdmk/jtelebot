package org.telegram.bot.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TrainSubscription;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface TrainSubscriptionRepository extends JpaRepository<TrainSubscription, Long> {
    TrainSubscription findByIdAndUser(Long id, User user);
    Page<TrainSubscription> findByUserOrderByStartDateDesc(User user, Pageable pageable);
    List<TrainSubscription> findByUserAndActive(User user, Boolean active);
}
