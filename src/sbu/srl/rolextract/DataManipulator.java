/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import qa.util.FileUtil;
import sbu.srl.datastructure.Sentence;

/**
 *
 * @author slouvan
 */
public class DataManipulator {

    public static void processDiffBetweenTrainTest(String frameFile, String trainSentencesFileName, String testSentenceFileName) throws FileNotFoundException, IOException {
        SpockDataReader reader = new SpockDataReader(frameFile, "/home/slouvan/NetBeansProjects/SRL-Integrated/configFrameFile/config.txt", false);
        String[] trainingSentences = FileUtil.readLinesFromFile(trainSentencesFileName);
        String[] testSentences = FileUtil.readLinesFromFile(testSentenceFileName);
        reader.readData();
        ArrayList<Sentence> sentences = reader.getSentences();
        List<String> processInTrain = new ArrayList<>();
        List<String> processInTest = new ArrayList<>();

        for (String trainSent : trainingSentences) {
            String trainProcess = sentences.stream().filter(s -> s.getRawText().equalsIgnoreCase(trainSent)).collect(Collectors.toList()).get(0).getProcessName();
            processInTrain.add(trainProcess);
        }

        for (String testSent : testSentences) {
            String testProicess = sentences.stream().filter(s -> s.getRawText().equalsIgnoreCase(testSent)).collect(Collectors.toList()).get(0).getProcessName();
            processInTest.add(testProicess);
        }

        Set<String> trainSet = new HashSet<String>(processInTrain);
        Set<String> testSet = new HashSet<String>(processInTest);
        testSet.removeAll(trainSet);
        for (String process : testSet)
        {
            System.out.println(process);
        }
    }

    // collect frequency of PropBank role 
    public static void freqARGCollectorEasySRL() throws IOException, FileNotFoundException, ClassNotFoundException {
        // <undergoer,<A0,100>>
        HashMap<String, HashMap<String, Integer>> processRoleARGRoleCount = new HashMap<String, HashMap<String, Integer>>();
        HashMap<Integer, String> idxARGPair = new HashMap<Integer, String>();
        FeatureExtractor fExtractor = (FeatureExtractor) FileUtil.deserializeFromFile("/home/slouvan/NetBeansProjects/SRL-Integrated/data/all-04-01-2016/train/Multi.featureExtract");
        //fExtractor.getMultiClassLabel(id)
        String[] vectors = FileUtil.readLinesFromFile("/home/slouvan/NetBeansProjects/SRL-Integrated/data/all-04-01-2016/train/Multi.vector");
        String[] featureIndexes = FileUtil.readLinesFromFile("/home/slouvan/NetBeansProjects/SRL-Integrated/data/all-04-01-2016/train/Multi.featureIndex");

        for (String featureIndex : featureIndexes) {
            String[] fields = featureIndex.split("\t");
            if (fields[2].contains("srl_propbank")) {
                String[] subFields = fields[1].split("_");
                idxARGPair.put(Integer.parseInt(fields[0]), subFields[subFields.length - 1]);
            }
        }
        for (String vector : vectors) {
            // check the class label first
            String fields[] = vector.split("\\s+");

            String label = fExtractor.getMultiClassLabel(Integer.parseInt(fields[0]));
            for (int i = 1; i < fields.length; i++) {
                String featureIndex = fields[i].split(":")[0];
                if (idxARGPair.get(Integer.parseInt(featureIndex)) != null) {
                    int x = 0;
                    if (processRoleARGRoleCount.get(label) == null) {
                        HashMap<String, Integer> argCount = new HashMap<String, Integer>();
                        argCount.put(idxARGPair.get(Integer.parseInt(featureIndex)), 1);
                        processRoleARGRoleCount.put(label, argCount);
                    } else {
                        HashMap<String, Integer> argCount = processRoleARGRoleCount.get(label);
                        if (argCount.get(idxARGPair.get(Integer.parseInt(featureIndex))) == null) {
                            argCount.put(idxARGPair.get(Integer.parseInt(featureIndex)), 1);
                        } else {
                            argCount.put(idxARGPair.get(Integer.parseInt(featureIndex)), argCount.get(idxARGPair.get(Integer.parseInt(featureIndex))) + 1);
                        }
                        processRoleARGRoleCount.put(label, argCount);
                    }
                }
            }
        }

        for (String processRole : processRoleARGRoleCount.keySet()) {
            HashMap<String, Integer> propBankCount = processRoleARGRoleCount.get(processRole);
            for (String propBankRole : propBankCount.keySet()) {
                System.out.println(processRole + "\t" + propBankRole + "\t" + propBankCount.get(propBankRole));
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        /*String allLines[] = FileUtil.readLinesFromFile("./data/training_all.tsv");
         String currentTrainingData[] = FileUtil.readLinesFromFile("./data/training_4_roles.tsv");
         HashMap<String, String> trainingDataSentences = new HashMap<String, String>();
         for (int i = 0; i < currentTrainingData.length; i++) {
         trainingDataSentences.put(currentTrainingData[i].split("\t")[7], currentTrainingData[i].split("\t")[7]);
         }
         PrintWriter writer = new PrintWriter("./data/knowledge_sentences.tsv");
         writer.println(allLines[0]);
         for (int i = 1; i < allLines.length; i++) {
         if (trainingDataSentences.get(allLines[i].split("\t")[7]) == null) {
         //System.out.println("NOT EXIST");
         writer.println(allLines[i]);
         } else {
         System.out.println("EXIST " + allLines[i]);
         }
         }

         writer.close();    */
        //freqARGCollectorEasySRL();
        processDiffBetweenTrainTest("./data/./training_4_roles.tsv", 
                                    "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-04-01-2016/fold-1/train/cv.1.train.sentence.sbu", 
                                    "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-04-01-2016/fold-1/test/cv.1.test.sentence.sbu");
    }

}
