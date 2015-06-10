/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import Util.ClearParserUtil;
import clear.engine.SRLPredict;
import clear.engine.SRLTrain;
import clear.util.FileUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import qa.ProcessFrameProcessor;

/**
 *
 * @author samuellouvan
 */
public class SRLTrainPredict {

    public void trainPredict(String trainingFileName, String testFileName, String outDir) throws FileNotFoundException, IOException, InterruptedException, ClassNotFoundException {
        // load frames from training File
        ProcessFrameProcessor proc = new ProcessFrameProcessor(trainingFileName);
        proc.loadProcessData();
        proc.toClearParserFormat(outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".clearparser");

        SRLTrain train = new SRLTrain();
        CmdLineParser cmd = new CmdLineParser(train);

        // build model
        try {
            ClearParserUtil.TRAIN_ARGS[3] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".clearparser";
            ClearParserUtil.TRAIN_ARGS[7] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".model";
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
        Thread.sleep(10000);
        ProcessFrameProcessor testProc = new ProcessFrameProcessor(testFileName);
        testProc.loadProcessData();
        testProc.toClearParserFormat(outDir + FileUtil.getFileNameWoExt(testFileName) + ".clearparser");

        ClearParserUtil.PREDICT_ARGS[3] = outDir + FileUtil.getFileNameWoExt(testFileName) + ".clearparser";
        ClearParserUtil.PREDICT_ARGS[5] = outDir + FileUtil.getFileNameWoExt(testFileName) + ".predict";
        ClearParserUtil.PREDICT_ARGS[7] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".model";
        new SRLPredict(ClearParserUtil.PREDICT_ARGS);

        // testing time
        // evaluate
    }

    public void trainPredictClearParser(String trainingFileName, String testFileName, String outDir) throws FileNotFoundException, IOException, InterruptedException, ClassNotFoundException {

        SRLTrain train = new SRLTrain();
        CmdLineParser cmd = new CmdLineParser(train);

        // build model
        try {
            ClearParserUtil.TRAIN_ARGS[3] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".clearparser";
            ClearParserUtil.TRAIN_ARGS[7] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".model";
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
        Thread.sleep(10000);

        ClearParserUtil.PREDICT_ARGS[3] = outDir + FileUtil.getFileNameWoExt(testFileName) + ".clearparser"; 
        ClearParserUtil.PREDICT_ARGS[5] = outDir + FileUtil.getFileNameWoExt(testFileName) + ".predict";
        ClearParserUtil.PREDICT_ARGS[7] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".model";
        new SRLPredict(ClearParserUtil.PREDICT_ARGS);

        // testing time
        // evaluate
    }
    public static void main(String[] args) throws IOException, FileNotFoundException, InterruptedException, ClassNotFoundException {
        SRLTrainPredict srlExp = new SRLTrainPredict();
        //srlExp.trainPredict("./data/process_50_ds_w_pattern.tsv", "./data/process_100.tsv", "./data/50_100_exp/");
        srlExp.trainPredictClearParser("./data/trainingProcessQuestionDS.clearparser", "./data/testingProcessQuestion.clearparser",  "./data/SRL-QA/");
    }
}
