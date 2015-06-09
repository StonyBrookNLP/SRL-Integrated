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
public class SRLTrainPredictIndividual {
    
    public void trainPredict(String trainingFileName, String testFileName, String outDir) throws FileNotFoundException, IOException, InterruptedException, ClassNotFoundException {
        // load frames from training File
        ProcessFrameProcessor proc = new ProcessFrameProcessor(trainingFileName);
        proc.loadProcessData();
        String trainingA0 = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".A0.clearparser";
        String trainingA1 = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".A1.clearparser";
        String trainingA2 = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".A2.clearparser";
        proc.toClearParserFormat(trainingA0, "A0");
        proc.toClearParserFormat(trainingA1, "A1");
        proc.toClearParserFormat(trainingA2, "A2");

        SRLTrain train = new SRLTrain();
        CmdLineParser cmd = new CmdLineParser(train);

        // build model
        try {
            ClearParserUtil.TRAIN_ARGS[3] = trainingA0;
            ClearParserUtil.TRAIN_ARGS[7] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".A0.model";
            cmd.parseArgument(ClearParserUtil.TRAIN_ARGS);
            train.init();
            train.train();
            
            ClearParserUtil.TRAIN_ARGS[3] = trainingA1;
            ClearParserUtil.TRAIN_ARGS[7] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".A1.model";
            cmd.parseArgument(ClearParserUtil.TRAIN_ARGS);
            train.init();
            train.train();
            
            ClearParserUtil.TRAIN_ARGS[3] = trainingA2;
            ClearParserUtil.TRAIN_ARGS[7] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".A2.model";
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
        String testFileNameFull = outDir + FileUtil.getFileNameWoExt(testFileName) + ".clearparser";
        testProc.toClearParserFormat(testFileNameFull);

        ClearParserUtil.PREDICT_ARGS[3] = testFileNameFull;
        ClearParserUtil.PREDICT_ARGS[5] = outDir + FileUtil.getFileNameWoExt(testFileName) + ".A0.predict";
        ClearParserUtil.PREDICT_ARGS[7] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".A0.model";
        new SRLPredict(ClearParserUtil.PREDICT_ARGS);
        
        
        ClearParserUtil.PREDICT_ARGS[3] = testFileNameFull;
        ClearParserUtil.PREDICT_ARGS[5] = outDir + FileUtil.getFileNameWoExt(testFileName) + ".A1.predict";
        ClearParserUtil.PREDICT_ARGS[7] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".A1.model";
        new SRLPredict(ClearParserUtil.PREDICT_ARGS);
        
        
        ClearParserUtil.PREDICT_ARGS[3] = testFileNameFull;
        ClearParserUtil.PREDICT_ARGS[5] = outDir + FileUtil.getFileNameWoExt(testFileName) + ".A2.predict";
        ClearParserUtil.PREDICT_ARGS[7] = outDir + FileUtil.getFileNameWoExt(trainingFileName) + ".A2.model";
        new SRLPredict(ClearParserUtil.PREDICT_ARGS);

        // testing time
        // evaluate
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, InterruptedException, ClassNotFoundException {
        SRLTrainPredictIndividual srlExp = new SRLTrainPredictIndividual();
        srlExp.trainPredict("./data/process_10_ds.tsv", "./data/test10dirty.tsv", "./data/50_100_exp/");
    }
}
