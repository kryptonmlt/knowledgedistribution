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
import org.kryptonmlt.networkdemonstrator.node.mock.CentralNodeMock;
import org.kryptonmlt.networkdemonstrator.node.mock.LeafNodeMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class MastersScenarioMock {

    private static final Logger LOGGER = LoggerFactory.getLogger(MastersScenarioMock.class);
    public static String[] COLUMN_NAMES = {"PM25_AQI", "PM10_AQI", "NO2"};

    public static void main(String[] args) throws SocketException, IOException {
        int delayMillis = 0;
        String datafile = "NormalizedData.xlsx";
        int startFeature = 1;
        int numberOfFeatures = 3;
        int maxLearnPoints = 1000;
        double error = Double.parseDouble(args[0]);
        double alpha = 0.05;
        WorthType type = WorthType.THETA;
        Integer k = 4; // Online K-Means
        double row = 0.05; // ART - only used when k is null
        int max_stations = 36;
        float DEGRADING_ALPHA = 0.01f;
        float MINIMUM_ALPHA = 0;
        int generations = 1;

        // Initialize Central Node
        CentralNode centralNode = new CentralNodeMock(numberOfFeatures, 5, MastersScenarioMock.COLUMN_NAMES, false);

        // Initialize IOT Devices (Sensors)
        FileInputStream file = new FileInputStream(new File(datafile));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        final List<LeafNode> leafNodes = new ArrayList<>();
        for (int i = 0; i < max_stations; i++) {
            final XSSFSheet sheet = workbook.getSheetAt(i);
            leafNodes.add(new LeafNodeMock((CentralNodeMock) centralNode, delayMillis, datafile, i, sheet, startFeature,
                    numberOfFeatures, alpha, maxLearnPoints, type, error, k, row, generations, DEGRADING_ALPHA, MINIMUM_ALPHA));
        }
        file.close();

        // Start communication with central node      
        for (int i = 0; i < leafNodes.size(); i++) {
            Thread t = new Thread((Runnable) leafNodes.get(i));
            t.setName("Sheet " + i);
            t.start();
        }

        // Perform Queries
        Thread t = new Thread(new QueryPerformer(centralNode, leafNodes, error, type));
        t.setName("QueyPerformer Thread");
        t.start();
    }
}
