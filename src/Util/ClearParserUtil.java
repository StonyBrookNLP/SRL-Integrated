/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.io.IOException;
import java.util.ArrayList;
import qa.ProcessFrame;
import qa.dep.DependencyTree;

/**
 *
 * @author samuellouvan
 */
public class ClearParserUtil {

    public static String[] TRAIN_ARGS = {"-c", "/Users/samuellouvan/NetBeansProjects/QA/config/config_srl_en.xml",
        "-i", "", //dirName + ds_ClearParser,
        "-t", "/Users/samuellouvan/NetBeansProjects/QA/config/feature_srl_en_conll09.xml",
        "-m", ""}; //dirName + ds_model

    public static String[] PREDICT_ARGS = {"-c", "/Users/samuellouvan/NetBeansProjects/QA/config/config_srl_en.xml",
        "-i", "", //  testDirName + testingFile
        "-o", "", // testDirName + predictionFile
        "-m", ""}; // modelDirName + ds_model}

    public static void clearParserTrain(String modelName, String trainingFileName) throws IOException, InterruptedException {
        String[] cmdarray = new String[]{
            "java", "-classpath", System.getProperty("java.class.path"), "clear.engine.SRLTrain", "-c", "clearparser-lib/config/config_srl_en.xml",
            "-t", "clearparser-lib/config/feature_srl_en_conll09.xml", "-m", modelName, "-i", trainingFileName};

        System.out.println(System.getProperty("java.class.path"));
        Process trainProcess = Runtime.getRuntime().exec(cmdarray);
        StdUtil.printOutput(trainProcess);
        StdUtil.printError(trainProcess);
        trainProcess.waitFor();
    }

    public static double clearParserPredict(String modelName, String testingFileName, String predictionFileName) throws IOException, InterruptedException {
        String[] cmdarray = new String[]{
            "java", "-classpath", System.getProperty("java.class.path"), "clear.engine.SRLPredict", "-c", "clearparser-lib/config/config_srl_en.xml", "-m", modelName, "-i", testingFileName, "-o", predictionFileName};
        Process predictProcess = Runtime.getRuntime().exec(cmdarray);
        double score = 0;
        //double score = StdUtil.getPredictionConfidenceScore(predictProcess);
        //StdUtil.getRawOutput(predictProcess);
        StdUtil.printError(predictProcess);
        predictProcess.waitFor();
        return score;
    }

    public static void clearParserPredictOnly(String modelName, String testingFileName, String predictionFileName) throws IOException {
        String[] cmdarray = new String[]{
            "java", "-classpath", System.getProperty("java.class.path"), "clear.engine.SRLPredict", "-c", "clearparser-lib/config/config_srl_en.xml", "-m", modelName, "-i", testingFileName, "-o", predictionFileName};
        Process predictProcess = Runtime.getRuntime().exec(cmdarray);
        StdUtil.printError(predictProcess);
        StdUtil.printOutput(predictProcess);
    }

    public static String clearParserEval(String process, int fold, String testingFileName, String predictionFileName) throws IOException, InterruptedException {
        String[] cmdarray = new String[]{
            "java", "-classpath", System.getProperty("java.class.path"), "clear.engine.SRLEvaluate", "-g", testingFileName, "-s", predictionFileName};
        Process evaluateProcess = Runtime.getRuntime().exec(cmdarray);
        StdUtil.printError(evaluateProcess);
        String results = StdUtil.getOutput(process, fold, evaluateProcess);
        evaluateProcess.waitFor();
        return results;
    }

    public static void clearParserEvalOnly(String testingFileName, String predictionFileName) throws IOException {
        String[] cmdarray = new String[]{
            "java", "-classpath", System.getProperty("java.class.path"), "clear.engine.SRLEvaluate", "-g", testingFileName, "-s", predictionFileName};
        Process evaluateProcess = Runtime.getRuntime().exec(cmdarray);
        StdUtil.printOutput(evaluateProcess);
        StdUtil.printError(evaluateProcess);
    }

    public static void generateClearParserFile(String tsvFileName, String clearParserFileName) {
        // Skip header 
    }

    public static String toClearParserFormat(DependencyTree tree, ProcessFrame procFrame) {
        String results = "";
        // Get the trigger, undergoer, enabler, result idx from procFrame
        procFrame.processRoleFillers();
        ArrayList<Integer> triggerIdx = procFrame.getTriggerIdx();
        ArrayList<Integer> undergoerIdx = procFrame.getUndergoerIdx();
        ArrayList<Integer> enablerIdx = procFrame.getEnablerIdx();
        ArrayList<Integer> resultIdx = procFrame.getResultIdx();

        String conLLRows[] = tree.toString().split("\n");
        if (triggerIdx.size() > 0) {
            // Update trigger
            for (Integer id : triggerIdx) {
                String field[] = conLLRows[id - 1].split("\\s+");
                field[7] = field[2] + ".01";
                // update 
                conLLRows[id - 1] = String.join("\t", field);

            }

            // Update undergoer
            for (Integer id : undergoerIdx) {
                String field[] = conLLRows[id - 1].split("\\s+");
                String undergoerStr = "";

                for (int i = 0; i < triggerIdx.size(); i++) {
                    undergoerStr += triggerIdx.get(i) + ":A0;";
                }
                undergoerStr = undergoerStr.substring(0, undergoerStr.length() - 1);

                field[8] = field[8] + undergoerStr;
                field[8] = field[8].replaceAll("_", "");
                conLLRows[id - 1] = String.join("\t", field);
            }

            // Update  enabler
            for (Integer id : enablerIdx) {
                String field[] = conLLRows[id - 1].split("\\s+");
                String enablerStr = "";

                for (int i = 0; i < triggerIdx.size(); i++) {
                    enablerStr += triggerIdx.get(i) + ":A1;";
                }
                enablerStr = enablerStr.substring(0, enablerStr.length() - 1);
                field[8] = field[8] + enablerStr;
                field[8] = field[8].replaceAll("_", "");
                conLLRows[id - 1] = String.join("\t", field);

            }

            // Update  result
            for (Integer id : resultIdx) {
                String field[] = conLLRows[id - 1].split("\\s+");
                String resultStr = "";

                for (int i = 0; i < triggerIdx.size(); i++) {
                    resultStr += triggerIdx.get(i) + ":A2;";
                }
                resultStr = resultStr.substring(0, resultStr.length() - 1);
                field[8] = field[8] + resultStr;
                field[8] = field[8].replaceAll("_", "");
                conLLRows[id - 1] = String.join("\t", field);

            }
        }
        for (int i = 0; i < conLLRows.length; i++) {
            String fields[] = conLLRows[i].split("\\s+");
            conLLRows[i] = String.join("\t", fields);
        }

        return String.join("\n", conLLRows);
    }

    
    public static void main(String[] args) throws IOException, InterruptedException {
        //System.out.println(System.getProperty("java.class.path"));
        clearParserPredict("/Users/samuellouvan/Downloads/clearparser-read-only/data/evaporate_evaporation.jointmodel.0",
                "/Users/samuellouvan/Downloads/clearparser-read-only/data/temp.clearparser",
                "test.out");
    }

}
