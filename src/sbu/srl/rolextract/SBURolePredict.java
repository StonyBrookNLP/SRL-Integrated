/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import Util.ArgProcessAnnotationDataUtil;
import Util.Constant;
import Util.GlobalV;
import Util.LibSVMUtil;
import Util.ProcessFrameUtil;
import Util.SentenceUtil;
import edu.uw.easysrl.main.Argument;
import edu.uw.easysrl.main.ParseResult;
import edu.uw.easysrl.main.Predicate;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import liblinear.FeatureNode;
import liblinear.Model;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import org.apache.commons.lang3.StringUtils;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.StanfordDepParserSingleton;
import qa.StanfordTokenizerSingleton;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;
import qa.srl.SRLWrapper;
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
    boolean knownAnnotation = true;

    public SBURolePredict(String testFileName, String configFileName, String modelDir, String predictionFileName, boolean isMultiClass) throws IOException, FileNotFoundException, ClassNotFoundException {
        dataReader = new SpockDataReader(testFileName, configFileName, false);
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

    public static boolean isOverlapping(String text, String otherText) {
        List<String> tokenizedText = StanfordTokenizerSingleton.getInstance().tokenize(text);
        List<String> tokenizedOtherText = StanfordTokenizerSingleton.getInstance().tokenize(otherText);
        for (String strText : tokenizedText) {
            if (tokenizedOtherText.contains(strText)) {
                return true;
            }
        }
        return false;
    }

    public static Object getBestArgument(ParseResult parseResult, String targetText) {
        List<Predicate> predicates = parseResult.getPredicates();
        List<Argument> arguments = new ArrayList<Argument>();

        for (Predicate predicate : predicates) {
            arguments.addAll(predicate.getArguments());
        }
        ArrayList<Object> overlappedSpans = new ArrayList<>();
        int minimumDistance = Integer.MAX_VALUE;
        boolean overlapping = false;
        for (Predicate predicate : predicates) {
            if (isOverlapping(predicate.getText(), targetText)) {
                minimumDistance = Math.min(minimumDistance, StringUtils.getLevenshteinDistance(targetText, predicate.getText()));
                overlapping = true;
            }
        }
        for (Argument argument : arguments) {
            if (isOverlapping(targetText, argument.getText())) {
                minimumDistance = Math.min(minimumDistance, StringUtils.getLevenshteinDistance(targetText, argument.getText()));
                overlapping = true;
            }
        }
        if (!overlapping) {
            return null; // NONE
        }
        for (Predicate predicate : predicates) {
            if (StringUtils.getLevenshteinDistance(targetText, predicate.getText()) == minimumDistance) {
                overlappedSpans.add(predicate);
            }
        }
        for (Argument argument : arguments) {
            if (StringUtils.getLevenshteinDistance(targetText, argument.getText()) == minimumDistance) {
                overlappedSpans.add(argument);
            }
        }
        if (overlappedSpans.size() > 1) {
            double maxScore = Double.MIN_VALUE;
            Object bestSpan = null;
            for (Object obj : overlappedSpans) {
                if (obj instanceof Predicate) {
                    if (((Predicate) obj).getScore() > maxScore) {
                        maxScore = ((Predicate) obj).getScore();
                        bestSpan = ((Predicate) obj);
                    }
                } else {
                    if (((Argument) obj).getArgScore() > maxScore) {
                        maxScore = ((Argument) obj).getArgScore();
                        bestSpan = ((Argument) obj);
                    }
                }
            }
            return bestSpan;
        } else {
            return overlappedSpans.get(0);
        }
    }

    public static void performPredictionEasySRL(String testObjFile, String testSentenceListFile, String outputFileName, String modelFileName, String foldDir) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ArrayList<Sentence> sentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testObjFile);
        new SRLWrapper().doPredictProcessRoleCCG(testSentenceListFile, outputFileName, modelFileName, foldDir.concat("/test/easySrlOut"), Constant.SRL_CCG, true, false);
        for (int i = 0; i < sentences.size(); i++) {
            System.out.println("SENTENCE : " + i + sentences.get(i).getRawText());
            Sentence currentSentence = sentences.get(i);
            ArrayList<ArgumentSpan> spans = currentSentence.getAllAnnotatedArgumentSpan();
            HashMap<String, String> argumentSpanThatHasAnnotation = currentSentence.getAllArgumentsThatHaveAnnotation();
            spans = (ArrayList<ArgumentSpan>) spans.stream().distinct().collect(toList());
            //if (knownAnnotation) {
            spans = (ArrayList<ArgumentSpan>) spans.stream().filter(s -> argumentSpanThatHasAnnotation.get(currentSentence.getId() + "_" + s.getStartIdx() + "_" + s.getEndIdx()) != null).collect(toList());
            //}
            HashMap<String, ArrayList<ArgumentSpan>> roleArgPrediction = new HashMap<String, ArrayList<ArgumentSpan>>();
            if (spans.size() == 0) {
                continue;
            }

            List<ParseResult> parseResult = SentenceUtil.readEasySRLJSONdata(foldDir.concat("/test/easySrlOut"));
            for (int j = 0; j < spans.size(); j++) {
                HashMap<String, Double> roleProbPair = new HashMap<String, Double>();
                HashMap<String, String> roleVectorPair = new HashMap<String, String>();
                ArgumentSpan currentSpan = spans.get(j);
                String text = currentSpan.getText();
                ParseResult sentParseResult = parseResult.get(i);
                int x = 0;

                if (sentParseResult.getParseScore() == -1.0) {
                    //currentSpan.setRoleProbPair(roleProbPair);
                    currentSpan.predictRoleType(true);
                    currentSpan.setRolePredicted("NONE");
                    roleProbPair.put("NONE", 1.0);
                    currentSpan.setRoleProbPair(roleProbPair);
                } else {
                    Object bestOverlap = getBestArgument(sentParseResult, text);
                    if (bestOverlap != null) {
                        //currentSpan.setRoleProbPair(roleProbPair);
                        String rolePredicted = "";
                        if (bestOverlap instanceof Predicate) {
                            rolePredicted = "trigger";
                            roleProbPair.put(rolePredicted, ((Predicate) bestOverlap).getScore());
                        } else {
                            try {
                                System.out.println(((Argument) bestOverlap).getLabel());
                                String label = ((Argument) bestOverlap).getLabel();

                                if (label.equalsIgnoreCase("ARG0") || label.equalsIgnoreCase("CAU")) {
                                    rolePredicted = "enabler";
                                } else if (label.equalsIgnoreCase("ARG1") ) {
                                    rolePredicted = "undergoer";
                                } else if (label.equalsIgnoreCase("ARG2") || label.equalsIgnoreCase("PNC")) {
                                    rolePredicted = "result";
                                }
                                else
                                {
                                    rolePredicted = "NONE";
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println(bestOverlap.getClass().toString());
                            }
                            roleProbPair.put(rolePredicted, ((Argument) bestOverlap).getArgScore());
                        }
                        currentSpan.predictRoleType(true);
                        currentSpan.setRolePredicted(rolePredicted);
                        currentSpan.setRoleProbPair(roleProbPair);
                    } else {
                        currentSpan.predictRoleType(true);
                        currentSpan.setRolePredicted("NONE");
                        roleProbPair.put("NONE", 1.0);
                        currentSpan.setRoleProbPair(roleProbPair);
                    }

                }

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
            if (roleArgPrediction == null) {
                System.out.println("Something is going wrong here");
                System.exit(0);
            }
            currentSentence.setRoleArgPrediction(roleArgPrediction);
        }

        // populateProbabilityILP(procDataAnnArr);
        // make unique of the SAME arguments,  based on startID and endID
        if (testObjFile.contains("gold")) {
            FileUtil.serializeToFile(sentences, testObjFile.replace("gold", "easysrlpredict"));
        } else {

            FileUtil.serializeToFile(sentences, testObjFile.replace("test.", "easysrlpredict."));
        }
    }

    // ONLY ANNOTATED OR NOT
    public void performPrediction(String testingFileName) throws IOException, FileNotFoundException, ClassNotFoundException {
        ArrayList<Sentence> sentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(testingFileName);
        for (int i = 0; i < sentences.size(); i++) {
            Sentence currentSentence = sentences.get(i);
            ArrayList<ArgumentSpan> spans = currentSentence.getAllAnnotatedArgumentSpan();
            HashMap<String, String> argumentSpanThatHasAnnotation = currentSentence.getAllArgumentsThatHaveAnnotation();
            spans = (ArrayList<ArgumentSpan>) spans.stream().distinct().collect(toList());
            if (knownAnnotation) {
                spans = (ArrayList<ArgumentSpan>) spans.stream().filter(s -> argumentSpanThatHasAnnotation.get(currentSentence.getId() + "_" + s.getStartIdx() + "_" + s.getEndIdx()) != null).collect(toList());
            }
            HashMap<String, ArrayList<ArgumentSpan>> roleArgPrediction = new HashMap<String, ArrayList<ArgumentSpan>>();
            if (!knownAnnotation && spans.size() == 0) {
                continue;
            }
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
                    for (String label : fExtractor.multiClassLabel.keySet()) {
                        int labelID = fExtractor.multiClassLabel.get(label);
                        int probID = -1;
                        for (int k = 0; k < labels.length; k++) {
                            if (labels[k] == labelID) {
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
            if (roleArgPrediction == null) {
                System.out.println("Something is going wrong here");
                System.exit(0);
            }
            currentSentence.setRoleArgPrediction(roleArgPrediction);
        }

        // populateProbabilityILP(procDataAnnArr);
        // make unique of the SAME arguments,  based on startID and endID
        if (testingFileName.contains("gold")) {
            FileUtil.serializeToFile(sentences, testingFileName.replace("gold", "predict"));
        } else {

            FileUtil.serializeToFile(sentences, testingFileName.replace("test.", "predict."));
        }
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
