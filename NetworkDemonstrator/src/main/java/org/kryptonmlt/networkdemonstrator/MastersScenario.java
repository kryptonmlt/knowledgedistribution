package org.kryptonmlt.networkdemonstrator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
import org.kryptonmlt.networkdemonstrator.evaluation.QueryPerformer;
import org.kryptonmlt.networkdemonstrator.node.CentralNode;
import org.kryptonmlt.networkdemonstrator.node.LeafNode;
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
        String hostname = "127.0.0.1";
        int serverPort = 12345;
        int delayMillis = 300;
        String datafile = "NormalizedData.xlsx";
        int startFeature = 1;
        int numberOfFeatures = 3;
        int maxLearnPoints = 1000;
        double error = 0.05;
        double alpha = 0.05;
        WorthType type = WorthType.THETA;
        Integer k = 4;
        double row = 0.05; // only used when k is null
        int max_stations = 1;

        // Initialize Central Node
        CentralNode centralNode = new CentralNode(serverPort, numberOfFeatures, k, MastersScenario.COLUMN_NAMES);
        new Thread(centralNode).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            LOGGER.error("Error when waiting for central node to start..", ex);
        }

        // Initialize IOT Devices (Sensors)
        FileInputStream file = new FileInputStream(new File(datafile));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        final List<LeafNode> leafNodes = new ArrayList<>();
        for (int i = 0; i < max_stations; i++) {
            final XSSFSheet sheet = workbook.getSheetAt(i);
            leafNodes.add(new LeafNode(hostname, serverPort,
                    delayMillis, datafile, sheet, startFeature,
                    numberOfFeatures, alpha, maxLearnPoints, type, error, k, row));
        }
        file.close();

        // Start communication with central node      
        for (int i = 0; i < leafNodes.size(); i++) {
            new Thread(leafNodes.get(i)).start();
        }

        // Perform Queries
        new Thread(new QueryPerformer(centralNode, leafNodes)).start();
    }
}
