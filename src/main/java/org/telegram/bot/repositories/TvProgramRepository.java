package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.TvProgram;

import java.time.LocalDateTime;
import java.util.List;

public interface TvProgramRepository extends JpaRepository<TvProgram, Integer> {
    List<TvProgram> findByStopBetweenAndTitleContainsIgnoreCase(LocalDateTime dateStart, LocalDateTime dateEnd, String title);
    List<TvProgram> findByChannelAndStopBetween(TvChannel tvChannel, LocalDateTime dateStart, LocalDateTime dateEnd);

    @Modifying
    @Query("DELETE FROM TvProgram")
    void clearTable();
}
