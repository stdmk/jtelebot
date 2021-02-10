package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TvChannel;

import java.util.List;

public interface TvChannelRepository extends JpaRepository<TvChannel, Integer> {
    List<TvChannel> findByNameContainsIgnoreCase(String name);
}
