/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import static Util.CCGParserUtil.getPropBankLabeledSentence;
import Util.Constant;
import Util.GlobalV;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import static java.util.Collections.reverseOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import static java.util.Map.Entry.comparingByValue;
import java.util.Set;
import java.util.stream.Collectors;

import sbu.srl.ml.*;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.srl.SRLWrapper;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgProcessAnnotationData;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.Sentence;

/**
 *
 * @author slouvan
 */
// INPUT : Annotation Frame
// TODO:
// Config for format of the file e.g. Google Spreadsheet, BRAT etc
// Config for what field inside the file
public class SBURoleTrain {

    String modelDir;
    Set<String> classLabels = new HashSet<String>();
    HashMap<String, FeatureExtractor> fExtractors;
    SpockDataReader annotationReader;
    String[] annotations;
    int fold = 5;
    private boolean multiClass = false;

    public SBURoleTrain(String annotationFileName, String configFileName, String modelDir, boolean multiClass) throws FileNotFoundException, IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.modelDir = modelDir;
        // Read from arg annotation file
        annotationReader = new SpockDataReader(annotationFileName, configFileName, false);
        annotationReader.readData();
        classLabels = annotationReader.getRoleLabels();
        fExtractors = new HashMap<String, FeatureExtractor>();
        SRLFeatureExtractor srlFeatExtractor = buildSRLOutput(annotationReader.getSentences());
        this.multiClass = multiClass;
        if (multiClass) {
            classLabels.add("NONE");
            fExtractors.put("Multi", new FeatureExtractor());
            fExtractors.get("Multi").buildTokens(annotationReader.getSentences());
            fExtractors.get("Multi").srlExtractor = srlFeatExtractor;
            fExtractors.get("Multi").readFeatureFile("./configSBUProcRel/features");
            int cnt = 0;
            for (String roleName : classLabels) {
                fExtractors.get("Multi").multiClassLabel.put(roleName, ++cnt);
            }
        } else {
            for (String roleName : classLabels) {
                fExtractors.put(roleName, new FeatureExtractor());
            }
            for (String roleExtractor : fExtractors.keySet()) {
                fExtractors.get(roleExtractor).buildTokens(annotationReader.getSentences());
                fExtractors.get(roleExtractor).srlExtractor = srlFeatExtractor;
                fExtractors.get(roleExtractor).readFeatureFile("./configSBUProcRel/features");
            }
        }

    }

    public SBURoleTrain(String serFile, boolean multiClass) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ArrayList<Sentence> sentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile(serFile);
        annotationReader = new SpockDataReader();
        annotationReader.sentences = sentences;
        classLabels = annotationReader.getRoleLabels();
        fExtractors = new HashMap<String, FeatureExtractor>();
        SRLFeatureExtractor srlFeatExtractor = buildSRLOutput(annotationReader.getSentences());
        this.multiClass = multiClass;
        if (multiClass) {
            classLabels.add("NONE");
            int cnt = 0;
            fExtractors.put("Multi", new FeatureExtractor());
            fExtractors.get("Multi").buildTokens(annotationReader.getSentences());
            fExtractors.get("Multi").srlExtractor = srlFeatExtractor;
            fExtractors.get("Multi").readFeatureFile("./configSBUProcRel/features");
            for (String roleName : classLabels) {
                fExtractors.get("Multi").multiClassLabel.put(roleName, ++cnt);
            }

        } else {
            for (String roleName : classLabels) {
                fExtractors.put(roleName, new FeatureExtractor());
            }
            for (String roleExtractor : fExtractors.keySet()) {
                fExtractors.get(roleExtractor).buildTokens(annotationReader.getSentences());
                fExtractors.get(roleExtractor).srlExtractor = srlFeatExtractor;
                fExtractors.get(roleExtractor).readFeatureFile("./configSBUProcRel/features");
            }
        }
    }

 
    SRLFeatureExtractor buildSRLOutput(ArrayList<Sentence> sentences) throws FileNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
        System.out.println("BuildSRL Output");
        /*PrintWriter writer = new PrintWriter("sentences.temp");
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);
            String text = sentence.getRawText();
            writer.println(text);
        }
        writer.close();
        new SRLWrapper().doPredict("sentences.temp", "sentences.args.temp", "./data/modelCCG", Constant.SRL_CCG, true, false);
        HashMap<String, Sentence> sentLabeledPair = getPropBankLabeledSentence("sentences.args.temp");
        SRLFeatureExtractor srlExtractor = new SRLFeatureExtractor(sentLabeledPair);
        System.out.println("End of BuildSRL Output");*/
        HashMap<String, Sentence> sentLabeledPair = getPropBankLabeledSentence("sentences.args.temp");
        SRLFeatureExtractor srlExtractor = new SRLFeatureExtractor(sentLabeledPair);
        return srlExtractor;
    }

    public void trainMultiClassClassifier(String outputDir) throws IOException, NoSuchMethodException, IllegalAccessException {
        if (!(new File(outputDir)).exists()) {
            boolean dirCreated = FileUtil.mkDir(outputDir);
            if (!dirCreated) {
                System.out.println("Failed to create a directory to store classifier model");
                System.exit(0);
            }
        }
        System.out.println("BEGIN TRAINING");
        ArrayList<Sentence> sentences = annotationReader.getSentences();
        FeatureExtractor fExtractor = fExtractors.get("Multi");
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);
            ArrayList<ArgumentSpan> spans = sentence.getAllAnnotatedArgumentSpan();
            for (int j = 0; j < spans.size(); j++) {
                fExtractor.extractFeature(sentence, spans.get(j));
            }
        }
        HashMap<String, Integer> totalTrainInstances = new HashMap<String, Integer>();
        double nonNONECounter = 0;
        HashMap<String,Integer> roleCounter = new HashMap<String,Integer>();
        
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);
            System.out.println("Processing "+(i+1)+" / "+(sentences.size()));
            for (String roleName : classLabels) {
                // count NON-NONE HERE
                ArrayList<ArgumentSpan> spans = sentence.getMultiClassAnnotatedArgumentSpan(roleName, classLabels.size() - 1);
                // COUNTER
                if (roleCounter.get(roleName) != null)
                {
                    roleCounter.put(roleName, roleCounter.get(roleName)+spans.size());
                }
                else{
                    roleCounter.put(roleName, spans.size());
                }
                
                
                if (totalTrainInstances.get(roleName) != null) {
                    totalTrainInstances.put(roleName, totalTrainInstances.get(roleName) + spans.size());
                } else {
                    totalTrainInstances.put(roleName, spans.size());
                }
                for (ArgumentSpan span : spans) {
                    span.setMultiClassLabel(roleName);
                    fExtractor.extractFeature(sentence, span);
                    fExtractor.extractFeatureVector(sentence, span, true);
                    fExtractors.put("Multi", fExtractor);
                }
                //System.out.println("TOTAL feature vector for " + roleName + " :" + fExtractor.featureVectors.size());
            }
        }
        String roleLabel = "Multi";
        for (String role : roleCounter.keySet())
        {
            if (!role.equalsIgnoreCase("NONE"))
            {
                nonNONECounter += roleCounter.get(role);
                //System.out.println("Total "+role+" :"+roleCounter.get(role));
            }
        }
        //System.out.println("NON NONE COUNTER total : "+nonNONECounter);
        int nbNONEToSample = (int)(nonNONECounter/4.0);
        //System.out.println("To sample : "+nbNONEToSample);
        fExtractors.get(roleLabel).dumpFeaturesIndex(outputDir + "/" + roleLabel + ".featureIndex");
        // UNDERSAMPLE 
        int noneClassID= fExtractors.get(roleLabel).multiClassLabel.get("NONE");
        //fExtractors.get(roleLabel).dumpFeatureVectors(outputDir + "/" + roleLabel + ".vector");                   // vector file
        fExtractors.get(roleLabel).dumpFeatureVectorsUnderSample(outputDir + "/" + roleLabel + ".vector", nbNONEToSample, String.valueOf(noneClassID));                   // vector file
        
        LibLinearWrapper.doTrain(outputDir + "/" + roleLabel + ".vector", outputDir + "/" + roleLabel + ".model"); // output model
        FileUtil.serializeToFile(fExtractors.get(roleLabel), outputDir + "/" + roleLabel + ".featureExtract"); // output fExtractor object
        // feature weight analyzer
        Model model = Linear.loadModel(new FileReader(outputDir + "/" + roleLabel + ".model"));
        double[] featureWeights = model.getFeatureWeights();
        // read model, retrieve feature weight, store it in hashmap <index, value>. sort it by value
        HashMap<Integer, Double> indexWeightPair = new HashMap<Integer, Double>();
        for (int i = 0; i < featureWeights.length; i++) {
            indexWeightPair.put(i + 1, featureWeights[i]);
        }

        Map<Integer, Double> sortedMap = indexWeightPair.entrySet().stream()
                .sorted(reverseOrder(comparingByValue())).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        PrintWriter writer = new PrintWriter(outputDir + "/" + roleLabel + ".featureWeightRank");
        String[] lines = FileUtil.readLinesFromFile(outputDir + "/" + roleLabel + ".featureIndex");
        for (Integer keySet : sortedMap.keySet()) {
            // for each elmt in hashmap print the featurevalue:featuretype:featureWeight // output it to a file
            // System.out.println("KEYSET INDEX : " + keySet);
            if (keySet <= lines.length) {
                String fields[] = lines[keySet - 1].split("\t");
                writer.println(fields[0] + "\t" + fields[1] + "\t" + fields[2] + "\t" + sortedMap.get(keySet));
            }
        }
        writer.close();

    }

    public void train(String outputDir) throws IOException, NoSuchMethodException, IllegalAccessException {
        if (this.multiClass) {
            trainMultiClassClassifier(outputDir);
        } else {
            trainBinaryClassifier(outputDir);
        }
    }

    public void trainBinaryClassifier(String outputDir) throws IOException, NoSuchMethodException, IllegalAccessException {
        if (!(new File(outputDir)).exists()) {
            boolean dirCreated = FileUtil.mkDir(outputDir);
            if (!dirCreated) {
                System.out.println("Failed to create a directory to store classifier model");
                System.exit(0);
            }
        }
        System.out.println("BEGIN TRAINING ");
        ArrayList<Sentence> sentences = annotationReader.getSentences();
        // for each sentence do

        for (int i = 0; i < sentences.size(); i++) {
            System.out.println("SENTENCE : " + i);
            Sentence sentence = sentences.get(i);
            for (String roleName : classLabels) {
                FeatureExtractor fExtractor = fExtractors.get(roleName);
                ArrayList<ArgumentSpan> spans = sentence.getAnnotatedArgumentSpan(roleName);
                for (ArgumentSpan span : spans) {
                    fExtractor.extractFeature(sentence, span);
                    fExtractors.put(roleName, fExtractor);
                }
            }
        }

        // for each sentence do
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);
            // for each class label X
            for (String roleName : classLabels) {
                FeatureExtractor fExtractor = fExtractors.get(roleName);
                ArrayList<ArgumentSpan> spans = sentence.getAnnotatedArgumentSpan(roleName);
                for (ArgumentSpan span : spans) {
                    fExtractor.extractFeature(sentence, span);
                    fExtractor.extractFeatureVector(sentence, span, false);
                    fExtractors.put(roleName, fExtractor);
                }
                System.out.println("TOTAL feature vector for " + roleName + " :" + fExtractor.featureVectors.size());
            }
        }
        int totalFeature = 0;
        for (String roleLabel : fExtractors.keySet()) {
            if (fExtractors.get(roleLabel).featureVectors.size() > 0) {
                fExtractors.get(roleLabel).dumpFeaturesIndex(outputDir + "/" + roleLabel + ".featureIndex");
                fExtractors.get(roleLabel).dumpFeatureVectors(outputDir + "/" + roleLabel + ".vector");                   // vector file
                LibLinearWrapper.doTrain(outputDir + "/" + roleLabel + ".vector", outputDir + "/" + roleLabel + ".model"); // output model
                FileUtil.serializeToFile(fExtractors.get(roleLabel), outputDir + "/" + roleLabel + ".featureExtract"); // output fExtractor object
                // feature weight analyzer
                Model model = Linear.loadModel(new FileReader(outputDir + "/" + roleLabel + ".model"));
                double[] featureWeights = model.getFeatureWeights();
                // read model, retrieve feature weight, store it in hashmap <index, value>. sort it by value
                HashMap<Integer, Double> indexWeightPair = new HashMap<Integer, Double>();
                for (int i = 0; i < featureWeights.length; i++) {
                    indexWeightPair.put(i + 1, featureWeights[i]);
                }

                Map<Integer, Double> sortedMap = indexWeightPair.entrySet().stream()
                        .sorted(reverseOrder(comparingByValue())).
                         collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                PrintWriter writer = new PrintWriter(outputDir + "/" + roleLabel + ".featureWeightRank");
                String[] lines = FileUtil.readLinesFromFile(outputDir + "/" + roleLabel + ".featureIndex");
                for (Integer keySet : sortedMap.keySet()) {
                    // for each elmt in hashmap print the featurevalue:featuretype:featureWeight // output it to a file
                    System.out.println("KEYSET INDEX : " + keySet);
                    if (keySet <= lines.length) {
                        String fields[] = lines[keySet - 1].split("\t");
                        writer.println(fields[0] + "\t" + fields[1] + "\t" + fields[2] + "\t" + sortedMap.get(keySet));
                    }
                }
                writer.close();

            }
        }

        for (String roleLabel : fExtractors.keySet()) {
            totalFeature = 0;
            if (fExtractors.get(roleLabel).featureVectors.size() > 0) {
                HashMap<String, HashMap<String, Integer>> featIndex = fExtractors.get(roleLabel).featureIndexPair;
                for (String featureName : featIndex.keySet()) {
                    totalFeature += featIndex.get(featureName).size();
                }
            }
            System.out.println("TOTAL FEATURE LEARNED " + totalFeature + " " + roleLabel);
        }
    }

    

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        //SBURoleTrain trainer = new SBURoleTrain("./data/CandidateSpanGeneration.frame.tsv", "./data/CandidateSpanGeneration.tsv", GlobalV.PROJECT_DIR + "/data/model-26-10-2015-no-lexical");
        SBURoleTrain trainer = new SBURoleTrain("./data/training_w_pattern_4_role.tsv", GlobalV.PROJECT_DIR + "/configFrameFile/config.txt", GlobalV.PROJECT_DIR + "/data/model-11-12-2015-multiclass", true);
        //trainer.trainBinaryClassifier("/data/model-11-12-2015-multiclass");
        trainer.trainMultiClassClassifier("./data/model-11-12-2015-multiclass");

    }
}
