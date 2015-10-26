/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import Util.GlobalV;
import Util.LibSVMUtil;
import Util.ProcessFrameUtil;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import liblinear.FeatureNode;
import liblinear.Model;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.util.FileUtil;
import sbu.srl.ml.LibLinearWrapper;

/**
 * INPUT : Process Frame (already preprocessed)
 *
 * @author slouvan
 */
public class SBURolePredict {

    ProcessFrameProcessor proc;
    ArrayList<String> classLabels = new ArrayList<String>();
    HashMap<String, FeatureExtractor> fExtractors;
    HashMap<String, liblinear.Model> models;
    String[] annotations;

    public SBURolePredict(String processFrameFileName, String annotationFileName, String modelDir) throws IOException, FileNotFoundException, ClassNotFoundException {
        proc = new ProcessFrameProcessor(processFrameFileName);
        proc.loadProcessData();
        classLabels = proc.getRoleLabels();
        annotations = FileUtil.readLinesFromFile(annotationFileName, true, "process");
        fExtractors = new HashMap<String, FeatureExtractor>();
        models = new HashMap<String, liblinear.Model>();
        for (int i = 0; i < classLabels.size(); i++) {
            if (FileUtil.isFileExist(modelDir + "/" + classLabels.get(i) + ".featureExtract")) {
                // Load feature extractor
                fExtractors.put(classLabels.get(i), (FeatureExtractor) FileUtil.deserializeFromFile(modelDir + "/" + classLabels.get(i) + ".featureExtract"));
                // Load model
                Model model = liblinear.Linear.loadModel(new FileReader(modelDir + "/" + classLabels.get(i) + ".model"));
                System.out.println(classLabels.get(i));

                models.put(classLabels.get(i), model);
            }
        }
        int x = 0;
    }

    public void performPrediction() throws IOException {
        ArrayList<ProcessFrame> frames = proc.getProcArr();
        for (int i = 0; i < frames.size(); i++) {
            ProcessFrame currentFrame = frames.get(i);
            currentFrame.setRoleFiller("A3", "");
            for (int j = 0; j < classLabels.size(); j++) {
                if (fExtractors.get(classLabels.get(j)) != null) {
                    FeatureExtractor fExtractor = fExtractors.get(classLabels.get(j));
                    ArrayList<Integer> tokenIdx = currentFrame.getRoleIdx(classLabels.get(j));
                    ArrayList<Integer> incorrectIdx = new ArrayList<Integer>();
                    for (int k = 0; k < tokenIdx.size(); k++) {
                        String rawVector = fExtractor.extractFeatureVectorValue(tokenIdx.get(k), currentFrame, classLabels.get(j), annotations[i], false);// IMPLEMENT THIS
                        //liblinear.Linear.predictProbability(;, x, prob_estimates)
                        FeatureNode[] x = LibLinearWrapper.toFeatureNode(rawVector, models.get(classLabels.get(j)));
                        int prediction = liblinear.Linear.predict(models.get(classLabels.get(j)), x);
                        double probs[] = new double[2];
                        liblinear.Linear.predictProbability(models.get(classLabels.get(j)), x, probs);

                        
                        liblinear.Linear.predictProbability(models.get(classLabels.get(j)), x, probs);
                            if (prediction == 1) {
                            System.out.println("CORRECT");

                        } else if (prediction == -1) {
                            System.out.println("NOT CORRECT");
                            incorrectIdx.add(tokenIdx.get(k));
                        }
                    }
                    for (int l = 0; l < incorrectIdx.size(); l++) {
                        tokenIdx.remove(incorrectIdx.get(l));
                    }
                    String roleFillers = currentFrame.getStringFromIdx(tokenIdx);
                    currentFrame.setRoleFiller(classLabels.get(j), roleFillers);
                } else {
                    // clear role fillers
                    currentFrame.setRoleFiller(classLabels.get(j), "");
                }
            }
        }
        // dumpFrameToFile
        ProcessFrameUtil.dumpFramesToFile(frames, GlobalV.PROJECT_DIR + "/data/extracted_sentences.predicted.tsv");
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException {
        SBURolePredict predictor = new SBURolePredict(GlobalV.PROJECT_DIR + "/data/undergoer_20testing.frame.tsv",
                GlobalV.PROJECT_DIR + "/data/undergoer_20testing.cleaned.tsv",
                GlobalV.PROJECT_DIR + "/data/modelDebug");
        predictor.performPrediction();
    }
}
