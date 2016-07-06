package org.kryptonmlt.networkdemonstrator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
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
        int max_stations = Integer.parseInt(args[1]);  //max=36
        int[] k = {1, 3, 5, 7, 10, 15, 20, 30};
        float[] row = {0.05f}; // ART - only used when k is null

        int errorMultiplier = 10;
        int delayMillis = 0;
        String dataFileName = "NormalizedData.xlsx";
        String queryFileName = "QueryData.xlsx";
        int startFeature = 1;
        int numberOfFeatures = 3;
        int learnLimit = 1000;
        float alpha = 0.05f;
        float clusteringAlpha = 0.005f;
        WorthType type = WorthType.THETA;
        boolean useStats = false;
        int use_max_points = 1000;
        double samplingRate = 0.1;
        int[] closestK = {1, 3, 30, 50, 100, 300, 500};

        int numberOfClusters;
        if (k == null) {
            numberOfClusters = row.length;
        } else {
            numberOfClusters = k.length;
        }
        // Initialize Central Node
        CentralNode centralNode = new CentralNodeImpl(numberOfFeatures, closestK, numberOfClusters, MastersScenario.COLUMN_NAMES, alpha, false);

        // Initialize IOT Devices (Sensors)
        FileInputStream file = new FileInputStream(new File(dataFileName));
        XSSFWorkbook workbook = new XSSFWorkbook(file);

        File queryFile = new File(queryFileName);
        XSSFWorkbook queryWorkbook;
        if (queryFile.exists()) {
            queryWorkbook = new XSSFWorkbook(new FileInputStream(queryFile));
        } else {
            queryWorkbook = new XSSFWorkbook();
            Iterator<XSSFSheet> queryIter = workbook.iterator();
            while (queryIter.hasNext()) {
                queryWorkbook.createSheet(queryIter.next().getSheetName());
            }
        }

        final List<LeafNode> leafNodes = new ArrayList<>();
        for (int i = 0; i < max_stations; i++) {
            final XSSFSheet sheet = workbook.getSheetAt(i);
            final XSSFSheet querySheet = queryWorkbook.getSheet(sheet.getSheetName());
            leafNodes.add(new LeafNodeImpl((CentralNodeImpl) centralNode, delayMillis, dataFileName, i, sheet, querySheet, startFeature,
                    numberOfFeatures, alpha, clusteringAlpha, learnLimit, type, error, k, row, useStats, use_max_points, samplingRate, closestK.length, errorMultiplier));
        }
        file.close();
        if (!queryFile.exists()) {
            FileOutputStream fos = new FileOutputStream(queryFile);
            queryWorkbook.write(fos);
            fos.flush();
            fos.close();
        }

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
