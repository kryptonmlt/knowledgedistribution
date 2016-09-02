package org.kryptonmlt.automatedvisualizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jzy3d.maths.Coord3d;
import org.kryptonmlt.automatedvisualizer.plots.Plot2D;
import org.kryptonmlt.automatedvisualizer.pojos.EnsembleModelStat;
import org.kryptonmlt.automatedvisualizer.pojos.UpdateModelStat;
import org.kryptonmlt.automatedvisualizer.utils.ColorUtils;

/**
 *
 * @author Kurt
 */
public class EnergyCostCalculator {

    public static int TRANSMISSION_COST = 720;

    public static int RECEIVING_COST = 110;

    public static int INSTRUCTION_COST = 4;

    /**
     * Reads stats.dat file, format: Simple Model Messages - Update Model
     * Theta,messages,error ..,..,.. Theta,messages,error - Ensemble Learning
     * theta gamma,messages (3 values),messages (12 values), error ..,..,..
     * gamma,messages (3 values),messages (12 values), error .., ..,..,..
     * ..,..,..
     *
     * @param args fileName
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws Exception {

        List<UpdateModelStat> updateModelStats = new ArrayList<>();
        Map<Double, List<EnsembleModelStat>> ensembleModelStats = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        String simpleModel = br.readLine();
        if (!"Simple Model".equals(simpleModel)) {
            throw new Exception("Simple Model requested");
        }
        String temp = br.readLine();
        int simpleModelMessages = Integer.parseInt(temp.split(",")[0]);
        double simpleModelError = Double.parseDouble(temp.split(",")[1]);
        br.readLine(); //-
        String updateModel = br.readLine();
        if (!"Update Model".equals(updateModel)) {
            throw new Exception("Update Model requested");
        }
        while ((temp = br.readLine()) != null && !temp.equals("-")) {
            String[] line = temp.split(",");
            updateModelStats.add(new UpdateModelStat(Double.parseDouble(line[0]), Integer.parseInt(line[1]), Double.parseDouble(line[2])));
        }
        String ensembleModel = br.readLine();
        if (!"Ensemble Learning".equals(ensembleModel)) {
            throw new Exception("Ensemble Learning requested");
        }
        double currentTheta = 0.0;
        while ((temp = br.readLine()) != null) {
            String[] line = temp.split(",");
            if (line.length == 1) {
                currentTheta = Double.parseDouble(line[0]);
                ensembleModelStats.put(currentTheta, new ArrayList<>());
                line = br.readLine().split(",");
            }
            ensembleModelStats.get(currentTheta)
                    .add(new EnsembleModelStat(Double.parseDouble(line[0]), Integer.parseInt(line[1]), Integer.parseInt(line[2]), Double.parseDouble(line[3]), currentTheta));
        }
        br.close();
        Collections.sort(updateModelStats);
        BufferedWriter bw = new BufferedWriter(new FileWriter("ThesisOutcome.stat"));
        bw.write("Simple Model\n-\n");
        List<Coord3d> simpleModelCoords = new ArrayList<>();
        List<Coord3d> simpleEnsembleModelCoords = new ArrayList<>();
        bw.write(simpleModelError + "," + simpleModelComputation(simpleModelMessages) + "\n");
        simpleEnsembleModelCoords.add(new Coord3d(simpleModelError, simpleModelComputation(simpleModelMessages)/1000000000.0, 0.0));
        bw.write("Network Update Model\n");
        List<Coord3d> updateModelCoords = new ArrayList<>();
        for (UpdateModelStat model : updateModelStats) {
            bw.write(model.getError() + "," + updateModelComputation(model.getMessages(), simpleModelMessages) + "\n");
            updateModelCoords.add(new Coord3d(model.getError(), updateModelComputation(model.getMessages(), simpleModelMessages)/1000000000.0, 0.0));
            simpleModelCoords.add(new Coord3d(model.getError(), simpleModelComputation(simpleModelMessages)/1000000000.0, 0.0));
        }
        bw.write("Ensemble Model\n-\n");
        Plot2D ensemblePlot = new Plot2D("Ensemble Model (K=3, KNN=10) vs Baseline", "Ensemble Model (K=3, KNN=10) vs Baseline", "Accuracy Error", "Network Overhead / J", false);
        List<EnsembleModelStat> tempList = new ArrayList<>();
        for (Double theta : ensembleModelStats.keySet()) {
            tempList.addAll(ensembleModelStats.get(theta));
            for (EnsembleModelStat model : ensembleModelStats.get(theta)) {
                List<Coord3d> ensembleModelCoords = new ArrayList<>();
                ensembleModelCoords.add(new Coord3d(model.getError(), ensembleModelComputation(model.getMessages3(), model.getMessages12(), simpleModelMessages)/1000000000.0, 0.0));
                ensemblePlot.addSeries(ensembleModelCoords, "THETA:" + model.getTheta() + ",GAMMA:" + model.getGamma(), ColorUtils.convertColor(ColorUtils.getInstance().getNextDarkColor()), true, true);
            }
        }
        ensemblePlot.addSeries(simpleEnsembleModelCoords, "Base Line", ColorUtils.convertColor(ColorUtils.getInstance().getNextDarkColor()), true, true);
        ensemblePlot.display();

        Collections.sort(tempList);
        for (EnsembleModelStat model : tempList) {
            bw.write(model.getError() + "," + ensembleModelComputation(model.getMessages3(), model.getMessages12(), simpleModelMessages) + "," + model.getTheta() + "," + model.getGamma() + "\n");
        }
        bw.flush();
        bw.close();

        //Display Plots
        Plot2D plot = new Plot2D("Network Update Model vs Baseline", "Network Update Model vs Baseline", "Discrepancy Error", "Network Overhead / J", false);
        plot.addSeries(updateModelCoords, "Network Update Model", java.awt.Color.RED, false, false);
        plot.addSeries(simpleModelCoords, "Base Line", java.awt.Color.BLUE, false, false);
        plot.display();
    }

    public static int simpleModelComputation(int messages) {
        return sensor3Calulcation(messages) + concentrator3Calulcation(messages);
    }

    public static int updateModelComputation(int messages, int totalMessageCount) {
        return sensor3Calulcation(messages) + concentrator3Calulcation(messages) + instructionCalulcation(totalMessageCount, 5);
    }

    public static int ensembleModelComputation(int messages3, int messages12, int totalMessageCount) {
        return sensor3Calulcation(messages3) + concentrator3Calulcation(messages3) + sensor12Calulcation(messages12)
                + concentrator12Calulcation(messages12) + instructionCalulcation(totalMessageCount, 9);
    }

    public static int sensor3Calulcation(int messages) {
        return messages * TRANSMISSION_COST;
    }

    public static int concentrator3Calulcation(int messages) {
        return messages * RECEIVING_COST;
    }

    public static int sensor12Calulcation(int messages) {
        return messages * TRANSMISSION_COST * 4;
    }

    public static int concentrator12Calulcation(int messages) {
        return messages * RECEIVING_COST * 4;
    }

    public static int instructionCalulcation(int messages, int instructions) {
        return messages * INSTRUCTION_COST * instructions;
    }

}
