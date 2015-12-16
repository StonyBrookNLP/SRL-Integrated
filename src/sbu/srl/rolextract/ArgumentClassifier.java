/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import Util.ArgProcessAnnotationDataUtil;
import Util.SentenceUtil;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgProcessAnnotationData;
import sbu.srl.datastructure.ArgProcessAnnotationDataSerializer;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.ArgumentSpanSerializer;
import sbu.srl.datastructure.JSONData;
import sbu.srl.datastructure.Sentence;
import sbu.srl.datastructure.SentenceSerializer;

/**
 *
 * @author slouvan
 */
public class ArgumentClassifier {

    SpockDataReader dataReader;
    @Option(name = "--training-file", usage = "source for the training file", required = true)
    private String trainingFileName;

    @Option(name = "--config-file", usage = "config for the training file", required = true)
    private String configFileName;
    
    @Option(name = "--multiclass", usage = "config for the training file")
    private boolean isMultiClass;

    @Option(name = "--cross-validation", usage = "number of folds for cross validation")
    private int crossValidation = -1;

    @Option(name = "--output-dir", usage = "output directory for prediction or cross validation", required = true)
    private String outputDir;

    private ArrayList<Sentence> sentences = new ArrayList<>();

    @Option(name = "--ilp", usage = "generate ILP output")
    private boolean ilp = false;

    @Option(name = "--tt", usage = "trainTest")
    private boolean trainTest = false;
    public ArgumentClassifier() throws FileNotFoundException, IOException {

    }

    public void doCrossValidation() throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (crossValidation != -1) {
            setupCrossValidationEnvironment(outputDir, crossValidation); // setting up the directory
            distributeCrossValidationData(outputDir, crossValidation);
            performCrossValidation(outputDir, crossValidation);
        }
    }

    public void performCrossValidation(String outputDir, int crossValidation) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        for (int i = 1; i <= crossValidation; i++) {
            File trainFoldDir = new File(outputDir.concat("/fold-").concat("" + i).concat("/train"));
            File testFoldDir = new File(outputDir.concat("/fold-").concat("" + i).concat("/test"));
            SBURoleTrain trainer = new SBURoleTrain(trainFoldDir.getAbsolutePath().concat("/train.ser"), isMultiClass);
            if (isMultiClass)
            {
                trainer.trainMultiClassClassifier(trainFoldDir.getAbsolutePath());
            }
            else
            {
                trainer.trainBinaryClassifier(trainFoldDir.getAbsolutePath());
            }
            SBURolePredict predict = new SBURolePredict(trainFoldDir.getAbsolutePath(), testFoldDir.getAbsolutePath().concat("/test.arggold.ser"), isMultiClass);
            predict.performPrediction(testFoldDir.getAbsolutePath().concat("/test.arggold.ser"));

            ArrayList<Sentence> predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testFoldDir.getAbsolutePath().concat("/test.argpredict.ser"));
            Map<String, List<Sentence>> groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));

            ArrayList<JSONData> jsonData = SentenceUtil.generateJSONData(groupByProcess);
            SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlout.json"), false);
            SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlpredict.json"), true);
        }
    }

    public void distributeCrossValidationData(String outputDir, int nbFold) throws IOException {
        //  Get unique sentences from the proc data
        //ArrayList<Sentence> sentences = ArgProcessAnnotationDataUtil.getUniqueSentence(sentences);
        //  Randomize it
        Collections.shuffle(sentences, new Random(System.nanoTime()));
        //  
        int startIdx = 0;
        int endIdx = sentences.size() / nbFold - 1;
        for (int i = 1; i <= nbFold; i++) {
            ArrayList<Sentence> trainingData = new ArrayList<>();
            ArrayList<Sentence> testingData = new ArrayList<>();
            if (i == nbFold) {
                endIdx = sentences.size() - 1;
            }
            // Get the training data
            int j;
            trainingData.addAll(sentences.subList(0, startIdx));
            trainingData.addAll(sentences.subList(endIdx + 1, sentences.size()));
            testingData.addAll(sentences.subList(startIdx, endIdx + 1));

            FileUtil.serializeToFile(trainingData, outputDir.concat("/fold-" + i).concat("/train/train.ser"));
            FileUtil.serializeToFile(testingData, outputDir.concat("/fold-" + i).concat("/test/test.arggold.ser"));

            startIdx = endIdx + 1;
            endIdx = endIdx + (sentences.size() / nbFold - 1);
        }

    }

    public void setupCrossValidationEnvironment(String crossValidationDirName, int nbFold) throws IOException {
        System.out.println("Setting up cross validation environment");
        boolean dirCreated = FileUtil.mkDir(crossValidationDirName);
        if (dirCreated) {
            File crossValDir = new File(crossValidationDirName);
            for (int i = 1; i <= nbFold; i++) {
                boolean succeed = FileUtil.mkDir(crossValDir.getAbsolutePath().concat("/fold-" + i));
                if (succeed) {
                    FileUtil.mkDir(crossValDir.getAbsolutePath().concat("/fold-" + i).concat("/train"));
                    FileUtil.mkDir(crossValDir.getAbsolutePath().concat("/fold-" + i).concat("/test"));
                } else {
                    System.out.println("FAILED CV Environment");
                }
            }
            System.out.println("Finish setting up cross validation environment");
        } else {
            System.out.println("Directory exist");
        }
    }

    public void doTrainClassify(double trainPctg) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        setupCrossValidationEnvironment(outputDir, 1);
        Collections.shuffle(sentences, new Random(System.nanoTime()));
        //  
        int startIdx = 0;
        int nbTrain = (int) (trainPctg * sentences.size());
        ArrayList<Sentence> trainingData = new ArrayList<>();
        ArrayList<Sentence> testingData = new ArrayList<>();

        trainingData.addAll(sentences.subList(0, nbTrain));
        testingData.addAll(sentences.subList(nbTrain, sentences.size()));

        FileUtil.serializeToFile(trainingData, outputDir.concat("/fold-1").concat("/train/train.ser"));
        FileUtil.serializeToFile(testingData, outputDir.concat("/fold-1").concat("/test/test.arggold.ser"));

        File trainFoldDir = new File(outputDir.concat("/fold-1").concat("/train"));
        File testFoldDir = new File(outputDir.concat("/fold-1").concat("/test"));
        SBURoleTrain trainer = new SBURoleTrain(trainFoldDir.getAbsolutePath().concat("/train.ser"), isMultiClass);
        if (isMultiClass)
        {

            trainer.trainMultiClassClassifier(trainFoldDir.getAbsolutePath());
        }
        else
            trainer.trainBinaryClassifier(trainFoldDir.getAbsolutePath());
        FileUtil.serializeToFile(trainingData, outputDir.concat("/fold-1").concat("/train/train.ser"));
        SBURolePredict predict = new SBURolePredict(trainFoldDir.getAbsolutePath(), testFoldDir.getAbsolutePath().concat("/test.arggold.ser"),isMultiClass);
        predict.performPrediction(testFoldDir.getAbsolutePath().concat("/test.arggold.ser"));

        ArrayList<Sentence> predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testFoldDir.getAbsolutePath().concat("/test.argpredict.ser"));
        Map<String, List<Sentence>> groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));

        ArrayList<JSONData> jsonData = SentenceUtil.generateJSONData(groupByProcess);
        SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlout.json"), false);
        SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlpredict.json"), true);
    }

    public void trainAndTest(String trainDir, String testDir) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        SBURoleTrain trainer = new SBURoleTrain(trainDir.concat("/train.ser"), isMultiClass);
        ArrayList<Sentence> trainData = (ArrayList<Sentence>) FileUtil.deserializeFromFile(trainDir.concat("/train.ser"));
        if (isMultiClass)
            trainer.trainMultiClassClassifier(trainDir);
        else
            trainer.trainBinaryClassifier(trainDir);
        
        FileUtil.serializeToFile(trainData, trainDir.concat("/train.ser"));
        SBURolePredict predict = new SBURolePredict(trainDir, testDir.concat("/test.arggold.ser"), isMultiClass);
        predict.performPrediction(testDir.concat("/test.arggold.ser"));
        ArrayList<Sentence> predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testDir.concat("/test.argpredict.ser"));
        Map<String, List<Sentence>> groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));

        ArrayList<JSONData> jsonData = SentenceUtil.generateJSONData(groupByProcess);
        SentenceUtil.flushDataToJSON(jsonData, testDir.concat("/test.srlout.json"), false);
        SentenceUtil.flushDataToJSON(jsonData, testDir.concat("/test.srlpredict.json"), true);
    }

    public void doMain() throws CmdLineException {
        try {
            dataReader = new SpockDataReader(trainingFileName, configFileName);
            dataReader.readData();
            sentences = dataReader.getSentences();
            sentences = (ArrayList<Sentence>) sentences.stream().filter(data -> data.isAnnotated()).collect(Collectors.toList());
            if (crossValidation != -1) {
                try {
                    doCrossValidation();
                } catch (IOException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
            else if (trainTest)
            {
                try {
                    trainAndTest("/home/slouvan/NetBeansProjects/SRL-Integrated/data/sandbox-60-40/train", 
                                 "/home/slouvan/NetBeansProjects/SRL-Integrated/data/sandbox-60-40/test");
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else {
                try {
                    System.out.println("Split 0.6");
                    doTrainClassify(0.6);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        try {
            ArgumentClassifier classifier = new ArgumentClassifier();
            CmdLineParser parser = new CmdLineParser(classifier);
            parser.parseArgument(args);
            classifier.doMain();
        } catch (CmdLineException ex) {
            Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
