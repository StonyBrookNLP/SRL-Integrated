/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import Util.GlobalV;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import static java.util.Collections.reverseOrder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static java.util.Map.Entry.comparingByValue;
import java.util.stream.Collectors;
import liblinear.Model;
import sbu.srl.ml.*;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.util.FileUtil;

/**
 *
 * @author slouvan
 */
// INPUT : Process Frame, Annotation Frame
// TODO:
// Config for format of the file e.g. Google Spreadsheet, BRAT etc
// Config for what field inside the file
public class SBURoleTrain {

    String modelDir;
    ArrayList<String> classLabels = new ArrayList<String>();
    HashMap<String, FeatureExtractor> fExtractors;
    ProcessFrameProcessor proc;
    String[] annotations;

    public SBURoleTrain(String processFrameFileName, String annotationFileName, String modelDir) throws FileNotFoundException, IOException, ClassNotFoundException {
        this.modelDir = modelDir;
        proc = new ProcessFrameProcessor(processFrameFileName);
        proc.loadProcessData();
        annotations = FileUtil.readLinesFromFile(annotationFileName, true, "process");
        classLabels = proc.getRoleLabels();
        fExtractors = new HashMap<String, FeatureExtractor>();
        for (int i = 0; i < classLabels.size(); i++) {
            fExtractors.put(classLabels.get(i), new FeatureExtractor());
        }
        for (String roleExtractor : fExtractors.keySet()) {
            fExtractors.get(roleExtractor).buildTokens(proc.getProcArr());
            fExtractors.get(roleExtractor).readFeatureFile("./configSBUProcRel/features");
        }
    }

    private int getClassLabel(String targetRole, String annotationInfo) {
        if (targetRole.equalsIgnoreCase("A0")) {
            int label = Integer.parseInt(annotationInfo.split("\t")[9]);
            return (label == 1) ? 1 : -1;
        } else if (targetRole.equalsIgnoreCase("A1")) {
            int label = Integer.parseInt(annotationInfo.split("\t")[10]);
            return (label == 1) ? 1 : -1;
        } else if (targetRole.equalsIgnoreCase("A2")) {
            String[] columns = annotationInfo.split("\t");
            int x = 0;
            int label = Integer.parseInt(annotationInfo.split("\t")[12]);
            return (label == 1) ? 1 : -1;
        } else if (targetRole.equalsIgnoreCase("A3")) {
            return 0;
        } else {
            // Trigger
            int label = Integer.parseInt(annotationInfo.split("\t")[11]);
            return (label == 1) ? 1 : -1;
        }
    }

    public void train() throws IOException, NoSuchMethodException, IllegalAccessException {
        // for each sentence do
        for (int i = 0; i < proc.getProcArr().size(); i++) {
            System.out.println("SENTENCE : "+i);
            ProcessFrame currentProcessFrame = proc.getProcArr().get(i);
            for (int j = 0; j < classLabels.size(); j++) {
                FeatureExtractor fExtractor = fExtractors.get(classLabels.get(j));
                if (currentProcessFrame.isExistRole(classLabels.get(j)) && fExtractor.isAnnotationExist(classLabels.get(j), annotations[i])) {
                    fExtractor.extractFeature(currentProcessFrame, classLabels.get(j), annotations[i]);
                    fExtractors.put(classLabels.get(j), fExtractor);
                }
            }
        }

        // for each sentence do
        for (int i = 0; i < proc.getProcArr().size(); i++) {
            ProcessFrame currentProcessFrame = proc.getProcArr().get(i);
            // for each class label X
            for (int j = 0; j < classLabels.size(); j++) {
                FeatureExtractor fExtractor = fExtractors.get(classLabels.get(j));
                if (currentProcessFrame.isExistRole(classLabels.get(j)) && fExtractor.isAnnotationExist(classLabels.get(j), annotations[i])) {
                    System.out.println("Extracting feature vector for " + classLabels.get(j));
                    fExtractor.extractFeatureVector(currentProcessFrame, classLabels.get(j), annotations[i]);
                    fExtractors.put(classLabels.get(j), fExtractor);
                }
            }
        }
        int totalFeature = 0;
        for (String roleLabel : fExtractors.keySet()) {
            if (fExtractors.get(roleLabel).featureVectors.size() > 0) {
                fExtractors.get(roleLabel).dumpFeaturesIndex(modelDir + "/" + roleLabel + ".featureIndex");
                fExtractors.get(roleLabel).dumpFeatureVectors(GlobalV.PROJECT_DIR + "/data/" + roleLabel + ".vector");               // vector file
                LibLinearWrapper.doTrain(GlobalV.PROJECT_DIR + "/data/" + roleLabel + ".vector", modelDir + "/" + roleLabel + ".model"); // output model
                FileUtil.serializeToFile(fExtractors.get(roleLabel), modelDir + "/" + roleLabel + ".featureExtract"); // output fExtractor object
                // feature weight analyzer
                Model model = liblinear.Linear.loadModel(new FileReader(modelDir + "/" + roleLabel + ".model"));
                double[] featureWeights = model.getFeatureWeights();
                // read model, retrieve feature weight, store it in hashmap <index, value>. sort it by value
                HashMap<Integer, Double> indexWeightPair = new HashMap<Integer, Double>();
                for (int i = 0; i < featureWeights.length; i++) {
                    indexWeightPair.put(i + 1, featureWeights[i]);
                }
                // read featureIndexDump
                //Map<Integer, Double> sortedMap
                //= 
                Map<Integer, Double> sortedMap = indexWeightPair.entrySet().stream()
                        .sorted(reverseOrder(comparingByValue())).
                        collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                PrintWriter writer = new PrintWriter(modelDir + "/" + roleLabel + ".featureWeightRank");
                String[] lines = FileUtil.readLinesFromFile(modelDir + "/" + roleLabel + ".featureIndex");
                for (Integer keySet : sortedMap.keySet()) {
                    // for each elmt in hashmap print the featurevalue:featuretype:featureWeight // output it to a file
                    System.out.println("KEYSET INDEX : " + keySet);
                    if (keySet <= lines.length) {
                        String fields[] = lines[keySet - 1].split("\t");
                        writer.println(fields[0] + "\t" + fields[1] + "\t" + fields[2] + "\t" + sortedMap.get(keySet));
                        //System.out.println(fields[0] + "\t" + fields[1] + "\t" + fields[2] + "\t" + sortedMap.get(keySet));
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

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        SBURoleTrain trainer = new SBURoleTrain("./data/CandidateSpanGeneration.frame.tsv", "./data/CandidateSpanGeneration.tsv", GlobalV.PROJECT_DIR + "/data/model-26-10-2015-no-lexical");
        trainer.train();     
    }
}
