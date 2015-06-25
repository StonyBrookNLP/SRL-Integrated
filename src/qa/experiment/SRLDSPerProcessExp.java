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
import clear.util.cluster.Prob2dMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;

/**
 *
 * @author samuellouvan
 */
public class SRLDSPerProcessExp {

    ProcessFrameProcessor proc;
    // -f <process file>  -o<outputDir> -d<dsDirectory>
    private ArrayList<String> blackList;
    @Option(name = "-f", usage = "process file", required = true, metaVar = "REQUIRED")
    private String processTsvFileName;

    @Option(name = "-o", usage = "output directory name", required = true, metaVar = "REQUIRED")
    private String outDirName;

    @Option(name = "-d", usage = "directory where the ds files located", required = true, metaVar = "REQUIRED")
    private String dsDirName;

    @Option(name = "-df", usage = "ds file name", required = false, metaVar = "OPTIONAL")
    private String dsFileName = "ds_all_processes_w_pattern.tsv";

    @Option(name = "-n", usage = "number of processes to test", required = false, metaVar = "OPTIONAL")
    private int nbProcess = 0;

    @Option(name = "-p", usage = "specific process to test", required = false, metaVar = "OPTIONAL")
    private String processToTest = "";

    @Option(name = "-t", usage = "number of training data", required = false, metaVar = "OPTIONAL")
    private int nbTrainingData = -1;

    @Option(name = "-mx", usage = "the training data is mixed between annotated data and DS", required = false, metaVar = "OPTIONAL")
    private boolean mixed = false;

    boolean limitedProcess = false;
    private ArrayList<ProcessFrame> frameArr;
    private HashMap<String, Integer> processFold;
    private ArrayList<String> processNames;
    ArrayList<String> testFilePath;
    ArrayList<String> trainingModelFilePath;
    String[] blackListProcess = {"Salivating", "composted", "decant_decanting", "dripping", "magneticseparation", "loosening", "momentum", "seafloorspreadingtheory", "sedimentation",
        "spear_spearing", "retract"};

    public SRLDSPerProcessExp() throws FileNotFoundException {
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
                processFold.put(normName, 0);
            }
        } else {
            for (int i = 0; i < proc.getProcArr().size(); i++) {
                String normName = ProcessFrameUtil.normalizeProcessName(proc.getProcArr().get(i).getProcessName());
                String[] normNames = normName.split("_");
                if (StringUtil.contains(processToTest, normNames)) {
                    frameArr.add(proc.getProcArr().get(i));
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

    public ArrayList<ProcessFrame> getTrainingData(ProcessFrame testFrame, ProcessFrameProcessor dsProc, String normalizedProcessName) {
        ArrayList<ProcessFrame> trainingFrames = dsProc.getProcessFrameByNormalizedName(normalizedProcessName);
        if (trainingFrames == null || trainingFrames.size() == 0) {
            System.out.print("PROBLEM, CANNOT FIND THE TEST PROCESS IN THE DS DATA : ");
            System.out.println(testFrame.getProcessName());
            //System.exit(0);
        } else {
            //System.out.println("FOUND");
        }
        if (nbTrainingData != -1) {
            if (trainingFrames.size() < nbTrainingData) {
                System.out.println("ERROR, training frames available is less than nbTrainingData specified");
                System.exit(0);
            }
            trainingFrames = new ArrayList<ProcessFrame>(trainingFrames.subList(0, nbTrainingData));
        }

        return trainingFrames;
    }

    public void trainAndPredict() throws FileNotFoundException, IOException, InterruptedException, ClassNotFoundException {
        testFilePath.clear();
        trainingModelFilePath.clear();
        ProcessFrameProcessor dsProc = new ProcessFrameProcessor(dsDirName + "/" + dsFileName);
        dsProc.loadProcessData();
        for (int i = 0; i < frameArr.size(); i++) {
            //System.out.println(i);
            ProcessFrame testFrame = frameArr.get(i);
            String normalizedProcessName = ProcessFrameUtil.normalizeProcessName(testFrame.getProcessName());
            if ((!limitedProcess || (limitedProcess && processNames.contains(normalizedProcessName))) && !blackList.contains(normalizedProcessName)) {
                int fold = processFold.get(normalizedProcessName);
                ProcessFrameUtil.toClearParserFormat(testFrame, outDirName + "/" + normalizedProcessName + ".test.cv." + fold);
                testFilePath.add(outDirName + "/" + normalizedProcessName + ".test.cv." + fold);
                processFold.put(normalizedProcessName, fold + 1);

                String trainingFileName = outDirName + "/" + normalizedProcessName + ".train.dsperprocess.cv." + fold;
                trainingModelFilePath.add(outDirName + "/" + normalizedProcessName + ".dsperprocessmodel.cv." + fold);
                String modelName = outDirName + "/" + normalizedProcessName + ".dsperprocessmodel.cv." + fold;

                ArrayList<ProcessFrame> trainingFrames = getTrainingData(testFrame, dsProc, normalizedProcessName);
                if (mixed) {
                    // Get the training data from manually annotatedData
                    for (int j = 0; j < frameArr.size(); j++) {
                        if (i != j) {
                            ProcessFrame frame = frameArr.get(j);
                            String normName = ProcessFrameUtil.normalizeProcessName(testFrame.getProcessName());
                            if (normalizedProcessName.equalsIgnoreCase(normName)) {
                                System.out.println("From annotated data");
                                trainingFrames.add(frame);
                            }
                        }
                    }
                }
                ProcessFrameUtil.toClearParserFormat(trainingFrames, trainingFileName);

                // Train trainingFrames
                SRLTrain train = new SRLTrain();
                CmdLineParser cmd = new CmdLineParser(train);

                try {
                    ClearParserUtil.TRAIN_ARGS[3] = trainingFileName;
                    ClearParserUtil.TRAIN_ARGS[7] = modelName;
                    cmd.parseArgument(ClearParserUtil.TRAIN_ARGS);
                    train.init();
                    train.train();
                } catch (CmdLineException e) {
                    System.err.println(e.getMessage());
                    cmd.printUsage(System.err);
                } catch (Exception e) {
                    e.printStackTrace();
                    // System.exit(0);
                }

            }
            // Perform prediction
        }
        Thread.sleep(10000);
        for (int i = 0; i < testFilePath.size(); i++) {
            ClearParserUtil.PREDICT_ARGS[3] = testFilePath.get(i);
            ClearParserUtil.PREDICT_ARGS[5] = testFilePath.get(i).replace(".test.", ".dsperprocess.predict.");
            ClearParserUtil.PREDICT_ARGS[7] = trainingModelFilePath.get(i);
            new SRLPredict(ClearParserUtil.PREDICT_ARGS);
        }

        // Prediction time
    }

    /**
     * Compute the precision, recall, F1 of the predictions by executing
     * combine.py and evaluate.py
     */
    public void evaluate() throws FileNotFoundException, IOException {
        System.out.println("Evaluating");
        PrintWriter gs_writer = new PrintWriter("gs.txt");
        PrintWriter srl_writer = new PrintWriter("srl.txt");
        for (int i = 0; i < testFilePath.size(); i++) {
            String[] gsTxt = FileUtil.readLinesFromFile(testFilePath.get(i));
            String[] srlTxt = FileUtil.readLinesFromFile(testFilePath.get(i).replace(".test.", ".dsperprocess.predict."));
            if (gsTxt.length != srlTxt.length) {
                System.out.println(testFilePath.get(i));
                System.out.println("MISMATCH DUE TO CLEARPARSER ERROR");
            } else {
                gs_writer.print(StringUtil.toString(gsTxt));
                srl_writer.print(StringUtil.toString(srlTxt));
            }
        }
        gs_writer.close();
        srl_writer.close();

        // create runtime to execute external command
        String pythonScriptPath = "./script/evaluate.py";
        String[] cmd = new String[4];
        cmd[0] = "python";
        cmd[1] = pythonScriptPath;
        cmd[2] = "gs.txt";
        cmd[3] = "srl.txt";
        
        ClearParserUtil.clearParserToTsv("gs.txt", "gs.tsv");
        ClearParserUtil.clearParserToTsv("srl.txt", "srl.tsv");
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(cmd);

        // retrieve output from python script
        BufferedReader bfr = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line = "";
        while ((line = bfr.readLine()) != null) {
            // display each output line form python script
            System.out.println(line);
        }
        StdUtil.printError(pr);
    }

    public static void main(String[] args) throws FileNotFoundException {
        SRLDSPerProcessExp srlExp = new SRLDSPerProcessExp();
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
