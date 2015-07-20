/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.srl;

import Util.ClearParserUtil;
import Util.Constant;
import Util.GlobalVariable;
import Util.MateParserUtil;
import clear.engine.SRLPredict;
import clear.engine.SRLTrain;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipOutputStream;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.w3c.dom.Element;
import qa.util.FileUtil;
import se.lth.cs.srl.Learn;
import static se.lth.cs.srl.Learn.learnOptions;
import se.lth.cs.srl.Parse;
import se.lth.cs.srl.io.AllCoNLL09Reader;
import se.lth.cs.srl.io.SentenceReader;
import se.lth.cs.srl.options.LearnOptions;
import se.lth.cs.srl.pipeline.Pipeline;
import se.lth.cs.srl.pipeline.Reranker;
import se.lth.cs.srl.util.BrownCluster;
import se.lth.cs.srl.util.Util;

/**
 *
 * @author samuellouvan
 */
public class SRLWrapper {

    public void doTrain(String trainingFileName, String modelFileName, int srlType, boolean domainAdaptation) throws FileNotFoundException, IOException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (srlType == Constant.SRL_CLEARPARSER) {
            // Train trainingFrames
            SRLTrain train = new SRLTrain();
            CmdLineParser cmd = new CmdLineParser(train);
            try {
                ClearParserUtil.TRAIN_ARGS[3] = trainingFileName;
                ClearParserUtil.TRAIN_ARGS[7] = modelFileName;
                cmd.parseArgument(ClearParserUtil.TRAIN_ARGS);
                train.init();
                train.train();
            } catch (CmdLineException e) {
                System.err.println(e.getMessage());
                cmd.printUsage(System.err);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Training file PROBLEMMMM : " + train.s_trainFile);
                System.exit(0);
            }
        } else if (srlType == Constant.SRL_MATE) {
            long startTime = System.currentTimeMillis();
            int triggerType = FileUtil.getTriggerType(trainingFileName); // for MATE
            // check trigger type
            String[] trainArgs = null;
            if (triggerType == Constant.TRIGGER_VB_ONLY) {
                System.out.println("VERB ONLY");
                //MateParserUtil.TRAIN_ARGS[0] = "eng-v";
                //new File(GlobalVariable.PROJECT_DIR.concat("/featuresets/eng")).renameTo(new File(GlobalVariable.PROJECT_DIR.concat("/featuresets/eng-temp")));
                //new File(GlobalVariable.PROJECT_DIR.concat("/featuresets/eng")).renameTo(new File(GlobalVariable.PROJECT_DIR.concat("/featuresets/eng-temp")));
                if (!domainAdaptation) {
                    trainArgs = new String[MateParserUtil.TRAIN_ARGS.length + 2];
                } else {
                    trainArgs = new String[MateParserUtil.TRAIN_ARGS.length + 4];
                }
                System.arraycopy(MateParserUtil.TRAIN_ARGS, 0, trainArgs, 0, MateParserUtil.TRAIN_ARGS.length);
                trainArgs[MateParserUtil.TRAIN_ARGS.length] = "-fdir";
                trainArgs[MateParserUtil.TRAIN_ARGS.length + 1] = GlobalVariable.PROJECT_DIR.concat("/featuresets/eng-v");
                if (domainAdaptation) {
                    trainArgs[MateParserUtil.TRAIN_ARGS.length + 2] = "-da";
                    trainArgs[MateParserUtil.TRAIN_ARGS.length + 3] = GlobalVariable.sourceIdxStart + "";
                }
            } else if (triggerType == Constant.TRIGGER_NN_ONLY) {
                //MateParserUtil.TRAIN_ARGS[0] = "eng-n";
                System.out.println("NOUN ONLY");
                if (!domainAdaptation) {
                    trainArgs = new String[MateParserUtil.TRAIN_ARGS.length + 2];
                } else {
                    trainArgs = new String[MateParserUtil.TRAIN_ARGS.length + 4];
                }
                System.arraycopy(MateParserUtil.TRAIN_ARGS, 0, trainArgs, 0, MateParserUtil.TRAIN_ARGS.length);
                trainArgs[MateParserUtil.TRAIN_ARGS.length] = "-fdir";
                trainArgs[MateParserUtil.TRAIN_ARGS.length + 1] = GlobalVariable.PROJECT_DIR.concat("/featuresets/eng-n");
                if (domainAdaptation) {
                    trainArgs[MateParserUtil.TRAIN_ARGS.length + 2] = "-da";
                    trainArgs[MateParserUtil.TRAIN_ARGS.length + 3] = GlobalVariable.sourceIdxStart + "";
                }
            }
            if (trainArgs == null) {
                MateParserUtil.TRAIN_ARGS[1] = trainingFileName;
                MateParserUtil.TRAIN_ARGS[2] = modelFileName;
                String params[] = MateParserUtil.TRAIN_ARGS;
                if (domainAdaptation) {
                    params = new String[MateParserUtil.TRAIN_ARGS.length + 2];
                    System.arraycopy(MateParserUtil.TRAIN_ARGS, 0, params, 0, MateParserUtil.TRAIN_ARGS.length);
                    params[params.length - 2] = "-da";
                    params[params.length - 1] = GlobalVariable.sourceIdxStart + "";
                    System.out.println(Arrays.toString(params));
                }
                try {
                    Method onLoaded = Learn.class.getMethod("main", String[].class);
                    onLoaded.invoke(null, (Object) params);
                } catch (InvocationTargetException e) {
                    System.out.println(e.getCause().toString());
                }
            } else {
                trainArgs[1] = trainingFileName;
                trainArgs[2] = modelFileName;
                String params[] = trainArgs;
                System.out.println(Arrays.toString(params));
                try {
                    Method onLoaded = Learn.class.getMethod("main", String[].class);
                    onLoaded.invoke(null, (Object) params);
                } catch (InvocationTargetException e) {
                    System.out.println("TrainException MATE: " + e.getCause().toString());
                } catch (Exception e) {
                    System.out.println("TrainException MATE: " + e.getCause().toString());

                }
            }
        }
    }

    public void doPredict(String testInputFileName, String predictionFileName, String modelFileName, int srlType, boolean autoPi, boolean domainAdaptation) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (srlType == Constant.SRL_CLEARPARSER) {
            ClearParserUtil.PREDICT_ARGS[3] = testInputFileName;
            ClearParserUtil.PREDICT_ARGS[5] = predictionFileName;
            ClearParserUtil.PREDICT_ARGS[7] = modelFileName;
            new SRLPredict(ClearParserUtil.PREDICT_ARGS);
        } else if (srlType == Constant.SRL_MATE) {
            String params[] = null;
            if (!autoPi) {
                MateParserUtil.PREDICT_ARGS_NOPI[1] = testInputFileName;
                MateParserUtil.PREDICT_ARGS_NOPI[2] = modelFileName;
                MateParserUtil.PREDICT_ARGS_NOPI[4] = predictionFileName;
                if (domainAdaptation) {
                    params = new String[MateParserUtil.PREDICT_ARGS_NOPI.length + 1];
                    //System.arraycopy(MateParserUtil.TRAIN_ARGS, 0, params, 0, MateParserUtil.TRAIN_ARGS.length);
                    System.arraycopy(MateParserUtil.PREDICT_ARGS_NOPI, 0, params, 0, MateParserUtil.PREDICT_ARGS_NOPI.length);
                    params[MateParserUtil.PREDICT_ARGS_NOPI.length-1] = "-da";
                    params[MateParserUtil.PREDICT_ARGS_NOPI.length] = predictionFileName;
                } else {
                    params = MateParserUtil.PREDICT_ARGS_NOPI;
                }
                System.out.println("MATEEEE NO AUTO PI");
            } else {
                MateParserUtil.PREDICT_ARGS_PI[1] = testInputFileName;
                MateParserUtil.PREDICT_ARGS_PI[2] = modelFileName;
                //MateParserUtil.PREDICT_ARGS_PI[3] = "-da";
                MateParserUtil.PREDICT_ARGS_PI[3] = predictionFileName;
                if (domainAdaptation) {
                    params = new String[MateParserUtil.PREDICT_ARGS_PI.length + 1];
                    //System.arraycopy(MateParserUtil.TRAIN_ARGS, 0, params, 0, MateParserUtil.TRAIN_ARGS.length);
                    System.arraycopy(MateParserUtil.PREDICT_ARGS_PI, 0, params, 0, MateParserUtil.PREDICT_ARGS_PI.length);
                    params[MateParserUtil.PREDICT_ARGS_PI.length-1] = "-da";
                    params[MateParserUtil.PREDICT_ARGS_PI.length] = predictionFileName;
                } else {
                    params = MateParserUtil.PREDICT_ARGS_PI;
                }

                System.out.println("MATE AUTO PI "+Arrays.toString(params));
            }

            try {
                Method onLoaded = Parse.class.getMethod("main", String[].class);
                onLoaded.invoke(null, (Object) params);
            } catch (InvocationTargetException e) {
                System.out.println(e.getCause().toString());
            }
        }

    }

    public void doPredict(String get, String replace, String get0, int srlType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
