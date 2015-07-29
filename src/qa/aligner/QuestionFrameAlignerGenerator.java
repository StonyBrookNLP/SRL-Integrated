/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.aligner;

import qa.srmodelbuilder.*;
import Util.ProcessFrameUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import qa.ProcessFrameProcessor;
import qa.srl.SRLWrapper;

/**
 *
 * @author samuellouvan
 */
public class QuestionFrameAlignerGenerator {

    public void generatePredictedQuestionFrame(String processFileName, String questionFrameFileName, String questionFramePredictedFileName, 
                                               String processPredictedFile, String answerFramePredicted, int parserType, boolean autoPi, boolean domainAdaptation) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        
        
        ProcessFrameProcessor proc = new ProcessFrameProcessor(processFileName);
        proc.loadProcessData();
        ProcessFrameProcessor questionProc = new ProcessFrameProcessor(questionFrameFileName);
        questionProc.setQuestionFrame(true);
        questionProc.loadProcessData();

        
        new ProcessFrameUtil().toParserFormat(proc.getProcArr(), "/tmp/train.parser", parserType);
        new ProcessFrameUtil().toParserFormat(questionProc.getProcArr(), "/tmp/question.parser", parserType);

        /* Train all model */
        new SRLWrapper().doTrain("/tmp/train.parser", "/tmp/model", parserType, false);
        new SRLWrapper().doPredict("/tmp/question.parser", "/tmp/questionFramePredicted.parser", "/tmp/model",parserType, autoPi, domainAdaptation);

        new SRLToAligner().generateQuestionAnswerFrameWithoutScore(questionFrameFileName, "/tmp/questionFramePredicted.parser", questionFramePredictedFileName, true, true, false);
        new SRLToAligner().generateQuestionAnswerFrameWithoutScore(processFileName, processPredictedFile, answerFramePredicted, false, false, true);
    }

    public void generatePredictedQuestionFrameWithScore(String processFileName, String questionFrameFileName, String questionFramePredictedFileName, 
                                                        String processPredictedFile, String answerFramePredicted, int parserType, boolean autoPi, boolean domainAdaptation) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException 
    {
        ProcessFrameProcessor proc = new ProcessFrameProcessor(processFileName);
        proc.loadProcessData();
        ProcessFrameProcessor questionProc = new ProcessFrameProcessor(questionFrameFileName);
        questionProc.setQuestionFrame(true);
        questionProc.loadProcessData();

        
        new ProcessFrameUtil().toParserFormat(proc.getProcArr(), "/tmp/train.parser", parserType);
        new ProcessFrameUtil().toParserFormat(questionProc.getProcArr(), "/tmp/question.parser", parserType);

        /* Train all model */
        new SRLWrapper().doTrain("/tmp/train.parser", "/tmp/model", parserType, false);
        new SRLWrapper().doPredict("/tmp/question.parser", "/tmp/questionFramePredicted.parser", "/tmp/model", 1, autoPi, false);

        new SRLToAligner().generateQuestionAnswerFrameWithScore(questionFrameFileName, "/tmp/questionFramePredicted.parser", questionFramePredictedFileName, true, true, false);
        new SRLToAligner().generateQuestionAnswerFrameWithScore(processFileName, processPredictedFile, answerFramePredicted, false, false, true);
    }
    
    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        new QuestionFrameAlignerGenerator().generatePredictedQuestionFrame("./data/process_frame_24_july.tsv", "./data/question_frame_23_july.tsv", 
                                                                           "./data/q_frame_combined.tsv", 
                                                                           "./data/a_frame_srl_dsperprocess.srl","./data/a_frame_srl_dsperprocess.tsv", 1, true, false);
        //new SRLToAligner().generateTsvForAlignerMergeVersion("./data/question_frame_23_june.tsv", "/tmp/questionFramePredicted.parser", "./data/question_frame_srl_manual.tsv", true, true, false);
        //new SRLToAligner().generateTsvForAlignerMergeVersion("./data/process_frame.tsv", "./data/all_predicted.srl", "./data/answer_frame_srl.tsv", false, false, true);

    }
}
