/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import Util.ArrUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;
import qa.ProcessFrame;
import qa.QuestionData;
import qa.ProcessFrameProcessor;
import qa.QuestionDataProcessor;
import qa.StanfordLemmatizer;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

// Load the frame
// Generate unique vector
/**
 *
 * @author samuellouvan
 */
class ProcessFeatureVector {

    private String processName;
    private ArrayList<String> featureVectors;

    public ProcessFeatureVector(String processName, ArrayList<String> featureVectors) {
        this.processName = processName;
        this.featureVectors = featureVectors;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public ArrayList<String> getFeatureVectors() {
        return featureVectors;
    }

    public void setFeatureVectors(ArrayList<String> featureVectors) {
        this.featureVectors = featureVectors;
    }
}

public class Baseline {

    ProcessFrameProcessor proc;
    QuestionDataProcessor ques;
    HashMap<String, Integer> tokenIDPair;
    ArrayList<String> bowFeature;
    ArrayList<ProcessFeatureVector> arrProcessFeature;
    StanfordLemmatizer slem = new StanfordLemmatizer();
    private String questionFileName;

    public Baseline(String processFileName, String questionFileName) {
        proc = new ProcessFrameProcessor(processFileName);
        ques = new QuestionDataProcessor(questionFileName);
        this.questionFileName = questionFileName;
        
    }

    public void loadProcessData() throws FileNotFoundException, IOException, ClassNotFoundException {
        proc.loadProcessData();
    }

    public void loadQuestionData() throws FileNotFoundException {
        ques.loadQuestionData();
    }

    public int getFrequency(String token, String[] tokens) {
        int cnt = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (token.equalsIgnoreCase(tokens[i])) {
                cnt++;
            }
        }

        return cnt;
    }

    public void generateFeatureVectors() {
        ArrayList<ProcessFrame> processesData = proc.getProcArr();
        HashMap<String, Integer> processCount = new HashMap<String,Integer>();
        bowFeature = new ArrayList<String>();
        for (int i = 0; i < processesData.size(); i++) {
            // Get the tokenized text
            String[] tokenizedText = processesData.get(i).getTokenizedText();
            for (String token : tokenizedText) {
                String currentToken = token.toLowerCase();
                if (!bowFeature.contains(currentToken)) {
                    bowFeature.add(currentToken);
                }
            }
        }
        System.out.println("TOTAL TOKEN(FEATURES) IN THE TRAINING DATA : " + bowFeature.size());

        String currentProcess = processesData.get(0).getProcessName();
        arrProcessFeature = new ArrayList<ProcessFeatureVector>();
        ArrayList<String> featuresArr = new ArrayList<String>();
        for (int i = 0; i < processesData.size(); i++) {
            System.out.println(i);
            String[] tokenizedText = processesData.get(i).getTokenizedText();
            if (!currentProcess.equalsIgnoreCase(processesData.get(i).getProcessName())) {
                ProcessFeatureVector procFeatureVector = new ProcessFeatureVector(currentProcess, new ArrayList<>(featuresArr));
                arrProcessFeature.add(procFeatureVector);
                featuresArr.clear();

                currentProcess = processesData.get(i).getProcessName();
            }

            String features = "";
            for (int j = 0; j < bowFeature.size(); j++) {
                int freq = getFrequency(bowFeature.get(j), tokenizedText);
                features += freq + "\t";

            }
            features += currentProcess;
            featuresArr.add(features);
        }
        ProcessFeatureVector procFeatureVector = new ProcessFeatureVector(currentProcess, featuresArr);
        arrProcessFeature.add(procFeatureVector);
        featuresArr.clear();

    }

    public FastVector generateWEKAFeatureVector() {
        FastVector fvWekaAttributes = new FastVector(bowFeature.size() + 1);
        for (int i = 0; i < bowFeature.size(); i++) {
            fvWekaAttributes.addElement(new Attribute(bowFeature.get(i)));
        }
        FastVector fvClassVal = new FastVector(arrProcessFeature.size());
        for (int i = 0; i < arrProcessFeature.size(); i++) {
            fvClassVal.addElement(arrProcessFeature.get(i).getProcessName().replaceAll("\\s+", ""));
        }
        Attribute ClassAttribute = new Attribute("processType", fvClassVal);
        fvWekaAttributes.addElement(ClassAttribute);

        return fvWekaAttributes;
    }

    public FastVector generateWEKAFeatureVector(String[] processes) {
        FastVector fvWekaAttributes = new FastVector(bowFeature.size() + 1);
        for (int i = 0; i < bowFeature.size(); i++) {
            fvWekaAttributes.addElement(new Attribute(bowFeature.get(i)));
        }
        FastVector fvClassVal = new FastVector(processes.length);
        for (int i = 0; i < processes.length; i++) {
            fvClassVal.addElement(processes[i]);
        }
        Attribute ClassAttribute = new Attribute("processType", fvClassVal);
        fvWekaAttributes.addElement(ClassAttribute);

        return fvWekaAttributes;
    }

    public int isNameFuzzyMatch(String[] str1, String[] str2) {
        for (int i = 0; i < str1.length; i++) {
            for (int j = 0; j < str2.length; j++) {
                int dist = StringUtils.getLevenshteinDistance(str1[i].trim(), str2[j].trim(), 3);
                if (dist != -1) {
                    String prefix = str1[i].substring(0, 3);
                    if (str2[j].trim().startsWith(prefix)) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    public String trainAndPredict(String[] processNames , String question) throws Exception {
        FastVector fvWekaAttribute = generateWEKAFeatureVector(processNames);
        Instances trainingSet = new Instances("Rel", fvWekaAttribute, bowFeature.size() + 1);
        trainingSet.setClassIndex(bowFeature.size());

        int cnt = 0;
        for (int i = 0; i < arrProcessFeature.size(); i++) {
            String[] names = arrProcessFeature.get(i).getProcessName().split("\\|");
            int sim = isNameFuzzyMatch(processNames, names);
            if (sim != -1) {
               // System.out.println("match " + arrProcessFeature.get(i).getProcessName());
                ArrayList<String> featureVector = arrProcessFeature.get(i).getFeatureVectors();
                for (int j = 0; j < featureVector.size(); j++) {
                    Instance trainInstance = new Instance(bowFeature.size() + 1);
                    String[] attrValues = featureVector.get(j).split("\t");
                   // System.out.println(trainInstance.numAttributes());
                   // System.out.println(fvWekaAttribute.size());
                    for (int k = 0; k < bowFeature.size(); k++) {
                        trainInstance.setValue((Attribute) fvWekaAttribute.elementAt(k), Integer.parseInt(attrValues[k]));
                    }
                    trainInstance.setValue((Attribute) fvWekaAttribute.elementAt(bowFeature.size()), processNames[sim]);
                    trainingSet.add(trainInstance);

                    //System.out.println(cnt);
                    cnt++;
                }
            }
        }
        
        Classifier cl = new NaiveBayes();
        cl.buildClassifier(trainingSet);
        Instance inst= new Instance(bowFeature.size() + 1);
        //String[] tokenArr = tokens.toArray(new String[tokens.size()]);
        for (int j = 0; j < bowFeature.size(); j++) {
            List<String> tokens = slem.tokenize(question);
            String[] tokArr = tokens.toArray(new String[tokens.size()]);
            int freq = getFrequency(bowFeature.get(j), tokArr);
            inst.setValue((Attribute) fvWekaAttribute.elementAt(j), freq);
        }
        
        inst.setDataset(trainingSet);
        int idxMax = ArrUtil.getIdxMax(cl.distributionForInstance(inst));
        return processNames[idxMax];
    }
    
    public boolean isExistProcess(String processName)
    {
        String[] processNameArr = new String[1];
        processNameArr[0] = processName;
        for (int i = 0; i < arrProcessFeature.size(); i++)
        {
            String[] names = arrProcessFeature.get(i).getProcessName().split("\\|");
            int sim = isNameFuzzyMatch(processNameArr, names);
            if (sim <= 3 && sim != -1)
                return true;
        }
        return false;
    }
    
    public String[] filterAnswer(String[] answer)
    {
        ArrayList<String> validAns = new ArrayList<String>();
        for (int i = 0; i < answer.length; i++)
        {
            if (isExistProcess(answer[i]))
                validAns.add(answer[i]);
        }
        
        String[] array = validAns.toArray(new String[validAns.size()]);
        return array;
    }
    
    public String predictAnswer(String questionSentence, String[] processNames) throws Exception {
        return trainAndPredict(processNames, questionSentence);
    }

    public void evaluate(Instances trainingData) throws Exception {
        Classifier c1 = new SMO();
        Evaluation eval = new Evaluation(trainingData);
        eval.crossValidateModel(c1, trainingData, 10, new Random(1));
        System.out.println("Estimated Accuracy: " + Double.toString(eval.pctCorrect()));
    }

    public static void main(String[] args) throws FileNotFoundException, Exception {
        Baseline baseline = new Baseline("./data/all_process_ds.tsv", "./data/question.tsv");
        baseline.loadProcessData();
        baseline.generateFeatureVectors();
        baseline.loadQuestionData();
        
        int nbCorrect = 0;
        for (int i = 0; i < baseline.ques.getNbQuestion(); i++)
        {
            QuestionData qData = baseline.ques.getQuestionData(i);
            String q = qData.getQuestionSentence();
            String[] answers = qData.getAnswers();
            String correctAnswer = qData.getCorrectAnswer().trim();
            String[] filteredAnswer = baseline.filterAnswer(answers);
            System.out.println(i+" Q :"+q);
            String predictedAnswer = "";
            if (filteredAnswer.length == 0)
            {
              //  System.out.println("NO KNOWLEDGE");
            }
            else if (filteredAnswer.length == 1)
            {
                predictedAnswer = filteredAnswer[0];
            }
            else
            {
                predictedAnswer = baseline.predictAnswer(q, filteredAnswer);
            }
            System.out.println(predictedAnswer);
            if (predictedAnswer.equalsIgnoreCase(correctAnswer.trim()))
            {
                //System.out.println("CORRECT!");
                nbCorrect++;
            }
        }    
        System.out.println("% correct : "+(nbCorrect/(baseline.ques.getNbQuestion() * 1.0) ));
    }

}
