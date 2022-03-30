package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.telegram.bot.domain.entities.TvChannel;

import java.util.List;

public interface TvChannelRepository extends JpaRepository<TvChannel, Integer> {
    List<TvChannel> findByNameContainsIgnoreCase(String name);

    @Modifying
    @Query("DELETE FROM TvChannel")
    void clearTable();
}
