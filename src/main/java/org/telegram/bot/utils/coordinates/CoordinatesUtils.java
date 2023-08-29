package org.telegram.bot.utils.coordinates;

import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class CoordinatesUtils {

    public static final Pattern COORDINATE_PATTERN1 = Pattern.compile("(\\d+)\\s*°\\s*(\\d+)\\s*'\\s*(\\d+[.,]*\\d*)\""); //35° 54' 36.24"
    public static final Pattern COORDINATE_PATTERN2 = Pattern.compile("(\\d+)\\s*°\\s*(\\d+[.,]*\\d*)\\s*'"); //35° 54.24"
    public static final Pattern COORDINATE_PATTERN3 = Pattern.compile("(\\d+[.,]*\\d*)"); //35.543624
    public static final Pattern COORDINATE_PATTERN4 = Pattern.compile("(\\d+)_(\\d+)_(\\d+)_(\\d+)"); ///location_56_503_35_543624

    @Nullable
    public Coordinates parseCoordinates(String data) {
        Coordinates coordinates = null;
        
        Matcher matcher1 = COORDINATE_PATTERN1.matcher(data);
        Matcher matcher2 = COORDINATE_PATTERN2.matcher(data);
        Matcher matcher3 = COORDINATE_PATTERN3.matcher(data);
        Matcher matcher4 = COORDINATE_PATTERN4.matcher(data);

        Double latitude = null;
        Double longitude = null;

        if (matcher4.find()) {
            coordinates = getCoordinateFromPattern4(matcher4);
        } else if (matcher1.find()) {
            latitude = getCoordinateFromPattern1(matcher1);
            if (matcher1.find()) {
                longitude = getCoordinateFromPattern1(matcher1);
            }
        } else if (matcher2.find()) {
            latitude = getCoordinateFromPattern2(matcher2);
            if (matcher2.find()) {
                longitude = getCoordinateFromPattern2(matcher2);
            }
        } else if (matcher3.find()) {
            latitude = getCoordinateFromPattern3(matcher3);
            if (matcher3.find()) {
                longitude = getCoordinateFromPattern3(matcher3);
            }
        }
        
        if (latitude != null && longitude != null) {
            coordinates = new Coordinates(latitude, longitude);
        }
    
        return coordinates;
    }
    
    private Double getCoordinateFromPattern1(Matcher matcher) {
        double deg = Double.parseDouble(replaceComma(matcher.group(1)));
        double min = Double.parseDouble(replaceComma(matcher.group(2)));
        double sec = Double.parseDouble(replaceComma(matcher.group(3)));

        return deg + (min / 60) + (sec / 3600);
    }

    private Double getCoordinateFromPattern2(Matcher matcher) {
        double deg = Double.parseDouble(replaceComma(matcher.group(1)));
        double min = Double.parseDouble(replaceComma(matcher.group(2)));

        return deg + (min / 60);
    }

    private Double getCoordinateFromPattern3(Matcher matcher) {
        return Double.parseDouble(matcher.group(1).replaceAll(",", "."));
    }

    private Coordinates getCoordinateFromPattern4(Matcher matcher) {
        return new Coordinates(
                Double.parseDouble(matcher.group(1) + "." + matcher.group(2)), 
                Double.parseDouble(matcher.group(3) + "." + matcher.group(4)));
    }

    private String replaceComma(String text) {
        return text.replaceAll(",", ".");
    }
    
}
