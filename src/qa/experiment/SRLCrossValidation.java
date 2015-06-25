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
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;

/**
 *
 * @author samuellouvan
 */
public class SRLCrossValidation {
    // -f process filename
    // -k fold
    // -o outDir
    // -t training Size

    ProcessFrameProcessor proc;
    ArrayList<ProcessFrame> frameArr;
    // -f <process file>  -o<outputDir> -d<dsDirectory>

    @Option(name = "-f", usage = "process file", required = true, metaVar = "REQUIRED")
    private String processTsvFileName;

    @Option(name = "-o", usage = "output directory name", required = true, metaVar = "REQUIRED")
    private String outDirName;

    @Option(name = "-t", usage = "number of training data", required = true, metaVar = "REQUIRED")
    private int nbTrainingData = -1;

    @Option(name = "-k", usage = "number of fold", required = true, metaVar = "REQUIRED")
    private int fold = -1;
    ArrayList<String> testFilePath;

    public SRLCrossValidation() {
        testFilePath = new ArrayList<String>();
    }

    public void init() throws IOException, FileNotFoundException, ClassNotFoundException {
        proc = new ProcessFrameProcessor(processTsvFileName);

        proc.loadProcessData();
        frameArr = proc.getProcArr();

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

    public ArrayList<ProcessFrame> getTrainingFrames(int trainStartIdx, int trainEndIdx) {
        ArrayList<ProcessFrame> trainingFrames = new ArrayList<ProcessFrame>();
        for (int i = 0; i < frameArr.size(); i++) {
            if (i < trainStartIdx || i >= trainEndIdx) {
                if (trainingFrames.size() < nbTrainingData) {
                    trainingFrames.add(frameArr.get(i));
                }
            }
        }
        return trainingFrames;
    }

    public ArrayList<ProcessFrame> getTestFrames(int trainStartIdx, int trainEndIdx) {
        ArrayList<ProcessFrame> testFrames = new ArrayList<ProcessFrame>(frameArr.subList(trainStartIdx, trainEndIdx));
        return testFrames;
    }

    public void trainTestCrossVal() throws IOException, InterruptedException {
        int testSize = proc.getProcArr().size() / fold;
        int startIdx = 0;
        int endIdx = testSize;

        testFilePath = new ArrayList<String>();
        for (int currentFold = 0; currentFold < fold; currentFold++) {
            ArrayList<ProcessFrame> testingFrames = getTestFrames(startIdx, endIdx);
            ArrayList<ProcessFrame> trainingFrames = getTrainingFrames(startIdx, endIdx);
            String trainingFileName = outDirName + "/train.cv." + currentFold;
            String testingFileName = outDirName + "/test.cv." + currentFold;
            String modelName = outDirName + "/model.cv." + currentFold;
            // Create training file
            ProcessFrameUtil.toClearParserFormat(trainingFrames, trainingFileName);
            // Create testing file
            ProcessFrameUtil.toClearParserFormat(testingFrames, testingFileName);
            testFilePath.add(testingFileName);

            SRLTrain train = new SRLTrain();
            CmdLineParser cmd = new CmdLineParser(train);

            // SRL Train
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

            // SRL Test
            startIdx = endIdx;
            endIdx = startIdx + testSize;
            if (endIdx >= frameArr.size()) {
                endIdx = frameArr.size() - 1;
            }
        }
        Thread.sleep(5000);
        for (int i = 0; i < testFilePath.size(); i++) {
            ClearParserUtil.PREDICT_ARGS[3] = testFilePath.get(i);
            ClearParserUtil.PREDICT_ARGS[5] = testFilePath.get(i).replace("test.", "predict.");
            ClearParserUtil.PREDICT_ARGS[7] = testFilePath.get(i).replace("test.", "model.");
            new SRLPredict(ClearParserUtil.PREDICT_ARGS);
        }
        int x =0;
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
            String[] srlTxt = FileUtil.readLinesFromFile(testFilePath.get(i).replace("test.", "predict."));
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

        ClearParserUtil.clearParserToTsv("gs.txt", outDirName + "/gs.tsv." + outDirName);
        ClearParserUtil.clearParserToTsv("srl.txt", outDirName + "/srl.tsv." + outDirName);
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

    public static void main(String[] args) {
        SRLCrossValidation srlExp = new SRLCrossValidation();
        CmdLineParser cmd = new CmdLineParser(srlExp);

        try {
            cmd.parseArgument(args);
            srlExp.init();
            srlExp.trainTestCrossVal();
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
