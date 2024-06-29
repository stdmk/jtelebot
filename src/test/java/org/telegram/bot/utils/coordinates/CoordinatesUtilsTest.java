package org.telegram.bot.utils.coordinates;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class CoordinatesUtilsTest {

    @ParameterizedTest
    @MethodSource("provideCoordinates")
    void parseCoordinates(String data, Coordinates expectedCoordinates) {
        Coordinates actualCoordinates = CoordinatesUtils.parseCoordinates(data);

        assertNotNull(actualCoordinates);
        assertEquals(expectedCoordinates.latitude(), actualCoordinates.latitude());
        assertEquals(expectedCoordinates.longitude(), actualCoordinates.longitude());
    }

    private static Stream<Arguments> provideCoordinates() {
        return Stream.of(
                Arguments.of("56° 50' 3\" 35° 54' 36,24\"", new Coordinates(56.83416666666667, 35.910066666666665)),
                Arguments.of("56° 50.3' 35° 54,3624'", new Coordinates(56.83833333333333, 35.90604)),
                Arguments.of("56.83417 35,90604", new Coordinates(56.83417, 35.90604)),
                Arguments.of("/location_56_83417_35_90604", new Coordinates(56.83417, 35.90604)));
    }

}