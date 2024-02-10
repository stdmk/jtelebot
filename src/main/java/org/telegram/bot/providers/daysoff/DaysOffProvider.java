package org.telegram.bot.providers.daysoff;

import java.util.List;
import java.util.Locale;

/**
 * Provider of days off information.
 */
public interface DaysOffProvider {

    /**
     * Get locale of provider.
     *
     * @return provider's locale.
     */
    Locale getLocale();

    /**
     * Get days off information for month in year.
     *
     * @param year calendar year
     * @param month calendar month.
     * @return list of days off.
     */
    List<Integer> getDaysOffInMonth(int year, int month);

}
