package org.kryptonmlt.automatedvisualizer.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jzy3d.colors.Color;

/**
 *
 * @author Kurt
 */
public class ColorUtils {

    private static ColorUtils instance = null;
    private final Color[] lightColors;
    private final Color[] darkColors;
    private int l = 0;
    private int d = 0;
    private final static float LIGHT_FACTOR = 1.3f;
    private final static float DARK_FACTOR = 0.6f;
    private final static float LIGHT_ALPHA = 0.8f;

    private ColorUtils() {
        List<Color> tempColors = generateColors();
        lightColors = new Color[tempColors.size()];
        darkColors = new Color[tempColors.size()];
        for (int i = 0; i < tempColors.size(); i++) {
            lightColors[i] = lightenColor(tempColors.get(i));
            darkColors[i] = darkenColor(tempColors.get(i));
        }
    }

    public static ColorUtils getInstance() {
        if (instance == null) {
            instance = new ColorUtils();
        }
        return instance;
    }

    public synchronized Color getNextLightColor() {
        Color c = lightColors[l % lightColors.length];
        l++;
        return c;
    }

    public synchronized Color getNextDarkColor() {
        Color c = darkColors[d % darkColors.length];
        d++;
        return c;
    }

    public synchronized Color getLightColor(int p) {
        return lightColors[p % lightColors.length];
    }

    public synchronized Color getDarkColor(int p) {
        return darkColors[p % darkColors.length];
    }

    public boolean isNextLightUnique() {
        return l < lightColors.length;
    }

    public boolean isNextDarkUnique() {
        return d < darkColors.length;
    }

    private List<Color> generateColors() {
        List<Color> c = new ArrayList<>();
        for (int r = 0; r < 100; r++) {
            c.add(new Color(r * 255 / 100, 255, 0));
        }
        for (int g = 100; g > 0; g--) {
            c.add(new Color(255, g * 255 / 100, 0));
        }
        for (int b = 0; b < 100; b++) {
            c.add(new Color(255, 0, b * 255 / 100));
        }
        for (int r = 100; r > 0; r--) {
            c.add(new Color(r * 255 / 100, 0, 255));
        }
        for (int g = 0; g < 100; g++) {
            c.add(new Color(0, g * 255 / 100, 255));
        }
        for (int b = 100; b > 0; b--) {
            c.add(new Color(0, 255, b * 255 / 100));
        }
        c.add(new Color(0, 255, 0));
        Collections.shuffle(c);
        return c;
    }

    public Color darkenColor(Color c) {
        return new Color(c.r * DARK_FACTOR, c.g * DARK_FACTOR, c.b * DARK_FACTOR, c.a);
    }

    public Color lightenColor(Color c) {
        return new Color(c.r * LIGHT_FACTOR, c.g * LIGHT_FACTOR, c.b * LIGHT_FACTOR, LIGHT_ALPHA);
    }
}
