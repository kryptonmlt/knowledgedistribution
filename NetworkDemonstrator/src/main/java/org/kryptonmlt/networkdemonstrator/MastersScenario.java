package org.kryptonmlt.networkdemonstrator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kryptonmlt.network.stats.QueryPerformer;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
import org.kryptonmlt.networkdemonstrator.node.CentralNode;
import org.kryptonmlt.networkdemonstrator.node.LeafNode;
import org.kryptonmlt.networkdemonstrator.node.mock.CentralNodeImpl;
import org.kryptonmlt.networkdemonstrator.node.mock.LeafNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class MastersScenario {

    private static final Logger LOGGER = LoggerFactory.getLogger(MastersScenario.class);
    public static String[] COLUMN_NAMES = {"PM25_AQI", "PM10_AQI", "NO2"};

    public static void main(String[] args) throws SocketException, IOException {

        double error = Double.parseDouble(args[0]);
        String clusterParameter = args[1];
        int max_stations = Integer.parseInt(args[2]);  //max=36
        Integer k = null;
        float row = 0.05f; // ART - only used when k is null
        if (clusterParameter.contains(".")) {
            row = Float.parseFloat(clusterParameter);
        } else {
            k = Integer.parseInt(clusterParameter);
        }

        int errorMultiplier = 10;
        int delayMillis = 0;
        String datafile = "NormalizedData.xlsx";
        int startFeature = 1;
        int numberOfFeatures = 3;
        int learnLimit = 1000;
        float alpha = 0.05f;
        float clusteringAlpha = 0.005f;
        WorthType type = WorthType.THETA;
        boolean useStats = false;
        int use_max_points = 1000;
        double samplingRate = 0.1;
        int[] closestK = {1, 3, 30, 50};

        // Initialize Central Node
        CentralNode centralNode = new CentralNodeImpl(numberOfFeatures, closestK, MastersScenario.COLUMN_NAMES, false);

        // Initialize IOT Devices (Sensors)
        FileInputStream file = new FileInputStream(new File(datafile));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        final List<LeafNode> leafNodes = new ArrayList<>();
        for (int i = 0; i < max_stations; i++) {
            final XSSFSheet sheet = workbook.getSheetAt(i);
            leafNodes.add(new LeafNodeImpl((CentralNodeImpl) centralNode, delayMillis, datafile, i, sheet, startFeature,
                    numberOfFeatures, alpha, clusteringAlpha, learnLimit, type, error, k, row, useStats, use_max_points, samplingRate, closestK.length, errorMultiplier));
        }
        file.close();

        // Start communication with central node      
        for (int i = 0; i < leafNodes.size(); i++) {
            Thread t = new Thread((Runnable) leafNodes.get(i));
            t.setName("Sheet " + i);
            t.start();
        }

        // Perform Queries
        Thread t = new Thread(new QueryPerformer(centralNode, leafNodes, error, type, k, row, closestK));
        t.setName("QueyPerformer Thread");
        t.start();
    }
}
