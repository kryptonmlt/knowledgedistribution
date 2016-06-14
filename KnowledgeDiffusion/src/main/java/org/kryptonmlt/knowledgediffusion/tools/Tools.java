package org.kryptonmlt.knowledgediffusion.tools;

/**
 *
 * @author Kurt
 */
public class Tools {

    private Tools() {

    }

    public static String[] properSplit(String data, String delimeter) {
        String values[] = new String[data.length() - data.replace(delimeter, "").length() + 1];
        int nextInterval = 0;
        for (int i = 0; i < values.length; i++) {
            if (i < values.length - 1) {
                nextInterval = data.indexOf(",");
                values[i] = data.substring(0, nextInterval);
                data = data.substring(nextInterval + 1);
            } else {
                values[i] = data.substring(0);
            }
        }
        return values;
    }

    public static void setAllStringArray(String[] a, String value) {
        for (int i = 0; i < a.length; i++) {
            a[i] = value;
        }
    }
}
