/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import Util.ClearParserUtil;
import Util.ProcessFrameUtil;
import Util.StdUtil;
import Util.StringUtil;
import clear.engine.SRLPredict;
import clear.engine.SRLTrain;
import clear.util.FileUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.srl.SRLEvaluate;
import qa.srl.SRLWrapper;

/**
 *
 * @author samuellouvan
 */
public class SRLDSCombinedModelCrossValidation {

    ProcessFrameProcessor proc;

    private ArrayList<String> blackList;
    @Option(name = "-f", usage = "process file", required = true, metaVar = "REQUIRED")
    private String processTsvFileName;

    @Option(name = "-o", usage = "output directory name", required = true, metaVar = "REQUIRED")
    private String outDirName;

    @Option(name = "-d", usage = "directory where the ds files located", required = true, metaVar = "REQUIRED")
    private String dsDirName;

    @Option(name = "-srl", usage = "SRL type", required = true, metaVar = "OPTIONAL")
    private int srlType;

    @Option(name = "-df", usage = "ds file name", required = false, metaVar = "OPTIONAL")
    private String dsFileName = "ds_all_processes_w_pattern.tsv";

    @Option(name = "-k", usage = "number of fold", required = true, metaVar = "REQUIRED")
    private int fold;

    @Option(name = "-n", usage = "number of processes to test", required = false, metaVar = "OPTIONAL")
    private int nbProcess = 0;

    @Option(name = "-p", usage = "Process to test", required = false, metaVar = "OPTIONAL")
    private String processToTest = "";

    @Option(name = "-pi", usage = "predicate/trigger identification", required = false, metaVar = "OPTIONAL")
    private boolean pi = false;
    
    boolean limitedProcess = false;
    private ArrayList<ProcessFrame> frameArr;
    private HashMap<String, Integer> processFold;
    private ArrayList<String> processNames;
    ArrayList<String> testFilePath;
    ArrayList<String> trainingModelFilePath;
    /* String[] blackListProcess = {"Salivating", "composted", "decant_decanting", "dripping", "magneticseparation", "loosening", "momentum", "seafloorspreadingtheory", "sedimentation",
     "spear_spearing", "retract", "distillation", "Feelsleepy", "filtering", "revising" "fertilization",
     "freeze_freezing", "germinating_germination", "inferring", "melt_melting", "reusing", "takeinnutrients_takinginnutrients", "sight",
     "upwelling", "write", "work", "vibrates_vibration_vibrations", "warming", "watercycle_thewatercycle", "weather_weathering", "whiten_becomewhiter", "windbreaking"};*/
    String[] blackListProcess = {"Salivating", "composted", "decant_decanting", "dripping", "magneticseparation", "loosening", "momentum", "seafloorspreadingtheory", "sedimentation",
        "spear_spearing", "retract"};

    public SRLDSCombinedModelCrossValidation() throws FileNotFoundException {
        trainingModelFilePath = new ArrayList<String>();
        testFilePath = new ArrayList<String>();
        processFold = new HashMap<String, Integer>();
        processNames = new ArrayList<String>();
        blackList = new ArrayList<String>();
        frameArr = new ArrayList<ProcessFrame>();

    }

    public void init() throws FileNotFoundException, IOException, ClassNotFoundException {
        proc = new ProcessFrameProcessor(processTsvFileName);
        proc.loadProcessData();
        blackList = new ArrayList(Arrays.asList(blackListProcess));
        if (nbProcess > 0) {
            limitedProcess = true;
            for (int i = 0; i < proc.getProcArr().size() && processNames.size() < nbProcess; i++) {
                String normProcessName = ProcessFrameUtil.normalizeProcessName(proc.getProcArr().get(i).getProcessName());
                if (!processNames.contains(normProcessName)) {
                    processNames.add(normProcessName);
                }
            }
        }
        if (processToTest.isEmpty()) {
            frameArr = proc.getProcArr();
            for (int i = 0; i < frameArr.size(); i++) {
                String normName = ProcessFrameUtil.normalizeProcessName(frameArr.get(i).getProcessName());
                if (!processNames.contains(normName)) {
                    processNames.add(normName);
                }
                processFold.put(normName, 0);
            }
        } else {
            for (int i = 0; i < proc.getProcArr().size(); i++) {
                String normName = ProcessFrameUtil.normalizeProcessName(proc.getProcArr().get(i).getProcessName());
                String[] normNames = normName.split("_");
                if (StringUtil.contains(processToTest, normNames)) {
                    frameArr.add(proc.getProcArr().get(i));
                    if (!processNames.contains(normName)) {
                        processNames.add(normName);
                    }
                    processFold.put(normName, 0);
                }
            }
            if (processFold.size() == 0) {
                System.out.println("Cannot find the process to test!");
                System.exit(0);
            }
        }
        File outDirHandler = new File(outDirName);
        if (outDirHandler.exists()) {
            return;
        }
        boolean success = outDirHandler.mkdir();

        if (!success) {
            System.out.println("FAILED to create output directory");
            System.exit(0);
        }
    }

    public void doTrain(String trainingFileName, String modelFileName) throws IOException, FileNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
       new SRLWrapper().doTrain(trainingFileName, modelFileName, srlType);
    }

    public void doPredict() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        for (int i = 0; i < testFilePath.size(); i++) {
            new SRLWrapper().doPredict(testFilePath.get(i), testFilePath.get(i).replace("test.", "dscombined.predict."), trainingModelFilePath.get(i), srlType,pi);
        }
    }

    public void trainAndPredict() throws FileNotFoundException, IOException, InterruptedException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        testFilePath.clear();
        trainingModelFilePath.clear();
        int startIdx = 0;
        int testSize = frameArr.size() / fold;
        int endIdx = startIdx + testSize;
        Collections.shuffle(frameArr);
        ProcessFrameProcessor dsProc = new ProcessFrameProcessor(dsDirName + "/" + dsFileName);
        dsProc.loadProcessData();
        for (int currentFold = 0; currentFold < fold; currentFold++) {
            ArrayList<ProcessFrame> testingFrames = new ArrayList<ProcessFrame>(frameArr.subList(startIdx, endIdx));
            ArrayList<ProcessFrame> trainingFrames = new ArrayList<ProcessFrame>(frameArr.subList(0, startIdx));
            trainingFrames.addAll(new ArrayList<ProcessFrame>(frameArr.subList(endIdx, frameArr.size())));
            trainingFrames.addAll(dsProc.getProcArr());

            String trainingFileName = outDirName + "/train.dscombined.cv." + currentFold;
            String testingFileName = outDirName + "/test.cv." + currentFold;
            String modelName = outDirName + "/dscombinedmodel.cv." + currentFold;

            testFilePath.add(testingFileName);
            trainingModelFilePath.add(modelName);

            ProcessFrameUtil.toParserFormat(trainingFrames, trainingFileName, srlType);
            ProcessFrameUtil.toParserFormat(testingFrames, testingFileName, srlType);

            doTrain(trainingFileName, modelName);
            startIdx = endIdx;
            if (currentFold == fold - 2) {
                endIdx = frameArr.size();
            } else {
                endIdx = startIdx + testSize;
            }
        }

        doPredict();
    }

    /**
     * Compute the precision, recall, F1 of the predictions by executing
     * combine.py and evaluate.py
     */
    public void evaluate() throws FileNotFoundException, IOException {
        new SRLEvaluate().evaluate(testFilePath, "test.", "dscombined.predict.", srlType);
    }

    public static void main(String[] args) throws FileNotFoundException {
        SRLDSCombinedModelCrossValidation srlExp = new SRLDSCombinedModelCrossValidation();
        CmdLineParser cmd = new CmdLineParser(srlExp);

        try {
            cmd.parseArgument(args);
            srlExp.init();
            srlExp.trainAndPredict();
            Thread.sleep(5000);
            srlExp.evaluate();
            System.out.println("FINISH");
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            cmd.printUsage(System.err);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
