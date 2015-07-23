/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import clear.dep.DepTree;
import clear.reader.SRLReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.apache.commons.lang3.ArrayUtils;
import qa.ProcessFrame;
import qa.dep.DependencyTree;

/**
 *
 * @author samuellouvan
 */
public class ClearParserUtil {

    public static String[] TRAIN_ARGS = {"-c", GlobalV.PROJECT_DIR + "/config/config_srl_en.xml",
        "-i", "", //dirName + ds_ClearParser,
        "-t", GlobalV.PROJECT_DIR + "/config/feature_srl_en_conll09.xml",
        "-m", ""}; //dirName + ds_model

    public static String[] PREDICT_ARGS = {"-c", GlobalV.PROJECT_DIR + "/config/config_srl_en.xml",
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

    public static String[] fromConLL2006to2009(String[] conll2006Str) {
        String conll09Str[] = new String[20];
        Arrays.fill(conll09Str, new String("_"));

        conll09Str[0] = conll2006Str[0];
        conll09Str[1] = conll2006Str[1];
        conll09Str[2] = conll2006Str[2];
        conll09Str[3] = conll2006Str[2];
        conll09Str[4] = conll2006Str[3];
        conll09Str[5] = conll2006Str[3];
        conll09Str[6] = "_";
        conll09Str[7] = "_";
        conll09Str[8] = conll2006Str[5];
        conll09Str[9] = conll2006Str[5];
        conll09Str[10] = conll2006Str[6];
        conll09Str[11] = conll2006Str[6];

        return conll09Str;
    }

    public static String toCONLL2009Format(DependencyTree tree, ProcessFrame procFrame) {
        String conll2006 = toClearParserFormat(tree, procFrame);
        String conll2009 = "";

        // Collect indexes of the trigger then sort
        String[] conll2006Rows = conll2006.split("\n");
        int[] triggerIdxs = IntStream.range(0, conll2006Rows.length)
                .filter(i -> conll2006Rows[i].contains(".01"))
                .map(i -> i + 1)
                .sorted()
                .toArray();

        int[] undergoerIdx = IntStream.range(0, conll2006Rows.length)
                .filter(i -> conll2006Rows[i].contains("A0"))
                .map(i -> i + 1)
                .sorted()
                .toArray();
        int[] enablerIdx = IntStream.range(0, conll2006Rows.length)
                .filter(i -> conll2006Rows[i].contains("A1"))
                .map(i -> i + 1)
                .sorted()
                .toArray();
        int[] resultIdx = IntStream.range(0, conll2006Rows.length)
                .filter(i -> conll2006Rows[i].contains("A2"))
                .map(i -> i + 1)
                .sorted()
                .toArray();
        int triggerIdx = -10; // DUMMY
        if (triggerIdxs.length > 0) {
            triggerIdx = triggerIdxs[0];
        }

        StringBuilder conll2009Rows = new StringBuilder();
        for (int i = 0; i < conll2006Rows.length; i++) {
            String[] conll2009Row = new String[20];
            conll2009Row = fromConLL2006to2009(conll2006Rows[i].split("\t"));
            // for each map and duplicate several fields
            if (i == triggerIdx - 1) {
                conll2009Row[12] = "Y";
                conll2009Row[13] = conll2009Row[2] + ".01";
            }
            if (ArrayUtils.contains(undergoerIdx, i + 1)) {
                conll2009Row[14] = "A0";
            }
            if (ArrayUtils.contains(enablerIdx, i + 1)) {
                conll2009Row[14] = "A1";
            }
            if (ArrayUtils.contains(resultIdx, i + 1)) {
                conll2009Row[14] = "A2";
            }
            conll2009Rows.append(String.join("\t", conll2009Row));
            conll2009Rows.append("\n");
        }

        // if trigger idx : Set Y for fillPRED 
        // update undergoer enabler result
        return conll2009Rows.toString();
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
            boolean triggerDone = false;
            ArrayList<Integer> updatedTriggerIdx = new ArrayList<Integer>();
            for (Integer id : triggerIdx) {
                String field[] = conLLRows[id - 1].split("\\s+");
                if (field[3].startsWith("VB")) {
                    field[7] = field[2] + ".01";
                    // update 
                    conLLRows[id - 1] = String.join("\t", field);
                    triggerDone = true;
                    updatedTriggerIdx.add(id);
                    break;
                }
            }
            if (!triggerDone) {
                for (Integer id : triggerIdx) {
                    String field[] = conLLRows[id - 1].split("\\s+");
                    if (field[3].startsWith("NN")) {
                        field[7] = field[2] + ".01";
                        // update 
                        conLLRows[id - 1] = String.join("\t", field);
                        triggerDone = true;
                        updatedTriggerIdx.add(id);
                        break;
                    }
                }
            }

            if (!triggerDone) {
                for (Integer id : triggerIdx) {
                    String field[] = conLLRows[id - 1].split("\\s+");
                    if (StringUtil.contains(field[1], procFrame.getProcessName().split("\\s+"))) {
                        field[7] = field[2] + ".01";
                        // update 
                        conLLRows[id - 1] = String.join("\t", field);
                        triggerDone = true;
                        updatedTriggerIdx.add(id);
                        break;
                    }
                }
            }

            if (!triggerDone) {
                for (Integer id : triggerIdx) {
                    String field[] = conLLRows[id - 1].split("\\s+");
                    field[7] = field[2] + ".01";
                    // update 
                    conLLRows[id - 1] = String.join("\t", field);
                    updatedTriggerIdx.add(id);
                    break;
                }
            }
            triggerIdx = updatedTriggerIdx;
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

    public static void clearParserToTsv(String inFileName, String outFileName) throws FileNotFoundException {
        SRLReader srlReader = new SRLReader(inFileName, true);
        ArrayList<DepTree> trees = new ArrayList<DepTree>();
        DepTree currentTree = null;
        while ((currentTree = srlReader.nextTree()) != null) {
            trees.add(currentTree);
        }

        ArrayList<ProcessFrame> frames = new ArrayList<ProcessFrame>();
        for (int i = 0; i < trees.size(); i++) {
            DepTree tree = trees.get(i);

            ArrayList<String> underGoers = tree.getRoleFillers("A0");
            ArrayList<String> enablers = tree.getRoleFillers("A1");
            ArrayList<String> results = tree.getRoleFillers("A2");
            ArrayList<String> triggers = tree.getAllPredicateForm();
            ProcessFrame frame = new ProcessFrame();
            frame.setUnderGoer(String.join(" ", underGoers));
            frame.setEnabler(String.join(" ", enablers));
            frame.setResult(String.join(" ", results));
            frame.setTrigger(String.join(" ", triggers));
            frame.setUnderSpecified("");
            frame.setRawText(tree.getRawText());
            frame.setProcessName("");
            frames.add(frame);
        }

        ProcessFrameUtil.dumpFramesToFile(frames, outFileName);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        //System.out.println(System.getProperty("java.class.path"));
        clearParserPredict("/Users/samuellouvan/Downloads/clearparser-read-only/data/evaporate_evaporation.jointmodel.0",
                "/Users/samuellouvan/Downloads/clearparser-read-only/data/temp.clearparser",
                "test.out");
    }

}
