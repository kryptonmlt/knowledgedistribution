package org.kryptonmlt.networkdemonstrator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
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

    public static void main(String[] args) throws SocketException, IOException {
        String hostname = "127.0.0.1";
        int serverPort = 12345;
        int delayMillis = 300;
        String datafile = "NormalizedData.xlsx";
        //String datafile = "Data.xlsx";
        int startFeature = 0;
        int numberOfFeatures = 3;
        int maxLearnPoints = 1000;
        double error = 0.01;
        double alpha = 0.05;
        WorthType type = WorthType.THETA;
        Integer k = 4;
        double row = 0.05; // only used when k is null

        // Initialize Central Node
        CentralNode centralNode = new CentralNode(serverPort, numberOfFeatures, k);
        new Thread(centralNode).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            LOGGER.error("Error when waiting for central node to start..", ex);
        }

        // Initialize IOT Devices (Sensors)
        FileInputStream file = new FileInputStream(new File(datafile));
        XSSFWorkbook workbook = new XSSFWorkbook(file);

        for (int i = 0; i < 10; i++) {
            final XSSFSheet sheet = workbook.getSheetAt(i);
            final int temp = i;
            new Thread() {
                @Override
                public void run() {
                    this.setName("Sheet"+temp);
                    try {
                        LeafNode device = new LeafNode(hostname, serverPort,
                                delayMillis, datafile, sheet, startFeature,
                                numberOfFeatures, alpha, maxLearnPoints, type, error, k, row);
                        device.initializeCommunication();
                    } catch (IOException ex) {
                        LOGGER.error("Error when initializing communication for Leaf Node with sheet " + temp, ex);
                    }
                }
            }.start();
        }

        // Perform Queries
        for (int i = 0; i < 15; i++) {
            try {
                Thread.sleep(10000l);
            } catch (InterruptedException ex) {
                LOGGER.error("Error when trying to wait for querying..", ex);
            }
            double[] query = {0.0, 0.0};
            double result = centralNode.query(query);
            double resultAll = centralNode.queryAll(query);
            LOGGER.info("Query {} = Quantized Result: {} , Average Result: {} ", i, result, resultAll);
        }
    }
}
