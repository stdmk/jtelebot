package org.telegram.bot.utils.coordinates;

import javax.validation.constraints.NotNull;
import java.util.Objects;

public record Coordinates(@NotNull Double latitude, @NotNull Double longitude) {

    public Coordinates(@NotNull Double latitude, @NotNull Double longitude) {
        this.latitude = Objects.requireNonNull(latitude);
        this.longitude = Objects.requireNonNull(longitude);
    }

}
