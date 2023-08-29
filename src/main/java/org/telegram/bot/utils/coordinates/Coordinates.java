package org.telegram.bot.utils.coordinates;

import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@Getter
public class Coordinates {

    @NotNull
    private final Double latitude;

    @NotNull
    private final Double longitude;

    public Coordinates(@NotNull Double latitude, @NotNull Double longitude) {
        this.latitude = Objects.requireNonNull(latitude);
        this.longitude = Objects.requireNonNull(longitude);
    }

}
