/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import Util.ClearParserUtil;
import Util.GlobalVariable;
import Util.ProcessFrameUtil;
import clear.engine.SRLPredict;
import clear.engine.SRLTrain;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.QuestionData;
import qa.QuestionDataProcessor;
import qa.dep.DependencyNode;
import qa.srl.SRLWrapper;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan 5 fold cross validation
 */
public class SRLToAlignerSplit {

    @Option(name = "-f", usage = "process file", required = true, metaVar = "REQUIRED")
    private String processFileTsv;

    @Option(name = "-q", usage = "question file", required = true, metaVar = "REQUIRED")
    private String questionFileTsv;

    @Option(name = "-qF", usage = "question frame file", required = true, metaVar = "REQUIRED")
    private String questionFrameFileTsv;

    @Option(name = "-ds", usage = "dsfile", required = false, metaVar = "OPTIONAL")
    private String dsFileTsv;

    @Option(name = "-k", usage = "number of fold", required = true, metaVar = "REQUIRED")
    private int fold;

    @Option(name = "-o", usage = "output directory name", required = true, metaVar = "REQUIRED")
    private String outDir;

    @Option(name = "-mx", usage = "the training data is mixed between annotated data and DS", required = false, metaVar = "OPTIONAL")
    private boolean dsMode = false;

    @Option(name = "-mxt", usage = "the training data is mixed between annotated data and DS", required = false, metaVar = "OPTIONAL")
    private boolean dsModeTest = false;

    QuestionDataProcessor qProc;
    ProcessFrameProcessor manualData;
    ProcessFrameProcessor dsData;

    public SRLToAlignerSplit() {

    }

    public ArrayList<QuestionData> getTrainingData(int startIdx, int endIdxTest, ArrayList<QuestionData> questions) {
        ArrayList<QuestionData> trainingData = new ArrayList<QuestionData>();
        for (int i = 0; i < startIdx; i++) {
            trainingData.add(questions.get(i));
        }
        for (int i = endIdxTest; i < questions.size(); i++) {
            trainingData.add(questions.get(i));
        }

        return trainingData;
    }

    public void init() {
        File outDirHandler = new File(outDir);
        if (outDirHandler.exists()) {
            return;
        }
        boolean success = outDirHandler.mkdir();

        if (!success) {
            System.out.println("FAILED to create output directory");
            System.exit(0);
        }
    }

    public ArrayList<ProcessFrame> extractProcessFromQuestions(ArrayList<QuestionData> questions, String mode) {
        ArrayList<String> processNames = new ArrayList<String>();
        ArrayList<ProcessFrame> extractedProcessFrame = new ArrayList<ProcessFrame>();

        for (int i = 0; i < questions.size(); i++) {
            String[] choices = questions.get(i).getAnswers();
            for (String choice : choices) {
                if (!processNames.contains(choice.trim()) && !choice.isEmpty()) {
                    processNames.add(choice.trim());
                }
            }
        }

        for (int i = 0; i < processNames.size(); i++) {
            ArrayList<ProcessFrame> selectedProcess = manualData.getProcessFrameByName(processNames.get(i));
            if (selectedProcess.size() > 0) {
                extractedProcessFrame.addAll(selectedProcess);
            }
        }
        if (dsMode) {
            if (mode.equalsIgnoreCase("training") || (mode.equalsIgnoreCase("testing") && dsModeTest)) {
                for (int i = 0; i < processNames.size(); i++) {
                    ArrayList<ProcessFrame> selectedProcess = dsData.getProcessFrameByName(processNames.get(i));
                    if (selectedProcess.size() > 0) {
                        extractedProcessFrame.addAll(selectedProcess);
                    }
                }
            }
        }
        return extractedProcessFrame;
    }

    public void dumpTestQuestions(String[] masterFile, String fileName, int startIdx, int endIdx) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        writer.println(masterFile[0]);

        for (int i = startIdx; i < endIdx; i++) {
            writer.println(masterFile[i + 1]);
        }
        writer.close();
    }

    public void produceCrossValidation() throws FileNotFoundException, IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
        qProc = new QuestionDataProcessor(questionFileTsv);
        qProc.loadQuestionData(); // Load the question , choices, correct answer
        manualData = new ProcessFrameProcessor(processFileTsv); // Load all process frame
        manualData.loadProcessData();
        if (dsMode) {
            dsData = new ProcessFrameProcessor(dsFileTsv);
            dsData.loadProcessData();
        }
        ProcessFrameProcessor qFrames = new ProcessFrameProcessor(questionFrameFileTsv); // Load all question frame
        //ProcessFrameProcessor qFrames = new ProcessFrameProcessor("./data/question_frame_23_june.tsv");
        qFrames.setQuestionFrame(true);
        qFrames.loadProcessData();
        String[] masterQuestions = FileUtil.readLinesFromFile(questionFileTsv);
        ArrayList<QuestionData> questionsData = qProc.getQuestionData();
        int testSize = questionsData.size() / fold;
        int startIdxTest = 0;
        int endIdxTest = startIdxTest + testSize;
        for (int i = 0; i < fold; i++) {
            // Get the test question
            List<QuestionData> testingDataList = questionsData.subList(startIdxTest, endIdxTest);
            ArrayList<QuestionData> testQuestion = new ArrayList<QuestionData>(testingDataList.size()); // Store test question sentence, choices and correct answer
            // Convert the test question to tsv
            dumpTestQuestions(masterQuestions, outDir + "/question.list.cv." + i + ".tsv", startIdxTest, endIdxTest);

            testQuestion.addAll(testingDataList);

            // Get the training question
            ArrayList<QuestionData> trainingQuestion = getTrainingData(startIdxTest, endIdxTest, questionsData);

            // Get process sentences from the training question
            ArrayList<ProcessFrame> trainingProcesses = extractProcessFromQuestions(trainingQuestion, "training");
            // Get process sentences from the testing question
            ArrayList<ProcessFrame> testingProcesses = extractProcessFromQuestions(testQuestion, "testing");
            // Find intersection between training and testing processes 
            ArrayList<String> sharedProcessesName = getSharedProcesses(trainingProcesses, testingProcesses);
            
            ArrayList<ProcessFrame> testingUnseenProcesses = sharedProcessesName.size() == 0?testingProcesses:getUnseenProcess(testingProcesses, sharedProcessesName);
            ArrayList<ProcessFrame> testingSeenProcesses = sharedProcessesName.size() > 0 ? getSeenProcess(testingProcesses, sharedProcessesName) :null ;

            String trainingFileName = outDir + "/question.train.cv." + i + ".parser";
            String testFileName = outDir + "/question.test.cv." + i + ".parser";
            String modelName = outDir + "/question.model.cv." + i + ".model";

            // Convert to SRL format
            ProcessFrameUtil.toParserFormat(trainingProcesses, trainingFileName, 1); // processes in the training question
            ProcessFrameUtil.toParserFormat(testingUnseenProcesses, testFileName, 1);      // processes in the testing question 

            //ProcessFrameUtil.toClearParserFormat(trainingProcesses, trainingFileName);
            //ProcessFrameUtil.toClearParserFormat(testingProcesses, testFileName);
            new SRLWrapper().doTrain(trainingFileName, modelName, 1, false); // do the training, this is basically combined model
            ArrayList<ProcessFrame> arrQFrames = new ArrayList<ProcessFrame>();
            // Get the gold frame of the question (tsv)
            for (int j = 0; j < testQuestion.size(); j++) {
                QuestionData qData = testQuestion.get(j);
                String rawQuestionSentences = qData.getQuestionSentence();
                String[] sents = rawQuestionSentences.split("\\.");

                for (String sent : sents) {
                    arrQFrames.addAll(qFrames.getQuestionFrame(sent));
                }
                if (arrQFrames.size() == 0) {
                    System.out.println("PROBLEM");
                    System.exit(0);
                }
            }

            // Convert it to clearparser  question.list.cv.i.parser
            ProcessFrameUtil.toParserFormat(arrQFrames, outDir + "/question.list.cv." + i + ".parser", 1);
            //ProcessFrameUtil.toClearParserFormat(arrQFrames, outDir + "/question.list.cv." + i + ".parser");
            // apply SRL model to the question.list.cv.i.parser save it to question.list.cv.i.predict.parser
            String questionFramepredictionFileName = outDir + "/question.list.predict.cv." + i + ".parser";
            String labeledQuestionFramesFileName = outDir + "/question.framepredict.cv." + i + ".tsv";

            
            //public void doPredict(String testInputFileName, String predictionFileName, String modelFileName, int srlType, boolean autoPi, boolean domainAdaptation)
            new SRLWrapper().doPredict(outDir + "/question.list.cv." + i + ".parser", questionFramepredictionFileName, modelName, 1, true, false);

            // For unseen process
            String predictionFileName = outDir + "/question.predict.cv." + i + ".parser";
            new SRLWrapper().doPredict(testFileName, predictionFileName, modelName, 1, true, false);
            // For seen process
            if (testingSeenProcesses != null)
                predictionSeenProcess(testingSeenProcesses, predictionFileName, trainingProcesses, 5);

            // This part create the TSV for the processes in the answer choices
            String predictionTSV = outDir + "/question.predict.cv." + i + ".tsv"; // PREICT.TSV
            ProcessFrameUtil.dumpFramesToFile(testingProcesses, predictionTSV);
            String labeledFramesFileName = outDir + "/frames.cv." + i + ".tsv";
          //  new SRLToAligner().generateTsvForAlignerMergeVersion(predictionTSV, predictionFileName, labeledFramesFileName, false);

            // This part creates the TSV for the question frames in the particular fold
            //Convert question.list.cv.i.predict.parser to question.frame.cv.0.tsv
            String questionFrameGoldTSV = outDir + "/question.framegold.cv." + i + ".tsv"; // PREDICT.TSV
            ProcessFrameUtil.dumpFramesToFile(arrQFrames, questionFrameGoldTSV);
            //new SRLToAligner().generateTsvForAlignerMergeVersion(questionFrameGoldTSV, questionFramepredictionFileName, labeledQuestionFramesFileName, true);

            startIdxTest = endIdxTest;
            endIdxTest = startIdxTest + testSize;

            if (i == fold - 2) {
                endIdxTest = questionsData.size();
            }
        }
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException {

        SRLToAlignerSplit srlExp = new SRLToAlignerSplit();
        CmdLineParser cmd = new CmdLineParser(srlExp);

        try {
            cmd.parseArgument(args);
            srlExp.init();
            srlExp.produceCrossValidation();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            cmd.printUsage(System.err);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*SRLToAlignerCrossValidation crossVal = new SRLToAlignerCrossValidation(GlobalVariable.PROJECT_DIR + "/data/questions_10_june.tsv",
         GlobalVariable.PROJECT_DIR + "/data/ds_most_frequent_7_06_2015/ds_all_processes_w_pattern.tsv");
         crossVal.produceCrossValidation();*/

        /*
         SRLToAlignerCrossValidation crossVal = new SRLToAlignerCrossValidation(GlobalVariable.PROJECT_DIR + "/data/questions_10_june.tsv",
         GlobalVariable.PROJECT_DIR + "/data/process_frame_june.tsv",
         GlobalVariable.PROJECT_DIR + "/data/ds_most_frequent_7_06_2015/ds_all_processes_w_pattern.tsv");
         crossVal.produceCrossValidation( GlobalVariable.PROJECT_DIR + "/data/SRLQAPipe/", false);
         */
    }

    public void doCrossValidation( ArrayList<ProcessFrame> seenProcessFrame, ArrayList<ProcessFrame> originalTrainingProcessFrame, int foldSize, String predictionConcatFile) throws IOException, InterruptedException, FileNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        int startIdx = 0;
        int testSize = seenProcessFrame.size() / foldSize;
        int endIdx = testSize;
        for (int currentFold = 0; currentFold < foldSize; currentFold++) {
            ArrayList<ProcessFrame> testingFrames = new ArrayList<ProcessFrame>(seenProcessFrame.subList(startIdx, endIdx));
            ArrayList<ProcessFrame> trainingFrames = new ArrayList<ProcessFrame>(seenProcessFrame.subList(0, startIdx));
            trainingFrames.addAll(new ArrayList<ProcessFrame>(seenProcessFrame.subList(endIdx, seenProcessFrame.size())));
            Predicate<ProcessFrame> isNotSameProcessName= frame -> !frame.getProcessName().equalsIgnoreCase(outDir);
            List<ProcessFrame> theRest = originalTrainingProcessFrame.stream().filter(isNotSameProcessName).collect(Collectors.toList());
            trainingFrames.addAll(new ArrayList<ProcessFrame>(theRest));
            
            
            String trainingFileName = "/tmp/train.cv."+currentFold;
            String testingFileName = "/tmp/test.cv."+currentFold;
            String modelFileName = "/tmp/model.cv."+currentFold;

            //testFilePath.add(testingFileName);
            //trainingModelFilePath.add(modelName);

            ProcessFrameUtil.toParserFormat(trainingFrames, trainingFileName, 1);
            ProcessFrameUtil.toParserFormat(testingFrames, testingFileName, 1);
            
            new SRLWrapper().doTrain(trainingFileName, modelFileName, 1, false);
            new SRLWrapper().doPredict(testingFileName, testingFileName.replace("test.", "predict."), modelFileName, 1, true, false);
            
            // Concatenate to the existing prediction file
            FileWriter fileWriter = new FileWriter(predictionConcatFile,true);
            BufferedWriter buffWriter = new BufferedWriter(fileWriter);
            
            Scanner scanner = new Scanner(new File(testingFileName.replace("test.", "predict.")));
            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                buffWriter.write(line+"\n");
            }
            
            buffWriter.close();
            fileWriter.close();
            
            startIdx = endIdx;
            if (currentFold == foldSize - 2) {
                endIdx = seenProcessFrame.size();
            } else {
                endIdx = startIdx + testSize;
            }
        }
    }
    
    
    private void predictionSeenProcess(ArrayList<ProcessFrame> testingSeenProcesses, String predictionFileName, ArrayList<ProcessFrame> originalTrainingFrame, int fold) throws IOException, InterruptedException, FileNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        HashMap<String, String> uniqueProcessName = getUniqueProcessName(testingSeenProcesses);
        int cnt = 0;
        for (String processName : uniqueProcessName.keySet())
        {
            ArrayList<ProcessFrame> frames = ProcessFrameUtil.getProcessFrameByRawName(processName, testingSeenProcesses);
            cnt += frames.size();
            Collections.shuffle(frames);
            if (frames.size() < fold)
            {
                doCrossValidation(frames, originalTrainingFrame, frames.size(), predictionFileName);
            }
            else
            {
                doCrossValidation(frames, originalTrainingFrame, 2, predictionFileName);
            }
           System.out.println(cnt+" /"+testingSeenProcesses.size());

            
        }
        System.out.println(cnt);
    }

    private HashMap<String, String> getUniqueProcessName(ArrayList<ProcessFrame> testingSeenProcesses) {
        HashMap<String, String> uniqueProcessName = new HashMap<String,String>();
        for (int i = 0; i < testingSeenProcesses.size(); i++)
        {
            uniqueProcessName.put(testingSeenProcesses.get(i).getProcessName(), "");
        }
        return uniqueProcessName;
    }

    private ArrayList<String> getSharedProcesses(ArrayList<ProcessFrame> trainingProcesses, ArrayList<ProcessFrame> testingProcesses) {
        HashMap<String, String> processNameInTraining = new HashMap<String,String>();
        HashMap<String, String> processNameInTesting = new HashMap<String,String>();
        for (int i = 0; i < trainingProcesses.size(); i++)
            processNameInTraining.put(trainingProcesses.get(i).getProcessName(), "");
        for (int i = 0; i < testingProcesses.size(); i++)
            processNameInTesting.put(testingProcesses.get(i).getProcessName(), "");
        
        ArrayList<String> sharedProcess = new ArrayList<String>();
        for (String processName : processNameInTraining.keySet())
        {
            if (processNameInTesting.get(processName) != null)
                sharedProcess.add(processName);
        }
        return sharedProcess;
    }

  

    private ArrayList<ProcessFrame> getUnseenProcess(ArrayList<ProcessFrame> testingProcesses, ArrayList<String> sharedProcessesName) {
        ArrayList<ProcessFrame> frames = new ArrayList<ProcessFrame>();
         //Predicate<ProcessFrame> isNotSameProcessName= frame -> !frame.getProcessName().equalsIgnoreCase(outDir);
          //  List<ProcessFrame> theRest = originalTrainingProcessFrame.stream().filter(isNotSameProcessName).collect(Collectors.toList());
        for (int i = 0; i < testingProcesses.size(); i++)
        {
            ProcessFrame frame = testingProcesses.get(i);
            if (!sharedProcessesName.contains(frame.getProcessName()))
                frames.add(frame);
        }
        return frames;
    }

    private ArrayList<ProcessFrame> getSeenProcess(ArrayList<ProcessFrame> testingProcesses, ArrayList<String> sharedProcessesName) {
        ArrayList<ProcessFrame> frames = new ArrayList<ProcessFrame>();
         //Predicate<ProcessFrame> isNotSameProcessName= frame -> !frame.getProcessName().equalsIgnoreCase(outDir);
          //  List<ProcessFrame> theRest = originalTrainingProcessFrame.stream().filter(isNotSameProcessName).collect(Collectors.toList());
        for (int i = 0; i < testingProcesses.size(); i++)
        {
            ProcessFrame frame = testingProcesses.get(i);
            if (sharedProcessesName.contains(frame.getProcessName()))
                frames.add(frame);
        }
        return frames;
    }
}
