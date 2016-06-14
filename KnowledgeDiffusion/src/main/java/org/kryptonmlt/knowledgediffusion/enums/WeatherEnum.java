package org.kryptonmlt.knowledgediffusion.enums;

/**
 *
 * @author Kurt
 */
public enum WeatherEnum {

    SNOWY,
    CLOUDY,
    SUNNY,
    OVERCAST,
    RAINY,
    FOGGY,
    DUSTY;

    public WeatherEnum getWeather(int type) {
        switch (type) {
            case 0:
                return SNOWY;
            case 1:
                return CLOUDY;
            case 2:
                return SUNNY;
            case 3:
                return OVERCAST;
            case 4:
                return RAINY;
            case 5:
                return FOGGY;
            case 6:
                return DUSTY;
            default:
                throw new IllegalArgumentException("Weather type " + type + " not supported");
        }
    }
}
