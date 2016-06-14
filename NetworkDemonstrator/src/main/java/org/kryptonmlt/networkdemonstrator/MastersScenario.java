package org.kryptonmlt.networkdemonstrator;

import java.io.IOException;
import java.net.SocketException;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
import org.kryptonmlt.networkdemonstrator.node.CentralNode;
import org.kryptonmlt.networkdemonstrator.node.LeafNode;

/**
 *
 * @author Kurt
 */
public class MastersScenario {

    public static void main(String[] args) throws SocketException, IOException {
        String hostname = "127.0.0.1";
        int serverPort = 12345;
        int delayMillis = 1000;
        String datafile = "NormalizedData.xlsx";
        int startFeature = 0;
        int numberOfFeatures = 3;
        int maxLearnPoints = 1000;
        int error = 1;
        double alpha = 0.05;
        WorthType type = WorthType.ALL;
        Integer k = 4;
        double row = 0.05; // only used when k is null

        //initialize central node
        int closestK = 3;
        CentralNode centralNode = new CentralNode(serverPort, numberOfFeatures, closestK);
        new Thread(centralNode).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        //initialize iot devices (sensors)
        for (int i = 0; i < 30; i++) {
            final int sheet = i;
            new Thread() {
                @Override
                public void run() {
                    try {
                        LeafNode device = new LeafNode(hostname, serverPort,
                                delayMillis, datafile, sheet, startFeature,
                                numberOfFeatures, alpha, maxLearnPoints, type, error, k, row);
                        device.initializeCommunication(false);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }.start();
        }
    }
}
