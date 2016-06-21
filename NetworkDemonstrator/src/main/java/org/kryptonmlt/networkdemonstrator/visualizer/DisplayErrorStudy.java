package org.kryptonmlt.networkdemonstrator.visualizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;

/**
 *
 * @author Kurt
 */
public class DisplayErrorStudy {

    private final static String DIFFERENCE_ERROR = "Difference Error";
    private final static String MESSAGES_SENT = "Messages Sent %";
    private final static String THETA = "Theta";
    private final static String[] MESSAGES_ERROR = {MESSAGES_SENT, DIFFERENCE_ERROR, ""};
    private final static String[] THETA_MESSAGES = {THETA, "", MESSAGES_SENT};
    private final static String[] THETA_ERROR = {THETA, "", DIFFERENCE_ERROR};

    public static void main(String args[]) throws Exception, FileNotFoundException {
        //String folderPath = args[0];
        //String worth_type = args[1];
        //int validPeers = Integer.parseInt(args[2]);
        String folderPath = "C:\\Users\\Kurt\\Dropbox\\KurtInGlasgow\\knowledgedistribution\\NetworkDemonstrator\\AUTOMATED_ERRORS_STUDY";
        String worth_type = "THETA";
        int validPeers = 36;

        List<Coord3d> messagesErrorInfo = new ArrayList<>();
        List<Coord3d> thetaMessagesInfo = new ArrayList<>();
        List<Coord3d> thetaErrorInfo = new ArrayList<>();
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        for (int i = 0; i < files.length; i++) {
            BufferedReader br = new BufferedReader(new FileReader(files[i].getPath()));
            int peersCount = Integer.parseInt(br.readLine());
            float t = Float.parseFloat(br.readLine());
            String w = br.readLine();
            if (peersCount == validPeers && worth_type.equals(w)) {
                System.out.println("File " + files[i].getName() + " matches criteria");
                float dd = Float.parseFloat(br.readLine());
                float messagesSentPercent = Float.parseFloat(br.readLine());
                messagesErrorInfo.add(new Coord3d(messagesSentPercent, dd, 0f));
                thetaMessagesInfo.add(new Coord3d(t, 0f, messagesSentPercent));
                thetaErrorInfo.add(new Coord3d(t, 0f, dd));
            } else {
                System.out.println("File " + files[i].getName() + " does not match criteria");
            }
        }
        showGraph(messagesErrorInfo, MESSAGES_ERROR);
        showGraph(thetaMessagesInfo, THETA_MESSAGES);
        showGraph(thetaErrorInfo, THETA_ERROR);
    }

    public static void showGraph(List<Coord3d> points, String[] names) throws Exception {
        System.out.println("Displaying graph " + Arrays.toString(names));
        ScatterPlot3D plot = new ScatterPlot3D(names, false);
        plot.show();
        Coord3d[] tempC = new Coord3d[points.size()];
        Color[] colors = new Color[points.size()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.BLUE;
        }
        tempC = points.toArray(tempC);
        plot.setPoints(tempC, colors);
        plot.updateLine(tempC, Color.BLUE, 0);
    }
}
