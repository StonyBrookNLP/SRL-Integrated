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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.QuestionData;
import qa.QuestionDataProcessor;
import qa.srl.SRLWrapper;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan 5 fold cross validation
 */
public class SRLToAlignerCrossValidation {

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

    public SRLToAlignerCrossValidation() {

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

    public void produceCrossValidation() throws FileNotFoundException, IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
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
            

            String trainingFileName = outDir + "/question.train.cv." + i + ".clearparser";
            String testFileName = outDir + "/question.test.cv." + i + ".clearparser";
            String modelName = outDir + "/question.model.cv." + i + ".model";

            // Convert to SRL format
            ProcessFrameUtil.toParserFormat(trainingProcesses, trainingFileName, 1); // processes in the training question
            ProcessFrameUtil.toParserFormat(testingProcesses, testFileName, 1);      // processes in the testing question 
            
            // Find intersection between training and testing processes 
            
            //ProcessFrameUtil.toClearParserFormat(trainingProcesses, trainingFileName);
            //ProcessFrameUtil.toClearParserFormat(testingProcesses, testFileName);

            new SRLWrapper().doTrain(trainingFileName, modelName, 1, false); // do the training, this is basically combined model
            
            
            ArrayList<ProcessFrame> arrQFrames = new ArrayList<ProcessFrame>();
            // Get the gold frame of the question (tsv)
            for (int j = 0; j < testQuestion.size(); j++)
            {
                QuestionData qData = testQuestion.get(j);
                String rawQuestionSentences = qData.getQuestionSentence();
                String[] sents = rawQuestionSentences.split("\\.");
                
                for (String sent : sents)
                {
                        arrQFrames.addAll(qFrames.getQuestionFrame(sent));
                }
                if (arrQFrames.size() == 0)
                {
                    System.out.println("PROBLEM");
                    System.exit(0);
                }
            }
            
            // Convert it to clearparser  question.list.cv.i.clearparser
            ProcessFrameUtil.toParserFormat(arrQFrames, outDir + "/question.list.cv." + i + ".clearparser", 1);
            //ProcessFrameUtil.toClearParserFormat(arrQFrames, outDir + "/question.list.cv." + i + ".clearparser");
            // apply SRL model to the question.list.cv.i.clearparser save it to question.list.cv.i.predict.clearparser
            String questionFramepredictionFileName = outDir + "/question.list.predict.cv." + i + ".clearparser";
            String labeledQuestionFramesFileName = outDir + "/question.framepredict.cv." + i + ".tsv";
            
            // TODO : Convert to MATE
            ClearParserUtil.PREDICT_ARGS[3] = outDir + "/question.list.cv." + i + ".clearparser";
            ClearParserUtil.PREDICT_ARGS[5] = questionFramepredictionFileName;  //PREDICT.CLEARPARSER
            ClearParserUtil.PREDICT_ARGS[7] = modelName;
            new SRLPredict(ClearParserUtil.PREDICT_ARGS);
            
             
            
            String predictionFileName = outDir + "/question.predict.cv." + i + ".clearparser";
            // TODO : Convert to MATE
            ClearParserUtil.PREDICT_ARGS[3] = testFileName;
            ClearParserUtil.PREDICT_ARGS[5] = predictionFileName;  //PREDICT.CLEARPARSER
            ClearParserUtil.PREDICT_ARGS[7] = modelName;
            new SRLPredict(ClearParserUtil.PREDICT_ARGS);

            
            // This part create the TSV for the processes in the answer choices
            String predictionTSV = outDir + "/question.predict.cv." + i + ".tsv"; // PREICT.TSV
            ProcessFrameUtil.dumpFramesToFile(testingProcesses, predictionTSV);
            String labeledFramesFileName = outDir + "/frames.cv." + i + ".tsv";
            new SRLToAligner().generateTsvForAligner(predictionTSV, predictionFileName, labeledFramesFileName);
            
            // This part creates the TSV for the question frames in the particular fold
            //Convert question.list.cv.i.predict.clearparser to question.frame.cv.0.tsv
            String questionFrameGoldTSV = outDir + "/question.framegold.cv." + i + ".tsv"; // PREDICT.TSV
            ProcessFrameUtil.dumpFramesToFile(arrQFrames, questionFrameGoldTSV);
            new SRLToAligner().generateTsvForAligner(questionFrameGoldTSV, questionFramepredictionFileName, labeledQuestionFramesFileName);
            
            startIdxTest = endIdxTest;
            endIdxTest = startIdxTest + testSize;
            
            if (i  == fold - 2 )
            {
                endIdxTest = questionsData.size();
            }
        }
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException {

        SRLToAlignerCrossValidation srlExp = new SRLToAlignerCrossValidation();
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
}
