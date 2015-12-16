/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import Util.ArgProcessAnnotationDataUtil;
import Util.GlobalV;
import Util.LibSVMUtil;
import Util.ProcessFrameUtil;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import liblinear.FeatureNode;
import liblinear.Model;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.StanfordDepParserSingleton;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgProcessAnnotationData;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.Sentence;
import sbu.srl.ml.LibLinearWrapper;

/**
 * INPUT : Process Frame (already preprocessed)
 *
 * @author slouvan
 */
public class SBURolePredict {

    SpockDataReader dataReader;
    Set<String> classLabels = new HashSet<String>();
    HashMap<String, FeatureExtractor> fExtractors;
    HashMap<String, liblinear.Model> models;
    String[] annotations;
    String configFileName;
    String predictionFileName;
    private boolean isMulticlass = false;

    public SBURolePredict(String testFileName, String configFileName, String modelDir, String predictionFileName, boolean isMultiClass) throws IOException, FileNotFoundException, ClassNotFoundException {
        dataReader = new SpockDataReader(testFileName, configFileName);
        dataReader.readProcessData();
        this.configFileName = configFileName;
        this.predictionFileName = predictionFileName;
        this.isMulticlass = isMultiClass;
        classLabels = dataReader.getRoleLabels();
        fExtractors = new HashMap<String, FeatureExtractor>();
        models = new HashMap<String, liblinear.Model>();
        if (isMultiClass) {
            classLabels.add("NONE");
            String roleName = "Multi";
            if (FileUtil.isFileExist(modelDir + "/" + roleName + ".featureExtract")) {
                // Load feature extractor
                fExtractors.put(roleName, (FeatureExtractor) FileUtil.deserializeFromFile(modelDir + "/" + roleName + ".featureExtract"));
                // Load model
                Model model = liblinear.Linear.loadModel(new FileReader(modelDir + "/" + roleName + ".model"));
                //System.out.println(roleName);
                models.put(roleName, model);
            }
        } else {
            for (String roleName : classLabels) {
                if (FileUtil.isFileExist(modelDir + "/" + roleName + ".featureExtract")) {
                    // Load feature extractor
                    fExtractors.put(roleName, (FeatureExtractor) FileUtil.deserializeFromFile(modelDir + "/" + roleName + ".featureExtract"));
                    // Load model
                    Model model = liblinear.Linear.loadModel(new FileReader(modelDir + "/" + roleName + ".model"));
                    //System.out.println(roleName);
                    models.put(roleName, model);
                }
            }
        }

    }

    public SBURolePredict(String modelDir, String serFile, boolean isMultiClass) throws IOException, FileNotFoundException, ClassNotFoundException {
        ArrayList<Sentence> sentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(serFile);
        dataReader = new SpockDataReader();
        dataReader.sentences = sentences;
        classLabels = dataReader.getRoleLabels();
        fExtractors = new HashMap<String, FeatureExtractor>();
        models = new HashMap<String, liblinear.Model>();
        this.isMulticlass = isMultiClass;
        if (isMultiClass) {
            String roleName = "Multi";
            if (FileUtil.isFileExist(modelDir + "/" + roleName + ".featureExtract")) {
                // Load feature extractor
                fExtractors.put(roleName, (FeatureExtractor) FileUtil.deserializeFromFile(modelDir + "/" + roleName + ".featureExtract"));
                // Load model
                Model model = liblinear.Linear.loadModel(new FileReader(modelDir + "/" + roleName + ".model"));
                // System.out.println(roleName);
                models.put(roleName, model);
            }
        } else {
            for (String roleName : classLabels) {
                if (FileUtil.isFileExist(modelDir + "/" + roleName + ".featureExtract")) {
                    // Load feature extractor
                    fExtractors.put(roleName, (FeatureExtractor) FileUtil.deserializeFromFile(modelDir + "/" + roleName + ".featureExtract"));
                    // Load model
                    Model model = liblinear.Linear.loadModel(new FileReader(modelDir + "/" + roleName + ".model"));
                    // System.out.println(roleName);
                    models.put(roleName, model);
                }
            }
        }

    }

    // ONLY ANNOTATED OR NOT
    public void performPrediction(String testingFileName) throws IOException, FileNotFoundException, ClassNotFoundException {
        ArrayList<Sentence> sentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testingFileName);
        for (int i = 0; i < sentences.size(); i++) {
            Sentence currentSentence = sentences.get(i);
            ArrayList<ArgumentSpan> spans = currentSentence.getAllAnnotatedArgumentSpan();
            HashMap<String,String> argumentSpanThatHasAnnotation = currentSentence.getAllArgumentsThatHaveAnnotation();
            spans = (ArrayList<ArgumentSpan>) spans.stream().distinct().collect(toList());
            spans = (ArrayList<ArgumentSpan>) spans.stream().filter( s -> argumentSpanThatHasAnnotation.get(currentSentence.getId()+"_"+s.getStartIdx()+"_"+s.getEndIdx()) != null).collect(toList());
            HashMap<String, ArrayList<ArgumentSpan>> roleArgPrediction = new HashMap<String, ArrayList<ArgumentSpan>>();
            for (int j = 0; j < spans.size(); j++) {
                HashMap<String, Double> roleProbPair = new HashMap<String, Double>();
                HashMap<String, String> roleVectorPair = new HashMap<String, String>();
                ArgumentSpan currentSpan = spans.get(j);
                if (isMulticlass) {
                    String roleLabel = "Multi";
                    FeatureExtractor fExtractor = fExtractors.get(roleLabel);
                    ArrayList<Integer> tokenIdx = currentSpan.getRoleIdx();
                    DependencyTree depTree = StanfordDepParserSingleton.getInstance().parse(currentSentence.getRawText());
                    // Check ada common ancestor yang berada di tokenIdx gak
                    DependencyNode headNode = depTree.getHeadNode(tokenIdx);
                    //for (int k = 0; k < tokenIdx.size(); k++) {
                    String rawVector = fExtractor.extractFeatureVectorValue(headNode.getId(), currentSentence, currentSpan, false, isMulticlass);
                    //liblinear.Linear.predictProbability(;, x, prob_estimates)
                    FeatureNode[] x = LibLinearWrapper.toFeatureNode(rawVector, models.get(roleLabel));
                    int prediction = liblinear.Linear.predict(models.get(roleLabel), x);
                    double probs[] = new double[fExtractor.multiClassLabel.size()];
                    liblinear.Linear.predictProbability(models.get(roleLabel), x, probs);
                    Model m = models.get(roleLabel);
                    int[] labels = m.getLabels();
                    //int positiveIdx = labels[0] == 1 ? 0 : 1;
                    for (String label : fExtractor.multiClassLabel.keySet())
                    {
                        int labelID = fExtractor.multiClassLabel.get(label);
                        int probID = -1;
                        for (int k = 0; k < labels.length; k++)
                        {
                            if (labels[k] == labelID)
                            {
                                probID = k;
                                break;
                            }
                            
                        }
                        roleProbPair.put(label, probs[probID]);
                    }
                    
                    roleVectorPair.put(roleLabel, rawVector);
                    currentSpan.setRoleProbPair(roleProbPair);
                    currentSpan.setRoleFeatureVector(roleVectorPair);
                    currentSpan.predictRoleType(true);
                } else {
                    for (String roleLabel : classLabels) {
                        if (fExtractors.get(roleLabel) != null) {
                            FeatureExtractor fExtractor = fExtractors.get(roleLabel);
                            ArrayList<Integer> tokenIdx = currentSpan.getRoleIdx();
                            DependencyTree depTree = StanfordDepParserSingleton.getInstance().parse(currentSentence.getRawText());
                            // Check ada common ancestor yang berada di tokenIdx gak
                            DependencyNode headNode = depTree.getHeadNode(tokenIdx);
                            //for (int k = 0; k < tokenIdx.size(); k++) {
                            String rawVector = fExtractor.extractFeatureVectorValue(headNode.getId(), currentSentence, currentSpan, false, isMulticlass);
                            //liblinear.Linear.predictProbability(;, x, prob_estimates)
                            FeatureNode[] x = LibLinearWrapper.toFeatureNode(rawVector, models.get(roleLabel));
                            int prediction = liblinear.Linear.predict(models.get(roleLabel), x);
                            double probs[] = new double[2];
                            liblinear.Linear.predictProbability(models.get(roleLabel), x, probs);
                            Model m = models.get(roleLabel);
                            int[] labels = m.getLabels();
                            int positiveIdx = labels[0] == 1 ? 0 : 1;
                            roleProbPair.put(roleLabel, probs[positiveIdx]);
                            roleVectorPair.put(roleLabel, rawVector);
                        }
                    }
                    currentSpan.setRoleProbPair(roleProbPair);
                    currentSpan.setRoleFeatureVector(roleVectorPair);
                    currentSpan.normalizeProbScore();
                    currentSpan.predictRoleType(false);
                }

                // store in this in the hashMap
                if (roleArgPrediction.get(currentSpan.getRolePredicted()) != null) {
                    ArrayList<ArgumentSpan> predictedSpan = roleArgPrediction.get(currentSpan.getRolePredicted());
                    predictedSpan.add(currentSpan);
                    roleArgPrediction.put(currentSpan.getRolePredicted(), predictedSpan);
                } else {
                    ArrayList<ArgumentSpan> predictedSpan = new ArrayList<ArgumentSpan>();
                    predictedSpan.add(currentSpan);
                    roleArgPrediction.put(currentSpan.getRolePredicted(), predictedSpan);
                }
            }
            currentSentence.setRoleArgPrediction(roleArgPrediction);
        }

        // populateProbabilityILP(procDataAnnArr);
        // make unique of the SAME arguments,  based on startID and endID
        FileUtil.serializeToFile(sentences, testingFileName.replace("gold", "predict"));
    }

    public void performPrediction() throws IOException {
        /*ArrayList<ArgProcessAnnotationData> procDataAnnArr = null; //;argAnnotationReader.getProcDataArr();
         for (int i = 0; i < procDataAnnArr.size(); i++) {
         ArgProcessAnnotationData currentProcData = procDataAnnArr.get(i);
         for (String roleName : classLabels) {
         if (fExtractors.get(roleName) != null) {
         FeatureExtractor fExtractor = fExtractors.get(roleName);
         ArrayList<Integer> tokenIdx = currentProcData.getRoleIdx(roleName);
         if (tokenIdx.size() == 0) {
         continue;
         }
         ArrayList<Integer> incorrectIdx = new ArrayList<Integer>();
         DependencyTree depTree = StanfordDepParserSingleton.getInstance().parse(currentProcData.getRawText());
         // Check ada common ancestor yang berada di tokenIdx gak
         DependencyNode headNode = depTree.getHeadNode(tokenIdx);
         //for (int k = 0; k < tokenIdx.size(); k++) {
         String rawVector = fExtractor.extractFeatureVectorValue(headNode.getId(), currentProcData, roleName, false);// IMPLEMENT THIS
         //liblinear.Linear.predictProbability(;, x, prob_estimates)
         FeatureNode[] x = LibLinearWrapper.toFeatureNode(rawVector, models.get(roleName));
         int prediction = liblinear.Linear.predict(models.get(roleName), x);
         double probs[] = new double[2];
         liblinear.Linear.predictProbability(models.get(roleName), x, probs);
         System.out.println("PREDICTION " + prediction);
         if (prediction == 1) {
         System.out.println("CORRECT");
         } else if (prediction == -1) {
         System.out.println("NOT CORRECT");
         //incorrectIdx.add(tokenIdx.get(k));
         currentProcData.clearRoleFiller(roleName);
         }

         } else {
         // clear role fillers
         currentProcData.clearRoleFiller(roleName);
         }
         }
         }
         // dumpFrameToFile
         ArgProcessAnnotationDataUtil.dumpRolePredictionToFile(procDataAnnArr, this.configFileName, this.predictionFileName);
         //ProcessFrameUtil.dumpFramesToFile(frames, GlobalV.PROJECT_DIR + "/data/predicted.tsv");*/
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException {
        /*SBURolePredict predictor = new SBURolePredict(GlobalV.PROJECT_DIR + "/data/undergoer_20testing.frame.tsv",
         GlobalV.PROJECT_DIR + "/data/undergoer_20testing.cleaned.tsv",
         GlobalV.PROJECT_DIR + "/data/modelDebug");*/
        /*SBURolePredict predictor = new SBURolePredict(GlobalV.PROJECT_DIR + "/data/training_w_pattern.tsv",
         GlobalV.PROJECT_DIR + "/configFrameFile/config.txt",
         GlobalV.PROJECT_DIR + "/data/model-03-11-2015-full",
         GlobalV.PROJECT_DIR + "/data/out/predicted.tsv");
         predictor.performPrediction();*/
    }
}
