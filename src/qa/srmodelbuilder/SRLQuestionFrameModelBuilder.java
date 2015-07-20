/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.srmodelbuilder;

import Util.ProcessFrameUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import qa.ProcessFrameProcessor;
import qa.experiment.SRLToAligner;
import qa.srl.SRLWrapper;

/**
 *
 * @author samuellouvan
 */
public class SRLQuestionFrameModelBuilder {

    public void generatePredictedQuestionFrame(String processFileName, String questionFrameFileName, String questionFramePredictedFileName, String processPredictedFile, String answerFramePredicted, int parserType) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ProcessFrameProcessor proc = new ProcessFrameProcessor(processFileName);
        proc.loadProcessData();
        ProcessFrameProcessor questionProc = new ProcessFrameProcessor(questionFrameFileName);
        questionProc.setQuestionFrame(true);
        questionProc.loadProcessData();

        ProcessFrameUtil.toParserFormat(proc.getProcArr(), "/tmp/train.parser", parserType);
        ProcessFrameUtil.toParserFormat(questionProc.getProcArr(), "/tmp/question.parser", parserType);

        new SRLWrapper().doTrain("/tmp/train.parser", "/tmp/model", parserType, false);
        new SRLWrapper().doPredict("/tmp/question.parser", "/tmp/questionFramePredicted.parser", "/tmp/model", 1, true, false);

        new SRLToAligner().generateTsvForAlignerMergeVersion(questionFrameFileName, "/tmp/questionFramePredicted.parser", questionFramePredictedFileName, true, true, false);
        new SRLToAligner().generateTsvForAlignerMergeVersion(processFileName, processPredictedFile, answerFramePredicted, false, false, true);
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        //new SRLQuestionFrameModelBuilder().generatePredictedQuestionFrame("./data/process_frame_june.tsv", "./data/question_frame_23_june.tsv", "./data/question_frame_srl_manual.tsv", 
        //                                                                  "./data/all_predicted_manual.srl","./data/answer_frame_srl_manual.tsv", 1);
        //new SRLToAligner().generateTsvForAlignerMergeVersion("./data/question_frame_23_june.tsv", "/tmp/questionFramePredicted.parser", "./data/question_frame_srl.tsv", true, true);
        //new SRLToAligner().generateTsvForAlignerMergeVersion("./data/process_frame.tsv", "./data/all_predicted.srl", "./data/answer_frame_srl.tsv", false, false, true);

    }
}
