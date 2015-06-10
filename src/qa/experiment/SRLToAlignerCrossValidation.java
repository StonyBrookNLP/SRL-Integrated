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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.QuestionData;
import qa.QuestionDataProcessor;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan 5 fold cross validation
 */
public class SRLToAlignerCrossValidation {

    private String questionFileTsv;
    private String processFileTsv;
    private String dsFileTsv;
    private boolean dsMode;
    QuestionDataProcessor qProc;
    ProcessFrameProcessor manualData;
    ProcessFrameProcessor dsData;

    public SRLToAlignerCrossValidation(String questionFileTsv, String processFileTsv, String dsFileTsv) {
        this.questionFileTsv = questionFileTsv;
        this.processFileTsv = processFileTsv;
        this.dsFileTsv = dsFileTsv;
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

    public ArrayList<ProcessFrame> extractProcessFromQuestions(ArrayList<QuestionData> questions) {
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
            for (int i = 0; i < processNames.size(); i++) {
                ArrayList<ProcessFrame> selectedProcess = dsData.getProcessFrameByName(processNames.get(i));
                if (selectedProcess.size() > 0) {
                    extractedProcessFrame.addAll(selectedProcess);
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

    public void produceCrossValidation(int fold, String outDir, boolean ds) throws FileNotFoundException, IOException, ClassNotFoundException {
        qProc = new QuestionDataProcessor(questionFileTsv);
        qProc.loadQuestionData();
        manualData = new ProcessFrameProcessor(processFileTsv);
        manualData.loadProcessData();
        dsData = new ProcessFrameProcessor(dsFileTsv);
        dsData.loadProcessData();
        String[] masterQuestions = FileUtil.readLinesFromFile(questionFileTsv);
        dsMode = ds;
        ArrayList<QuestionData> questionsData = qProc.getQuestionData();
        int testSize = questionsData.size() / fold;
        int startIdxTest = 0;
        int endIdxTest = startIdxTest + testSize;
        for (int i = 0; i < fold; i++) {

            List<QuestionData> testingDataList = questionsData.subList(startIdxTest, endIdxTest);
            ArrayList<QuestionData> testQuestion = new ArrayList<QuestionData>(testingDataList.size());
            dumpTestQuestions(masterQuestions, outDir + "question.list.cv." + i + ".tsv", startIdxTest, endIdxTest);
            testQuestion.addAll(testingDataList);
            ArrayList<QuestionData> trainingQuestion = getTrainingData(startIdxTest, endIdxTest, questionsData);
            // extract processes for training
            ArrayList<ProcessFrame> trainingProcesses = extractProcessFromQuestions(trainingQuestion);
            // extract processes for testing
            ArrayList<ProcessFrame> testingProcesses = extractProcessFromQuestions(testQuestion);

            String trainingFileName = outDir + "question.train.cv." + i + ".clearparser";
            String testFileName = outDir + "question.test.cv." + i + ".clearparser";
            String modelName = outDir + "question.model.cv." + i + ".model";

            ProcessFrameUtil.toClearParserFormat(trainingProcesses, trainingFileName);
            ProcessFrameUtil.toClearParserFormat(testingProcesses, testFileName);

            //System.exit(0);
            SRLTrain train = new SRLTrain();
            CmdLineParser cmd = new CmdLineParser(train);
            // build model
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

            String predictionFileName = outDir + "question.predict.cv." + i + ".clearparser";
            ClearParserUtil.PREDICT_ARGS[3] = testFileName;
            ClearParserUtil.PREDICT_ARGS[5] = predictionFileName;  //PREDICT.CLEARPARSER
            ClearParserUtil.PREDICT_ARGS[7] = modelName;
            new SRLPredict(ClearParserUtil.PREDICT_ARGS);

            String predictionTSV = outDir + "question.predict.cv." + i + ".tsv"; // PREDICT.TSV
            ProcessFrameUtil.dumpFramesToFile(testingProcesses, predictionTSV);
            String labeledFramesFileName = outDir + "frames.cv." + i + ".tsv";
            new SRLToAligner().generateTsvForAligner(predictionTSV, predictionFileName, labeledFramesFileName);
            startIdxTest = endIdxTest;
            endIdxTest = startIdxTest + testSize;
            if (endIdxTest > questionsData.size()) {
                endIdxTest = questionsData.size();
            }

        }
    }

    public static void main(String[] arg) throws IOException, FileNotFoundException, ClassNotFoundException {
        SRLToAlignerCrossValidation crossVal = new SRLToAlignerCrossValidation(GlobalVariable.PROJECT_DIR + "/data/questions_10_june.tsv",
                GlobalVariable.PROJECT_DIR + "/data/process_frame_june.tsv",
                GlobalVariable.PROJECT_DIR + "/data/ds_most_frequent_7_06_2015/ds_all_processes_w_pattern.tsv");
        crossVal.produceCrossValidation(5, GlobalVariable.PROJECT_DIR + "/data/SRLQAPipe/", false);
    }
}
