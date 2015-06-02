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
public class SRLCombinedModelExp {

    ProcessFrameProcessor proc;

    private ArrayList<String> blackList;
    @Option(name = "-f", usage = "process file", required = true, metaVar = "REQUIRED")
    private String processTsvFileName;

    @Option(name = "-o", usage = "output directory name", required = true, metaVar = "REQUIRED")
    private String outDirName;

    @Option(name = "-n", usage = "number of processes to test", required = false, metaVar = "OPTIONAL")
    private int nbProcess = 0;

    boolean limitedProcess = false;
    private ArrayList<ProcessFrame> frameArr;
    private HashMap<String, Integer> processFold;
    private ArrayList<String> processNames;
    ArrayList<String> testFilePath;
    ArrayList<String> trainingModelFilePath;
    String[] blackListProcess = {"Salivating", "composted", "decant_decanting", "dripping", "magneticseparation", "loosening", "momentum", "seafloorspreadingtheory", "sedimentation",
        "spear_spearing", "retract"};

    public SRLCombinedModelExp() throws FileNotFoundException {
        trainingModelFilePath = new ArrayList<String>();
        testFilePath = new ArrayList<String>();
        processFold = new HashMap<String, Integer>();
        processNames = new ArrayList<String>();
        blackList = new ArrayList<String>();
    }

    public void init() throws FileNotFoundException {
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
        frameArr = proc.getProcArr();
        for (int i = 0; i < frameArr.size(); i++) {
            String normName = ProcessFrameUtil.normalizeProcessName(frameArr.get(i).getProcessName());
            processFold.put(normName, 0);
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

    public void trainAndPredict() throws FileNotFoundException, IOException, InterruptedException {
        testFilePath.clear();
        trainingModelFilePath.clear();
        for (int i = 0; i < frameArr.size(); i++) {
            ProcessFrame testFrame = frameArr.get(i);
            String normalizedProcessName = ProcessFrameUtil.normalizeProcessName(testFrame.getProcessName());
            if ((!limitedProcess || (limitedProcess && processNames.contains(normalizedProcessName))) && !blackList.contains(normalizedProcessName)) {
                int fold = processFold.get(normalizedProcessName);
                ProcessFrameUtil.toClearParserFormat(testFrame, outDirName + "/" + normalizedProcessName + ".test.cv." + fold);  // out to <process_frame_>.test.cv.<fold>
                testFilePath.add(outDirName + "/" + normalizedProcessName + ".test.cv." + fold);
                processFold.put(normalizedProcessName, fold + 1);

                // Get the testing data
                ArrayList<ProcessFrame> trainingFrames = new ArrayList<ProcessFrame>();
                for (int j = 0; j < frameArr.size(); j++) {
                    if (i != j) {
                        String processName = ProcessFrameUtil.normalizeProcessName(frameArr.get(j).getProcessName());
                        //trainingFrames.add(frameArr.get(j));
                        if (!blackList.contains(processName)) {
                            trainingFrames.add(frameArr.get(j));
                        } else {
                            System.out.println("BLACKLIST");
                        }
                    }
                }
                String trainingFileName = outDirName + "/" + normalizedProcessName + ".train.combined.cv." + fold;
                trainingModelFilePath.add(outDirName + "/" + normalizedProcessName + ".combinedmodel.cv." + fold);
                String modelName = outDirName + "/" + normalizedProcessName + ".combinedmodel.cv." + fold;
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
                    System.exit(0);
                }

            }
            // Perform prediction
        }
        Thread.sleep(10000);
        for (int i = 0; i < testFilePath.size(); i++) {
            ClearParserUtil.PREDICT_ARGS[3] = testFilePath.get(i);
            ClearParserUtil.PREDICT_ARGS[5] = testFilePath.get(i).replace(".test.", ".combined.predict.");
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
            String[] srlTxt = FileUtil.readLinesFromFile(testFilePath.get(i).replace(".test.", ".combined.predict."));
            gs_writer.print(StringUtil.toString(gsTxt));
            srl_writer.print(StringUtil.toString(srlTxt));
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
        SRLCombinedModelExp srlExp = new SRLCombinedModelExp();
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
