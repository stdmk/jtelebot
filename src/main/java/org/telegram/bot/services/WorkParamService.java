package org.telegram.bot.services;

import org.telegram.bot.domain.entities.WorkParam;

import java.util.List;

public interface WorkParamService {
    /**
     * Save a WorkParam.
     *
     * @param workParam the entity to save.
     * @return the persisted entity.
     */
    WorkParam save(WorkParam workParam);

    /**
     * Save WorkParams.
     *
     * @param workParamList list of workParam entities to save.
     */
    void save(List<WorkParam> workParamList);

    /**
     * Get a WorkParams by bot token.
     *
     * @param token bot token of workParams to get.
     * @return the persisted entities.
     */
    List<WorkParam> get(String token);

    /**
     * Get a WorkParams by bot token and names.
     *
     * @param token bot token of workParams to get.
     * @param nameList list of names of workParams
     * @return the persisted entities.
     */
    List<WorkParam> get(String token, List<String> nameList);
}
