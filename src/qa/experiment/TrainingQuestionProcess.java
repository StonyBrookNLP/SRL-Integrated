/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import Util.ClearParserUtil;
import Util.GlobalVariable;
import Util.ProcessFrameUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
public class TrainingQuestionProcess {

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException {
        ProcessFrameProcessor procManual = new ProcessFrameProcessor(GlobalVariable.PROJECT_DIR + "/data/process_frame_june.tsv");
        ProcessFrameProcessor procDS = new ProcessFrameProcessor(GlobalVariable.PROJECT_DIR + "/data/ds_most_frequent_7_06_2015/ds_all_processes_w_pattern.tsv");
        ArrayList<ProcessFrame> trainManualAndDS = new ArrayList<ProcessFrame>();
        ArrayList<ProcessFrame> trainManual = new ArrayList<ProcessFrame>();
        procManual.loadProcessData();
        procDS.loadProcessData();

        ArrayList<String> trainingProcessNames = new ArrayList<String>();
        String[] processes = FileUtil.readLinesFromFile(GlobalVariable.PROJECT_DIR + "/data/trainingQuestion.tsv");
        for (String process : processes) {
            trainingProcessNames.add(process);
        }

        for (int i = 0; i < trainingProcessNames.size(); i++) {
            ArrayList<ProcessFrame> resManual = procManual.getProcessFrameByName(trainingProcessNames.get(i));
            ArrayList<ProcessFrame> resDS = procDS.getProcessFrameByName(trainingProcessNames.get(i));
            trainManual.addAll(resManual);
            trainManualAndDS.addAll(resManual);
            trainManualAndDS.addAll(resDS);
        }
        ProcessFrameUtil.toClearParserFormat(trainManualAndDS, GlobalVariable.PROJECT_DIR + "/data/trainingProcessQuestionDS.clearparser");
        ProcessFrameUtil.toClearParserFormat(trainManual, GlobalVariable.PROJECT_DIR + "/data/trainingProcessQuestionManual.clearparser");
        
        processes = FileUtil.readLinesFromFile(GlobalVariable.PROJECT_DIR + "/data/testingQuestion.tsv");
        ArrayList<String> testingProcessNames = new ArrayList<String>();
        for (String process : processes) {
            testingProcessNames.add(process);
        }

        ArrayList<ProcessFrame> testingProcesses = new ArrayList<ProcessFrame>();
        for (int i = 0; i < testingProcessNames.size(); i++) {
            ArrayList<ProcessFrame> res = procManual.getProcessFrameByName(testingProcessNames.get(i));
            testingProcesses.addAll(res);
        }
        ProcessFrameUtil.toClearParserFormat(testingProcesses, GlobalVariable.PROJECT_DIR + "/data/testingProcessQuestion.clearparser");
        ProcessFrameUtil.dumpFramesToFile(testingProcesses, GlobalVariable.PROJECT_DIR + "/data/testingProcessQuestion.tsv");
    }
}
