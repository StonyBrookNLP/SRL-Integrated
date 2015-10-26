/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.util.FileUtil;

/**
 *
 * @author slouvan
 */
public class Experiment {

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        // Read process frames
        ProcessFrameProcessor proc = new ProcessFrameProcessor("./data/result_training_data.frame.tsv");
        proc.loadProcessData();
        // Read annotation file
        String[] annotations = FileUtil.readLinesFromFile("./data/result_training_data.cleaned.tsv", true, "process");

        int x = 0;
        FeatureExtractor fExtractor = new FeatureExtractor();
        fExtractor.readFeatureFile("./configSBUProcRel/features");

        // for each row in process frame
        //     extract all possible features
        ArrayList<ProcessFrame> procFrameArr = proc.getProcArr();
        fExtractor.buildTokens(procFrameArr);
        for (int i = 0; i < procFrameArr.size(); i++) {
            ProcessFrame pFrame = procFrameArr.get(i);
            fExtractor.extractFeature(pFrame, "A2", annotations[i]);
        }

        for (int i = 0; i < procFrameArr.size(); i++) {
            ProcessFrame pFrame = procFrameArr.get(i);
            fExtractor.extractFeatureVector(pFrame, "A2", annotations[i]);
        }
        ArrayList<String> featureVectors = fExtractor.featureVectors;
        
        FileUtil.dumpToFile(featureVectors, "./data/result_training_data.vector","");
        // for each row in the process frame do
        //    perform dependency parse
        //    specify role of interest
        //    extract feature put the label on it
    }
}
