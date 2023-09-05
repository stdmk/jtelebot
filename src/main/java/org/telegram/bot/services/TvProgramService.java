package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.TvProgram;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.TvProgram}.
 */
public interface TvProgramService {
    /**
     * Get a TvProgram.
     *
     * @param tvProgramId of TvProgram to get.
     * @return the persisted entity.
     */
    TvProgram get(Integer tvProgramId);

    /**
     * Get list of TvPrograms.
     *
     * @param tvProgramTitle of TvProgram to get.
     * @param dateTime of TvProgram to get.
     * @param hours count to program
     * @return list of persisted entities.
     */
    List<TvProgram> get(String tvProgramTitle, LocalDateTime dateTime, int hours);

    /**
     * Get list of TvPrograms.
     *
     * @param tvChannel entity of TvProgram to get.
     * @param dateTime of TvProgram to get.
     * @param hours count to program
     * @return list of persisted entities.
     */
    List<TvProgram> get(TvChannel tvChannel, LocalDateTime dateTime, int hours);

    /**
     * Save a TvProgram.
     *
     * @param tvProgram the entity to save.
     */
    void save(TvProgram tvProgram);

    /**
     * Save a list of TvProgram.
     *
     * @param tvProgramList entities to save.
     * @return list of persisted entities.
     */
    List<TvProgram> save(List<TvProgram> tvProgramList);

    /**
     * Clear a TvProgram table.
     */
    void clearTable();
}
