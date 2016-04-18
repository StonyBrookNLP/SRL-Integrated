/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import Util.ArgProcessAnnotationDataUtil;
import Util.GlobalV;
import Util.SentenceUtil;
import Util.StdUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.cmu.cs.lti.ark.fn.data.prep.AllAnnotationsMergingWithoutNE;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import scala.actors.threadpool.Arrays;

/**
 *
 * @author slouvan
 */
public class ArgumentClassifier {

    SpockDataReader dataReader;
    @Option(name = "--training-file", usage = "source for the training file", required = true)
    private String trainingFileName;

    @Option(name = "--testing-file", usage = "source for the testing file")
    private String testingFileName;

    @Option(name = "--config-file", usage = "config for the training file", required = true)
    private String configFileName;

    @Option(name = "--multiclass", usage = "config for the training file")
    private boolean isMultiClass;

    @Option(name = "--cross-validation", usage = "number of folds for cross validation")
    private int crossValidation = -1;

    @Option(name = "--extractor", usage = "Extractor mode")
    private boolean extractorMode = false;

    @Option(name = "--output-dir", usage = "output directory for prediction or cross validation", required = true)
    private String outputDir;

    private ArrayList<Sentence> sentences = new ArrayList<>();

    @Option(name = "--ilp", usage = "generate ILP output")
    private boolean ilp = false;

    @Option(name = "--tt", usage = "trainTest")
    private boolean trainTest = false;

    @Option(name = "--dev", usage = "devSet")
    private boolean devSet = false;

    @Option(name = "--feat-addition", usage = "--feat-addition")
    private boolean featAddition = false;

    @Option(name = "--dev-process-file", usage = "dev process filename")
    private String devProcessFileName;

    @Option(name = "--semafor-offset", usage = "SEMAFOR OFFSET FOR TRAINING DATA", required = true)
    private int semOffset = -1;

    private String MALT_PARSER_PATH = "/home/slouvan/NetBeansProjects/semafor/bin/runMalt.sh";

    public ArgumentClassifier() throws FileNotFoundException, IOException {

    }

    public void doCrossValidation() throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
        if (crossValidation != -1) {
            setupCrossValidationEnvironment(outputDir, crossValidation); // setting up the directory
            //distributeCrossValidationData(outputDir, crossValidation);
            distributeCrossValidationByProcess(outputDir, crossValidation);
            performCrossValidation(outputDir, crossValidation);
            //performAblation(outputDir, crossValidation);
        }
    }

    public void doFeatureAddition() throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
        if (crossValidation != -1) {
            setupCrossValidationEnvironment(outputDir, crossValidation); // setting up the directory
            distributeCrossValidationByProcess(outputDir, crossValidation);
            performedFeatureAddition(outputDir, crossValidation);
        }
    }

    public void performCrossValidation(String outputDir, int crossValidation) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        for (int i = 1; i <= crossValidation; i++) {
            File trainFoldDir = new File(outputDir.concat("/fold-").concat("" + i).concat("/train"));
            File testFoldDir = new File(outputDir.concat("/fold-").concat("" + i).concat("/test"));
            SBURoleTrain trainer = new SBURoleTrain(trainFoldDir.getAbsolutePath().concat("/train.ser"), isMultiClass);
            trainer.train(trainFoldDir.getAbsolutePath());

            SBURolePredict predict = new SBURolePredict(trainFoldDir.getAbsolutePath(), testFoldDir.getAbsolutePath().concat("/test.arggold.ser"), isMultiClass);
            predict.performPrediction(testFoldDir.getAbsolutePath().concat("/test.arggold.ser"));

            ArrayList<Sentence> predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testFoldDir.getAbsolutePath().concat("/test.argpredict.ser"));
            Map<String, List<Sentence>> groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));

            ArrayList<JSONData> jsonData = SentenceUtil.generateJSONData(groupByProcess);
            SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlout.json"), false);
            SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlpredict.json"), true);
            SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.ilppredict.json"), true);
            SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.semaforpredict.json"), true);
            SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.easysrlpredict.json"), true);
            
            
            
            /*predict.performPredictionEasySRL(testFoldDir.getAbsolutePath().concat("/test.arggold.ser"), 
             outputDir.concat("/fold-" + i).concat("/test/cv." + i + ".test.sentence.sbu"), 
             outputDir.concat("/fold-" + i).concat("/test/cv." + i + ".raw.predict.easysrl"), 
             "./data/modelCCG", outputDir.concat("/fold-" + i));
            
             predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testFoldDir.getAbsolutePath().concat("/test.argeasysrlpredict.ser"));
             groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));

             jsonData = SentenceUtil.generateJSONData(groupByProcess);*/
            SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.easysrlpredict.json"), true);
        }
    }

    public List<String> getAblationFeatures(String fileNamePath) throws FileNotFoundException {
        String[] lines = FileUtil.readLinesFromFile(fileNamePath);
        return Arrays.asList(lines);
    }

    public void performAblation(String outputDir, int crossValidation) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
        ArrayList<String> triedFeatures = new ArrayList<String>(Arrays.asList(FileUtil.readLinesFromFile("./configSBUProcRel/features.ori")));
        List<String> ablationFeatures = getAblationFeatures("./configSBUProcRel/features.ablation");

        for (int idxAblation = 0; idxAblation < ablationFeatures.size(); idxAblation++) {
            System.out.println("Removing features : " + ablationFeatures.get(idxAblation));
            Thread.sleep(3000);
            List<String> removedFeatures = Arrays.asList(ablationFeatures.get(idxAblation).split(","));
            triedFeatures.removeAll(removedFeatures);
            FileUtil.dumpToFile(triedFeatures, "./configSBUProcRel/features");
            for (int idxFold = 1; idxFold <= crossValidation; idxFold++) {
                File trainFoldDir = new File(outputDir.concat("/fold-").concat("" + idxFold).concat("/train"));
                File testFoldDir = new File(outputDir.concat("/fold-").concat("" + idxFold).concat("/test"));
                SBURoleTrain trainer = new SBURoleTrain(trainFoldDir.getAbsolutePath().concat("/train.ser"), isMultiClass);
                trainer.train(trainFoldDir.getAbsolutePath());

                SBURolePredict predict = new SBURolePredict(trainFoldDir.getAbsolutePath(), testFoldDir.getAbsolutePath().concat("/test.arggold.ser"), isMultiClass);
                predict.performPrediction(testFoldDir.getAbsolutePath().concat("/test.arggold.ser"));

                ArrayList<Sentence> predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testFoldDir.getAbsolutePath().concat("/test.argpredict.ser"));
                Map<String, List<Sentence>> groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));

                ArrayList<JSONData> jsonData = SentenceUtil.generateJSONData(groupByProcess);
                SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlout.json"), false);
                SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlpredict.json"), true);
                SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.ilppredict.json"), true); // dummy
                SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.semaforpredict.json"), true);// dummy
                SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.easysrlpredict.json"), true);// dummy

            }
            // copy all data to ILP's data folder
            // cp -r outputDir /home/slouvan/NetBeansProjects/ILP/data/
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "/home/slouvan/NetBeansProjects/SRL-Integrated/script/cpDir.sh", outputDir,
                        "/home/slouvan/NetBeansProjects/ILP/data/");
                //pb.environment().put("param1", )
                Process p = pb.start();     // Start the process.
                p.waitFor();                // Wait for the process to finish.
                StdUtil.printOutput(p);

                pb = new ProcessBuilder("/usr/bin/python", "/home/slouvan/NetBeansProjects/ILP/evaluate.py");
                p = pb.start();             // Start the process.
                p.waitFor();                // Wait for the process to finish.
                StdUtil.printOutput(p);

                System.out.println("Script executed successfully");
            } catch (Exception e) {
                e.printStackTrace();
            }
            String[] lines = FileUtil.readLinesFromFile("/home/slouvan/NetBeansProjects/ILP/stats.txt");
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(GlobalV.PROJECT_DIR + "/ablationNew.txt", true)));
            //more code
            out.println((new Date()).toString() + " Removed features " + removedFeatures);
            out.println("Eval : " + Arrays.toString(lines));
            out.close();

            triedFeatures.addAll(removedFeatures);
        }
    }

    public void performGreedySearch(String outputDir, int crossValidation) throws FileNotFoundException, IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
        // availFeatures  =  Get all available features)
        List<String> availableFeatures = new ArrayList<String>(Arrays.asList(FileUtil.readLinesFromFile("./configSBUProcRel/features")));
        int nbFeat = availableFeatures.size();
        ArrayList<String> triedFeatures = Lists.newArrayList();
        while (triedFeatures.size() < nbFeat) {
            double maxF1 = 0.0;
            String bestFeat = "";
            for (int i = 0; i < availableFeatures.size(); i++) {
                String nextFeat = availableFeatures.get(i);
                System.out.println("Trying with " + nextFeat);
                Thread.sleep(5000);
                triedFeatures.add(nextFeat);
                FileUtil.dumpToFile(triedFeatures, "./configSBUProcRel/features");

                for (int j = 1; j <= 1; j++) {
                    File trainFoldDir = new File(outputDir.concat("/fold-").concat("" + j).concat("/train"));
                    File testFoldDir = new File(outputDir.concat("/fold-").concat("" + j).concat("/test"));
                    SBURoleTrain trainer = new SBURoleTrain(trainFoldDir.getAbsolutePath().concat("/train.ser"), isMultiClass);
                    trainer.train(trainFoldDir.getAbsolutePath());

                    SBURolePredict predict = new SBURolePredict(trainFoldDir.getAbsolutePath(), testFoldDir.getAbsolutePath().concat("/test.arggold.ser"), isMultiClass);
                    predict.performPrediction(testFoldDir.getAbsolutePath().concat("/test.arggold.ser"));

                    ArrayList<Sentence> predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testFoldDir.getAbsolutePath().concat("/test.argpredict.ser"));
                    Map<String, List<Sentence>> groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));

                    ArrayList<JSONData> jsonData = SentenceUtil.generateJSONData(groupByProcess);
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlout.json"), false);
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlpredict.json"), true);
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.ilppredict.json"), true);
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.semaforpredict.json"), true);
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.easysrlpredict.json"), true);
                }
                // copy all data to ILP's data folder
                // cp -r outputDir /home/slouvan/NetBeansProjects/ILP/data/
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            "/home/slouvan/NetBeansProjects/SRL-Integrated/script/cpDir.sh", outputDir,
                            "/home/slouvan/NetBeansProjects/ILP/data/");
                    //pb.environment().put("param1", )
                    Process p = pb.start();     // Start the process.
                    p.waitFor();                // Wait for the process to finish.
                    StdUtil.printOutput(p);

                    pb = new ProcessBuilder("/usr/bin/python", "/home/slouvan/NetBeansProjects/ILP/evaluate.py");
                    p = pb.start();             // Start the process.
                    p.waitFor();                // Wait for the process to finish.
                    StdUtil.printOutput(p);

                    System.out.println("Script executed successfully");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String[] lines = FileUtil.readLinesFromFile("/home/slouvan/NetBeansProjects/ILP/f1.txt");
                double currentF1 = Double.parseDouble(lines[0]);
                if (currentF1 > maxF1) {
                    maxF1 = currentF1;
                    bestFeat = nextFeat;
                }
                triedFeatures.remove(nextFeat);
            }

            triedFeatures.add(bestFeat);
            System.out.println("Features used : " + triedFeatures);
            System.out.println("Best feature at length " + triedFeatures.size() + " is " + bestFeat + " currentF1 : " + maxF1);
            availableFeatures.remove(bestFeat);
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(GlobalV.PROJECT_DIR + "/ablation.txt", true)));
            out.println("Features used : " + triedFeatures);
            //more code
            out.println((new Date()).toString() + " Best feature at length " + triedFeatures.size() + " is " + bestFeat + " currentF1 : " + maxF1);
            System.out.println("Tried features length : " + triedFeatures.size() + " NbFeat :" + nbFeat);
            out.close();
            //more code                                                                                                                                      
        }
        //      for each feat from availFeat
        //         add nextFEat to triedFeat
        //         set the feature config file
        //         doCrossVal, output dummy semafor etc
        //         measureF1 {python here} output to a file, read that file
        //         updateMax
        //         remove nextFeat
        //      print best F1 here
        //      add bestFeat to triedFeat
    }

    public void performedFeatureAddition(String outputDir, int crossValidation) throws FileNotFoundException, IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
        List<String> ablationFeatures = getAblationFeatures("./configSBUProcRel/features.ablation");
        ArrayList<String> stepwiseFeatures = new ArrayList<String>();
        for (int idxAblation = 0; idxAblation < ablationFeatures.size(); idxAblation++) {
            double maxF1 = Double.MIN_VALUE;
            ArrayList<String> currentBestFeat = new ArrayList<String>();
            String[] metricsBest = null;
            for (int idxFeat = 0; idxFeat < ablationFeatures.size(); idxFeat++) {
                Thread.sleep(3000);
                ArrayList<String> addedFeatures = new ArrayList<String>();
                addedFeatures.addAll(Arrays.asList(ablationFeatures.get(idxFeat).split(",")));
                //(ArrayList<String>) Arrays.asList(ablationFeatures.get(idxAblation).split(","));
                boolean triedFeatures = false;

                for (int i = 0; i < addedFeatures.size(); i++) {
                    if (stepwiseFeatures.contains(addedFeatures.get(i))) {
                        triedFeatures = true;
                    }
                }
                if (triedFeatures) {
                    continue;
                }
                System.out.println("Adding features : " + ablationFeatures.get(idxFeat));
                stepwiseFeatures.addAll(addedFeatures);
                FileUtil.dumpToFile(stepwiseFeatures, "./configSBUProcRel/features");

                for (int idxFold = 1; idxFold <= crossValidation; idxFold++) {
                    File trainFoldDir = new File(outputDir.concat("/fold-").concat("" + idxFold).concat("/train"));
                    File testFoldDir = new File(outputDir.concat("/fold-").concat("" + idxFold).concat("/test"));
                    SBURoleTrain trainer = new SBURoleTrain(trainFoldDir.getAbsolutePath().concat("/train.ser"), isMultiClass);
                    trainer.train(trainFoldDir.getAbsolutePath());

                    SBURolePredict predict = new SBURolePredict(trainFoldDir.getAbsolutePath(), testFoldDir.getAbsolutePath().concat("/test.arggold.ser"), isMultiClass);
                    predict.performPrediction(testFoldDir.getAbsolutePath().concat("/test.arggold.ser"));

                    ArrayList<Sentence> predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testFoldDir.getAbsolutePath().concat("/test.argpredict.ser"));
                    Map<String, List<Sentence>> groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));

                    ArrayList<JSONData> jsonData = SentenceUtil.generateJSONData(groupByProcess);
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlout.json"), false);
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.srlpredict.json"), true);
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.ilppredict.json"), true); // dummy
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.semaforpredict.json"), true);// dummy
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.easysrlpredict.json"), true);// dummy
                    SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.fgpredict.json"), true);// dummy

                }
                // copy all data to ILP's data folder
                // cp -r outputDir /home/slouvan/NetBeansProjects/ILP/data/
                copyAndEval(outputDir);
                String[] lines = FileUtil.readLinesFromFile("/home/slouvan/NetBeansProjects/ILP/stats.txt");
                double currentF1 = Double.parseDouble(lines[0].split("\t")[2]);
                if (currentF1 > maxF1) {
                    maxF1 = currentF1;
                    currentBestFeat = addedFeatures;
                    metricsBest = lines;
                }

                stepwiseFeatures.removeAll(addedFeatures);
            }
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(GlobalV.PROJECT_DIR + "/additionNew.txt", true)));
            out.println((new Date()).toString() + " Best features at this stage is  " + currentBestFeat);
            out.println("Eval : " + Arrays.toString(metricsBest));
            stepwiseFeatures.addAll(currentBestFeat);
            out.println("All current features :" + stepwiseFeatures);
            out.close();
        }
    }

    public void copyAndEval(String outputDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "/home/slouvan/NetBeansProjects/SRL-Integrated/script/cpDir.sh", outputDir,
                    "/home/slouvan/NetBeansProjects/ILP/data/");
            //pb.environment().put("param1", )
            Process p = pb.start();     // Start the process.
            p.waitFor();                // Wait for the process to finish.
            StdUtil.printOutput(p);

            pb = new ProcessBuilder("/usr/bin/python", "/home/slouvan/NetBeansProjects/ILP/evaluate.py");
            p = pb.start();             // Start the process.
            p.waitFor();                // Wait for the process to finish.
            StdUtil.printOutput(p);

            System.out.println("Script executed successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void distributeCrossValidationData(String outputDir, int nbFold) throws IOException {
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
            // ==============================================   SEMAFOR ==============================================================================================================================================
            SpockDataReader.generateSEMAFORFrameAnnotation(trainingData, outputDir.concat("/fold-" + i).concat("/train/cv." + i + ".train.sentences.frame.sbu.elements"),
                    outputDir.concat("/fold-" + i).concat("/train/cv." + i + ".train.sentence.sbu"), semOffset); // DUMP REQUIRED DATA FOR SEMAFOR
            SpockDataReader.dumpRawSentences(testingData, outputDir.concat("/fold-" + i).concat("/test/cv." + i + ".test.sentence.sbu"));
            SpockDataReader.dumpSentenceLexTargetIdxs(testingData, outputDir.concat("/fold-" + i).concat("/test/cv." + i + ".test.process.target"));
            // EXECUTE ./runMalt.sh here
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        MALT_PARSER_PATH, outputDir.concat("/fold-" + i).concat("/train/cv." + i + ".train.sentence.sbu"),
                        outputDir.concat("/fold-" + i).concat("/train"));
                //pb.environment().put("param1", )
                Process p = pb.start();     // Start the process.
                p.waitFor();                // Wait for the process to finish.
                StdUtil.printOutput(p);
                System.out.println("Script executed successfully");
                AllAnnotationsMergingWithoutNE.mergeAllAnnotations(outputDir.concat("/fold-" + i).concat("/train/tokenized"),
                        outputDir.concat("/fold-" + i).concat("/train/conll"),
                        outputDir.concat("/fold-" + i).concat("/train/tmp"),
                        outputDir.concat("/fold-" + i).concat("/train/cv." + i + ".train.sentences.all.lemma.tags.sbu"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            // ==============================================   SEMAFOR ==========================================================================================
            startIdx = endIdx + 1;
            endIdx = endIdx + (sentences.size() / nbFold - 1);
        }
    }

    public void generateDevSet(String outputDir, int nbFold, List<String> processes) throws FileNotFoundException, IOException {

        sentences = (ArrayList<Sentence>) sentences.stream().filter(s -> processes.contains(s.getProcessName())).collect(Collectors.toList());
        Map<String, List<Sentence>> processSentPair = sentences.stream().collect(Collectors.groupingBy(s -> s.getProcessName()));
        int partitionSize = sentences.size() / nbFold;
        int blockSize = 0;
        int currentFoldCnt = 1;

        ArrayList<Sentence> trainingData = new ArrayList<Sentence>();
        ArrayList<Sentence> testingData = new ArrayList<Sentence>();
        HashMap<String, String> testProcessName = new HashMap<String, String>();
        HashMap<String, String> trainingProcessName = new HashMap<String, String>();
        for (String testingProcess : processSentPair.keySet()) {
            System.out.println("Process " + testingProcess + " Nb Sentence :" + processSentPair.get(testingProcess).size());
            // if foldNumber is equal to totalFold then
            // keep adding to testData
            if (currentFoldCnt == nbFold) {
                System.out.println("Processing last fold");
                testingData.addAll(processSentPair.get(testingProcess));
                testProcessName.put(testingProcess, testingProcess);
            } // if the block counter still less than partition size AND foldNumber is less than totalFold
            // keep adding to testingData
            else if (blockSize < partitionSize && currentFoldCnt < nbFold) {
                System.out.println("Has not reached the boundary, keep adding testing data");
                blockSize += processSentPair.get(testingProcess).size();
                testingData.addAll(processSentPair.get(testingProcess));
                testProcessName.put(testingProcess, testingProcess);
                System.out.println("BLOCK SIZE : " + blockSize);
            } else {
                System.out.println("Boundary reached, get the training data and flush everything");
                for (String trainingProcess : processSentPair.keySet()) {
                    if (testProcessName.get(trainingProcess) == null) {
                        trainingData.addAll(processSentPair.get(trainingProcess));
                        trainingProcessName.put(trainingProcess, trainingProcess);
                    }
                }
                System.out.println("Flushing fold " + currentFoldCnt);
                // serialize training & testing processes
                String trainingProcessesStr = Joiner.on("\t").join(trainingProcessName.keySet().iterator());
                String testingProcessessStr = Joiner.on("\t").join(testProcessName.keySet().iterator());
                FileUtil.dumpToFile(trainingProcessesStr, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/train_process_name"));
                FileUtil.dumpToFile(testingProcessessStr, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/test_process_name"));
                System.out.println("Nb Sentence in train" + trainingData.size());
                System.out.println("Nb Sentence in test" + testingData.size());
                FileUtil.serializeToFile(trainingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/train.ser"));

                // ==============================================   SEMAFOR ==============================================================================================================================================
                // ============================================================================================================================================================================================
                SpockDataReader.generateSEMAFORFrameAnnotation(trainingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentences.frame.elements.sbu"),
                        outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentence.sbu"), semOffset); // DUMP REQUIRED DATA FOR SEMAFOR
                SpockDataReader.dumpRawSentences(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/cv." + currentFoldCnt + ".test.sentence.sbu"));
                SpockDataReader.dumpSentenceLexTargetIdxs(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/cv." + currentFoldCnt + ".test.process.target"));
                // EXECUTE ./runMalt.sh here
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            MALT_PARSER_PATH, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentence.sbu"),
                            outputDir.concat("/fold-" + currentFoldCnt).concat("/train"));
                    //pb.environment().put("param1", )
                    Process p = pb.start();     // Start the process.
                    p.waitFor();                // Wait for the process to finish.
                    StdUtil.printOutput(p);
                    System.out.println("Script executed successfully");
                    AllAnnotationsMergingWithoutNE.mergeAllAnnotations(outputDir.concat("/fold-" + currentFoldCnt).concat("/train/tokenized"),
                            outputDir.concat("/fold-" + currentFoldCnt).concat("/train/conll"),
                            outputDir.concat("/fold-" + currentFoldCnt).concat("/train/tmp"),
                            outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentences.all.lemma.tags.sbu"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // ============================================================================================================================================================================================
                // ==============================================   END OF SEMAFOR ==========================================================================================

                FileUtil.serializeToFile(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/test.arggold.ser"));
                trainingData.clear();
                testingData.clear();

                blockSize = 0;
                currentFoldCnt++;
                testProcessName.clear();
                trainingProcessName.clear();

            }
        }

        // handle for the last fold""
        for (String trainingProcess : processSentPair.keySet()) {
            if (testProcessName.get(trainingProcess) == null) {
                trainingData.addAll(processSentPair.get(trainingProcess));
                trainingProcessName.put(trainingProcess, trainingProcess);
            }
        }
        // serialize training & testing processes
        System.out.println("Flushing fold " + currentFoldCnt);
        String trainingProcessesStr = Joiner.on("\t").join(trainingProcessName.keySet().iterator());
        String testingProcessessStr = Joiner.on("\t").join(testProcessName.keySet().iterator());
        FileUtil.dumpToFile(trainingProcessesStr, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/train_process_name"));
        FileUtil.dumpToFile(testingProcessessStr, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/test_process_name"));
        System.out.println("Nb Sentence in train" + trainingData.size());
        System.out.println("Nb Sentence in test" + testingData.size());
        FileUtil.serializeToFile(trainingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/train.ser"));

        // ==============================================   SEMAFOR ==============================================================================================================================================
        // ============================================================================================================================================================================================
        SpockDataReader.generateSEMAFORFrameAnnotation(trainingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentences.frame.elements.sbu"),
                outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentence.sbu"), semOffset); // DUMP REQUIRED DATA FOR SEMAFOR
        SpockDataReader.dumpRawSentences(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/cv." + currentFoldCnt + ".test.sentence.sbu"));
        SpockDataReader.dumpSentenceLexTargetIdxs(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/cv." + currentFoldCnt + ".test.process.target"));
        // EXECUTE ./runMalt.sh here
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    MALT_PARSER_PATH, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentence.sbu"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train"));
            //pb.environment().put("param1", )
            Process p = pb.start();     // Start the process.
            p.waitFor();                // Wait for the process to finish.
            StdUtil.printOutput(p);
            System.out.println("Script executed successfully");
            AllAnnotationsMergingWithoutNE.mergeAllAnnotations(outputDir.concat("/fold-" + currentFoldCnt).concat("/train/tokenized"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train/conll"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train/tmp"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentences.all.lemma.tags.sbu"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // ============================================================================================================================================================================================
        // ==============================================   END OF SEMAFOR ==========================================================================================

        FileUtil.serializeToFile(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/test.arggold.ser"));
    }

    public void distributeCrossValidationByProcess(String outputDir, int nbFold) throws FileNotFoundException, IOException, InterruptedException {
        // 
        Map<String, List<Sentence>> processSentPair = sentences.stream().collect(Collectors.groupingBy(s -> s.getProcessName()));
        int partitionSize = sentences.size() / nbFold;
        int blockSize = 0;
        int currentFoldCnt = 1;
        Thread.sleep(10000);
        System.out.println("Total sentences : " + sentences.size());
        ArrayList<Sentence> trainingData = new ArrayList<Sentence>();
        ArrayList<Sentence> testingData = new ArrayList<Sentence>();
        HashMap<String, String> testProcessName = new HashMap<String, String>();
        HashMap<String, String> trainingProcessName = new HashMap<String, String>();
        for (String testingProcess : processSentPair.keySet()) {
            System.out.println("Process " + testingProcess + " Nb Sentence :" + processSentPair.get(testingProcess).size());
            // if foldNumber is equal to totalFold then
            // keep adding to testData
            if (currentFoldCnt == nbFold) {
                System.out.println("Processing last fold");
                testingData.addAll(processSentPair.get(testingProcess));
                testProcessName.put(testingProcess, testingProcess);
            } // if the block counter still less than partition size AND foldNumber is less than totalFold
            // keep adding to testingData
            else if (blockSize < partitionSize && currentFoldCnt < nbFold) {
                System.out.println("Has not reached the boundary, keep adding testing data");
                blockSize += processSentPair.get(testingProcess).size();
                testingData.addAll(processSentPair.get(testingProcess));
                testProcessName.put(testingProcess, testingProcess);
                System.out.println("BLOCK SIZE : " + blockSize);
            } else {
                System.out.println("Boundary reached, get the training data and flush everything");
                for (String trainingProcess : processSentPair.keySet()) {
                    if (testProcessName.get(trainingProcess) == null) {
                        trainingData.addAll(processSentPair.get(trainingProcess));
                        trainingProcessName.put(trainingProcess, trainingProcess);
                    }
                }
                System.out.println("Flushing fold " + currentFoldCnt);
                // serialize training & testing processes
                String trainingProcessesStr = Joiner.on("\t").join(trainingProcessName.keySet().iterator());
                String testingProcessessStr = Joiner.on("\t").join(testProcessName.keySet().iterator());
                FileUtil.dumpToFile(trainingProcessesStr, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/train_process_name"));
                FileUtil.dumpToFile(testingProcessessStr, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/test_process_name"));
                System.out.println("Nb Sentence in train" + trainingData.size());
                System.out.println("Nb Sentence in test" + testingData.size());
                FileUtil.serializeToFile(trainingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/train.ser"));

                // ==============================================   SEMAFOR ==============================================================================================================================================
                // ============================================================================================================================================================================================
                SpockDataReader.generateSEMAFORFrameAnnotation(trainingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentences.frame.elements.sbu"),
                        outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentence.sbu"), semOffset); // DUMP REQUIRED DATA FOR SEMAFOR
                SpockDataReader.dumpRawSentences(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/cv." + currentFoldCnt + ".test.sentence.sbu"));
                SpockDataReader.dumpSentenceLexTargetIdxs(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/cv." + currentFoldCnt + ".test.process.target"));
                // EXECUTE ./runMalt.sh here
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            MALT_PARSER_PATH, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentence.sbu"),
                            outputDir.concat("/fold-" + currentFoldCnt).concat("/train"));
                    //pb.environment().put("param1", )
                    Process p = pb.start();     // Start the process.
                    p.waitFor();                // Wait for the process to finish.
                    StdUtil.printOutput(p);
                    System.out.println("Script executed successfully");
                    AllAnnotationsMergingWithoutNE.mergeAllAnnotations(outputDir.concat("/fold-" + currentFoldCnt).concat("/train/tokenized"),
                            outputDir.concat("/fold-" + currentFoldCnt).concat("/train/conll"),
                            outputDir.concat("/fold-" + currentFoldCnt).concat("/train/tmp"),
                            outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentences.all.lemma.tags.sbu"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // ============================================================================================================================================================================================
                // ==============================================   END OF SEMAFOR ==========================================================================================

                FileUtil.serializeToFile(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/test.arggold.ser"));
                trainingData.clear();
                testingData.clear();
                blockSize = 0;
                currentFoldCnt++;
                testProcessName.clear();
                trainingProcessName.clear();
            }
        }

        // handle for the last fold""
        for (String trainingProcess : processSentPair.keySet()) {
            if (testProcessName.get(trainingProcess) == null) {
                trainingData.addAll(processSentPair.get(trainingProcess));
                trainingProcessName.put(trainingProcess, trainingProcess);
            }
        }
        // serialize training & testing processes
        System.out.println("Flushing fold " + currentFoldCnt);
        String trainingProcessesStr = Joiner.on("\t").join(trainingProcessName.keySet().iterator());
        String testingProcessessStr = Joiner.on("\t").join(testProcessName.keySet().iterator());
        FileUtil.dumpToFile(trainingProcessesStr, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/train_process_name"));
        FileUtil.dumpToFile(testingProcessessStr, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/test_process_name"));
        System.out.println("Nb Sentence in train" + trainingData.size());
        System.out.println("Nb Sentence in test" + testingData.size());
        FileUtil.serializeToFile(trainingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/train.ser"));

        // ==============================================   SEMAFOR ==============================================================================================================================================
        // ============================================================================================================================================================================================
        SpockDataReader.generateSEMAFORFrameAnnotation(trainingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentences.frame.elements.sbu"),
                outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentence.sbu"), semOffset); // DUMP REQUIRED DATA FOR SEMAFOR
        SpockDataReader.dumpRawSentences(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/cv." + currentFoldCnt + ".test.sentence.sbu"));
        SpockDataReader.dumpSentenceLexTargetIdxs(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/cv." + currentFoldCnt + ".test.process.target"));
        // EXECUTE ./runMalt.sh here
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    MALT_PARSER_PATH, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentence.sbu"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train"));
            //pb.environment().put("param1", )
            Process p = pb.start();     // Start the process.
            p.waitFor();                // Wait for the process to finish.
            StdUtil.printOutput(p);
            System.out.println("Script executed successfully");
            AllAnnotationsMergingWithoutNE.mergeAllAnnotations(outputDir.concat("/fold-" + currentFoldCnt).concat("/train/tokenized"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train/conll"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train/tmp"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentences.all.lemma.tags.sbu"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // ============================================================================================================================================================================================
        // ==============================================   END OF SEMAFOR ==========================================================================================

        FileUtil.serializeToFile(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/test.arggold.ser"));
    }

    public void distributeTrainTest() throws FileNotFoundException, IOException, InterruptedException, ClassNotFoundException {
        //sentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile("./data/training_4_roles.ser");
        Map<String, List<Sentence>> processSentPair = sentences.stream().collect(Collectors.groupingBy(s -> s.getProcessName()));
        int blockSize = 0;
        int currentFoldCnt = 1;
        Thread.sleep(10000);
        System.out.println("Total sentences : " + sentences.size());
        ArrayList<Sentence> trainingData = new ArrayList<Sentence>();
        ArrayList<Sentence> testingData = new ArrayList<Sentence>();
        HashMap<String, String> testProcessName = new HashMap<String, String>();
        HashMap<String, String> trainingProcessName = new HashMap<String, String>();
        for (String trainingProcess : processSentPair.keySet()) {
            if (testProcessName.get(trainingProcess) == null) {
                trainingData.addAll(processSentPair.get(trainingProcess));
                trainingProcessName.put(trainingProcess, trainingProcess);
            }
        }
        // serialize training & testing processes
        String trainingProcessesStr = Joiner.on("\t").join(trainingProcessName.keySet().iterator());
        FileUtil.dumpToFile(trainingProcessesStr, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/train_process_name"));
        System.out.println("Nb Sentence in train" + trainingData.size());
        FileUtil.serializeToFile(trainingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/train.ser"));
        // ==============================================   SEMAFOR ==============================================================================================================================================
        // ============================================================================================================================================================================================
        SpockDataReader.generateSEMAFORFrameAnnotation(trainingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentences.frame.elements.sbu"),
                outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentence.sbu"), semOffset); // DUMP REQUIRED DATA FOR SEMAFOR

        // ============================================== TESTING =======================================================================
        
        SpockDataReader testDataReader = new SpockDataReader(testingFileName, configFileName, true);
        testDataReader.readData();
        ArrayList<Sentence> testingSentences = testDataReader.getSentences();//= (ArrayList<Sentence>)FileUtil.deserializeFromFile("/home/slouvan/NetBeansProjects/SRL-Integrated/thousand_sentences.ser");
        FileUtil.serializeToFile(testingSentences, "/home/slouvan/NetBeansProjects/SRL-Integrated/thousand_sentences.ser");
        Map<String, List<Sentence>> testProcessSentPair = testingSentences.stream().collect(Collectors.groupingBy(s -> s.getProcessName()));
        for (String testingProcess : testProcessSentPair.keySet()) {
            testProcessName.put(testingProcess, testingProcess);
            testingData.addAll(testProcessSentPair.get(testingProcess));
        }
        String testingProcessessStr = Joiner.on("\t").join(testProcessName.keySet().iterator());
        System.out.println("Nb Sentence in test" + testingData.size());

        FileUtil.dumpToFile(testingProcessessStr, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/test_process_name"));
        SpockDataReader.dumpRawSentences(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/cv." + currentFoldCnt + ".test.sentence.sbu"));
        SpockDataReader.dumpSentenceLexTargetIdxs(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/cv." + currentFoldCnt + ".test.process.target"));
        // EXECUTE ./runMalt.sh here
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    MALT_PARSER_PATH, outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentence.sbu"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train"));
            //pb.environment().put("param1", )
            Process p = pb.start();     // Start the process.
            p.waitFor();                // Wait for the process to finish.
            StdUtil.printOutput(p);
            System.out.println("Script executed successfully");
            AllAnnotationsMergingWithoutNE.mergeAllAnnotations(outputDir.concat("/fold-" + currentFoldCnt).concat("/train/tokenized"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train/conll"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train/tmp"),
                    outputDir.concat("/fold-" + currentFoldCnt).concat("/train/cv." + currentFoldCnt + ".train.sentences.all.lemma.tags.sbu"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ============================================================================================================================================================================================
        // ==============================================   END OF SEMAFOR ==========================================================================================
        FileUtil.serializeToFile(testingData, outputDir.concat("/fold-" + currentFoldCnt).concat("/test/test.arggold.ser"));
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
        if (isMultiClass) {
            trainer.trainMultiClassClassifier(trainFoldDir.getAbsolutePath());
        } else {
            trainer.trainBinaryClassifier(trainFoldDir.getAbsolutePath());
        }
        FileUtil.serializeToFile(trainingData, outputDir.concat("/fold-1").concat("/train/train.ser"));
        SBURolePredict predict = new SBURolePredict(trainFoldDir.getAbsolutePath(), testFoldDir.getAbsolutePath().concat("/test.arggold.ser"), isMultiClass);
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
        if (isMultiClass) {
            trainer.trainMultiClassClassifier(trainDir);
        } else {
            trainer.trainBinaryClassifier(trainDir);
        }

        FileUtil.serializeToFile(trainData, trainDir.concat("/train.ser"));
        SBURolePredict predict = new SBURolePredict(trainDir, testDir.concat("/test.arggold.ser"), isMultiClass);
        predict.performPrediction(testDir.concat("/test.arggold.ser"));
        ArrayList<Sentence> predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testDir.concat("/test.argpredict.ser"));
        Map<String, List<Sentence>> groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));

        ArrayList<JSONData> jsonData = SentenceUtil.generateJSONData(groupByProcess);
        SentenceUtil.flushDataToJSON(jsonData, testDir.concat("/test.srlout.json"), false);
        SentenceUtil.flushDataToJSON(jsonData, testDir.concat("/test.srlpredict.json"), true);
    }

    public void knowledgeExtractor() throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        boolean dirCreated = FileUtil.mkDir(outputDir);
        dirCreated = FileUtil.mkDir(outputDir.concat("/train"));
        dirCreated = FileUtil.mkDir(outputDir.concat("/test"));
        if (dirCreated) // this is not a good checking, leave it for now
        {
            // TRAINING
            sentences = (ArrayList<Sentence>) sentences.stream().filter(data -> data.isAnnotated()).collect(Collectors.toList());
            FileUtil.serializeToFile(sentences, outputDir.concat("/train/train.ser"));
            SBURoleTrain trainer = new SBURoleTrain(outputDir.concat("/train/train.ser"), isMultiClass);
            trainer.train(outputDir.concat("/train"));
            FileUtil.serializeToFile(sentences, outputDir.concat("/train/train.ser"));

            // Read the knowledge sentences using SPOCK data reader
            SpockDataReader reader = new SpockDataReader(testingFileName, configFileName, true); // process, config, is testing
            reader.readData();
            ArrayList<Sentence> testSentences = reader.getSentences();
            FileUtil.serializeToFile(testSentences, outputDir.concat("/test/test.ser"));
            SBURolePredict predict = new SBURolePredict(outputDir.concat("/train"), outputDir.concat("/test/test.ser"), isMultiClass);
            predict.knownAnnotation = false;
            predict.performPrediction(outputDir.concat("/test/test.ser"));
            ArrayList<Sentence> predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(outputDir.concat("/test/predict.ser"));
            Map<String, List<Sentence>> groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));
            ArrayList<JSONData> jsonData = SentenceUtil.generateJSONData(groupByProcess);
            SentenceUtil.flushDataToJSON(jsonData, outputDir.concat("/test/srlpredict.json"), true);
        }
    }

    public void doMain() throws CmdLineException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
        try {
            /*dataReader = new SpockDataReader(trainingFileName, configFileName, false);
             dataReader.readData();
             sentences = dataReader.getSentences();*/
            sentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile("./data/training_4_roles.ser");

            if (crossValidation != -1 && !devSet && !featAddition && !extractorMode && !trainTest) {
                try {
                    sentences = (ArrayList<Sentence>) sentences.stream().filter(data -> data.isAnnotated()).collect(Collectors.toList());
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

            } else if (featAddition && crossValidation != -1) {
                doFeatureAddition();
            } else if (devSet) {
                File processFile = new File(devProcessFileName);
                String[] lines = FileUtil.readLinesFromFile(devProcessFileName);
                String[] processes = lines[0].split("\t");
                List<String> processesList = new ArrayList<String>(Arrays.asList(processes));
                setupCrossValidationEnvironment(outputDir, crossValidation);
                generateDevSet(outputDir, crossValidation, processesList);
                performCrossValidation(outputDir, crossValidation);
            } else if (trainTest) {
               // try {
                    //trainAndTest("/home/slouvan/NetBeansProjects/SRL-Integrated/data/sandbox-60-40/train",
                    //        "/home/slouvan/NetBeansProjects/SRL-Integrated/data/sandbox-60-40/test");
                    setupCrossValidationEnvironment(outputDir, crossValidation); // setting up the directory
                    //distributeCrossValidationData(outputDir, crossValidation);
                    distributeTrainTest();
                    //performCrossValidation(outputDir, crossValidation);
            

               /* } catch (ClassNotFoundException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
                }*/
            } else if (extractorMode) {
                System.out.println("Extractor mode");
                try {
                    knowledgeExtractor();
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
            } else {
                try {
                    sentences = (ArrayList<Sentence>) sentences.stream().filter(data -> data.isAnnotated()).collect(Collectors.toList());
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

    public static void main(String[] args) throws InterruptedException {
        try {
            ArgumentClassifier classifier = new ArgumentClassifier();
            CmdLineParser parser = new CmdLineParser(classifier);
            parser.parseArgument(args);
            classifier.doMain();
        } catch (CmdLineException ex) {
            Logger.getLogger(ArgumentClassifier.class.getName()).log(Level.SEVERE, null, ex);
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
}
