package org.kryptonmlt.networkdemonstrator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kryptonmlt.network.stats.QueryPerformer;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
import org.kryptonmlt.networkdemonstrator.node.impl.ConcentratorImpl;
import org.kryptonmlt.networkdemonstrator.node.impl.SensorImpl;
import org.kryptonmlt.networkdemonstrator.utils.ConversionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kryptonmlt.networkdemonstrator.node.Concentrator;
import org.kryptonmlt.networkdemonstrator.node.Sensor;

/**
 * Reads the networkdemonstrator.properties file to input the properties.
 * Also requires 1 argument which specifies the THETA
 * It starts the simulation of an IoT environment 1 concentrator and x sensors depending on the properties.
 * It runs QueryPerformer to save the statistics.
 * @author Kurt
 */
public class MastersScenario {

    private static final Logger LOGGER = LoggerFactory.getLogger(MastersScenario.class);
    public static String[] COLUMN_NAMES = {"PM25_AQI", "PM10_AQI", "NO2"};

    public static void main(String[] args) throws SocketException, IOException {
        Map<String, String> props = readInputFile(new File("networkdemonstrator.properties"));
        int max_stations = Integer.parseInt(props.get("max_stations"));  //max=36
        double error = Double.parseDouble(args[0]);
        float gamma = Float.parseFloat(props.get("gamma"));
        int[] knn = readArray(props.get("knn"));
        int[] k = readArray(props.get("k"));
        boolean showVisualization = Boolean.parseBoolean(props.get("showVisualization"));
        int startFeature = Integer.parseInt(props.get("startFeature"));
        int numberOfFeatures = Integer.parseInt(props.get("numberOfFeatures"));
        int learnLimit = Integer.parseInt(props.get("learnLimit"));
        int delayMillis = Integer.parseInt(props.get("delayMillis"));
        double samplingRate = Double.parseDouble(props.get("samplingRate"));
        boolean useStats = Boolean.parseBoolean(props.get("useStats"));
        int use_max_points = Integer.parseInt(props.get("use_max_points_stats"));
        String dataFileName = props.get("dataFileName");
        String queryFileName = props.get("queryFileName");
        String featureModelFileName = props.get("featureModelFileName");

        float[] row = {0.05f}; // ART - only used when k is null
        int errorMultiplier = 10;
        float alpha = 0.05f;
        float clusteringAlpha = 0.005f;
        WorthType type = WorthType.THETA;

        int numberOfClusters;
        if (k == null) {
            numberOfClusters = row.length;
        } else {
            numberOfClusters = k.length;
        }
        File featureModelFile = new File(featureModelFileName);
        double[] weights = {0, 0, 0};
        int featuresUsed = 0;
        if (featureModelFile.exists()) {
            LOGGER.info("Feature Model file {} found, using it", featureModelFileName);
            BufferedReader br = new BufferedReader(new FileReader(featureModelFile));
            weights = ConversionUtils.convertStringArrayToDoubleArray(br.readLine().split(","));
            featuresUsed = Integer.parseInt(br.readLine());
        } else {
            LOGGER.info("Feature Model file {} containing weights not found initializing them to 0", featureModelFileName);
        }
        // Initialize Central Node
        Concentrator centralNode = new ConcentratorImpl(numberOfFeatures, knn, numberOfClusters, MastersScenario.COLUMN_NAMES, alpha, showVisualization);
        centralNode.getFeatureModel().setWeights(weights);
        centralNode.setFeaturesReceived(featuresUsed);
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

        final List<Sensor> leafNodes = new ArrayList<>();
        for (int i = 0; i < max_stations; i++) {
            final XSSFSheet sheet = workbook.getSheetAt(i);
            final XSSFSheet querySheet = queryWorkbook.getSheet(sheet.getSheetName());
            leafNodes.add(new SensorImpl((ConcentratorImpl) centralNode, delayMillis, dataFileName, i, sheet, querySheet, startFeature,
                    numberOfFeatures, alpha, clusteringAlpha, learnLimit, type, error, k, row, useStats, use_max_points, samplingRate, knn.length, errorMultiplier, gamma));
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
        Thread t = new Thread(new QueryPerformer(centralNode, leafNodes, error, type, k, row, knn));
        t.setName("QueyPerformer Thread");
        t.start();
    }

    private static Map<String, String> readInputFile(File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        Map<String, String> props = new HashMap<>();
        String temp;
        while ((temp = br.readLine()) != null) {
            String[] p = temp.split("=");
            props.put(p[0].trim(), p[1].trim());
        }
        br.close();
        return props;
    }

    private static int[] readArray(String line) {
        if (line == null) {
            return null;
        }
        String[] items = line.split(",");
        int[] result = new int[items.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Integer.parseInt(items[i]);
        }
        return result;
    }
}
