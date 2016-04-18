/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.sr.eval;

import Util.JSONDataUtil;
import Util.SentenceUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import sbu.srl.datastructure.ArgProcessAnnotationData;
import sbu.srl.datastructure.Sentence;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.ArgumentSpanDeserializer;
import sbu.srl.datastructure.ILPSRLDataDeserializer;
import sbu.srl.datastructure.JSONData;
import sbu.srl.datastructure.SentenceDeserializer;

/**
 *
 * @author slouvan
 */
// Take two input files : predicted and gold
// Compute P,R,F1
// Read all possible labels
// 
public class Evaluator {

    private String predictionFileName;
    private String goldFileName;

    public Evaluator(String predictionFileName, String goldFileName) {
        this.predictionFileName = predictionFileName;
        this.goldFileName = goldFileName;
    }

    public void evaluate(String goldJsonFile, String predictionJsonFile) throws IOException {
        final ArrayList<JSONData> goldArr = SentenceUtil.readJSONData(goldJsonFile, false);
        final ArrayList<JSONData> predArr = SentenceUtil.readJSONData(predictionJsonFile, true);
        // Read all possible roles + NONE
        Set<String> labels = JSONDataUtil.getAllUniqueRoleLabelFromJSON(goldArr);
        labels.add("NONE");
        HashMap<String, ArrayList<ArgumentSpan>> goldLabelArgPair = new HashMap<String, ArrayList<ArgumentSpan>>();
        HashMap<String, ArrayList<ArgumentSpan>> predLabelArgPair = new HashMap<String, ArrayList<ArgumentSpan>>();
        HashMap<String, String> sIDArgIDGoldLabelPair = new HashMap<String, String>();
        for (String roleLabel : labels) {
            goldLabelArgPair.put(roleLabel, new ArrayList<ArgumentSpan>());
            for (int i = 0; i < goldArr.size(); i++) {
                ArrayList<Sentence> goldSentences = goldArr.get(i).getSentence();
                for (int j = 0; j < goldSentences.size(); j++) {
                    Sentence goldSentence = goldSentences.get(j);
                    ArrayList<ArgumentSpan> goldSpans = goldSentence.getAllAnnotatedArgumentSpanFromJSON(roleLabel);
                    ArrayList<ArgumentSpan> existingSpans = goldLabelArgPair.get(roleLabel);
                    existingSpans.addAll(goldSpans);
                    goldLabelArgPair.put(roleLabel, existingSpans);
                    for (ArgumentSpan span : goldSpans) {
                        sIDArgIDGoldLabelPair.put(goldSentence.getId() + "_" + span.getStartIdxJSON() + "_" + span.getEndIdxJSON(), roleLabel);
                    }
                }
            }
        }
        HashMap<String, String> sIDArgIDPredLabelPair = new HashMap<String, String>();
        for (String roleLabel : labels) {
            predLabelArgPair.put(roleLabel, new ArrayList<ArgumentSpan>());
            for (int i = 0; i < predArr.size(); i++) {
                ArrayList<Sentence> predictedSentences = predArr.get(i).getSentence();
                for (int j = 0; j < predictedSentences.size(); j++) {
                    Sentence predictedSentence = predictedSentences.get(j);
                    ArrayList<ArgumentSpan> predictedSpans = (ArrayList<ArgumentSpan>) predictedSentence.getPredictedArgumentSpanJSON().stream().filter(d -> d.getRolePredicted().equalsIgnoreCase(roleLabel)).collect(toList());
                    ArrayList<ArgumentSpan> existingSpans = predLabelArgPair.get(roleLabel);
                    existingSpans.addAll(predictedSpans);
                    predLabelArgPair.put(roleLabel, existingSpans);
                    for (ArgumentSpan span : predictedSpans) {
                        sIDArgIDPredLabelPair.put(predictedSentence.getId() + "_" + span.getStartIdxJSON() + "_" + span.getEndIdxJSON(), roleLabel);
                    }
                }
            }
        }
        if (sIDArgIDPredLabelPair.keySet().size() != sIDArgIDGoldLabelPair.keySet().size()) {
            System.out.println("Evaluation dataset is wrong. Difference in gold and predicted");
            for (String key : sIDArgIDPredLabelPair.keySet()) {
                if (sIDArgIDGoldLabelPair.get(key) == null) {
                    System.out.println(key);
                }
            }
            System.exit(0);
        }

        HashMap<String, Double> confMatrix = new HashMap<String, Double>();
        List<String> labelsArr = new ArrayList<String>(labels);
        for (int i = 0; i < labelsArr.size(); i++) {
            for (int j = 0; j < labelsArr.size(); j++) {
                confMatrix.put(labelsArr.get(i) + "_" + labelsArr.get(j), 0.0);
            }
        }
        for (String roleLabel : labels) {
            for (String sIdStartEndId : sIDArgIDGoldLabelPair.keySet()) {
                if (sIDArgIDGoldLabelPair.get(sIdStartEndId).equalsIgnoreCase(roleLabel)) {
                    if (sIDArgIDGoldLabelPair.get(sIdStartEndId).equalsIgnoreCase("NONE") &&  sIDArgIDPredLabelPair.get(sIdStartEndId).equalsIgnoreCase("NONE"))
                        System.out.println(sIdStartEndId);
                    if (confMatrix.get(sIDArgIDGoldLabelPair.get(sIdStartEndId) + "_" + sIDArgIDPredLabelPair.get(sIdStartEndId)) == null) {
                        confMatrix.put(sIDArgIDGoldLabelPair.get(sIdStartEndId) + "_" + sIDArgIDPredLabelPair.get(sIdStartEndId),
                                1.0);
                    } else {
                        confMatrix.put(sIDArgIDGoldLabelPair.get(sIdStartEndId) + "_" + sIDArgIDPredLabelPair.get(sIdStartEndId),
                                confMatrix.get(sIDArgIDGoldLabelPair.get(sIdStartEndId) + "_" + sIDArgIDPredLabelPair.get(sIdStartEndId)) + 1.0);
                    }

                }
            }
        }

        Set<String> classNames = new HashSet<String>();
        for (String key : confMatrix.keySet()) {
            String[] classes = key.split("_");
            if (classes != null && classes.length > 0) {
                classNames.addAll(Arrays.asList(classes));
            }
        }
        List<String> sortedClassNames = new ArrayList<String>();
        sortedClassNames.addAll(classNames);
        Collections.sort(sortedClassNames);
        System.out.print(" ");
        for (String predictedClassName : sortedClassNames) {
            System.out.print("\t" + predictedClassName);
        }
        System.out.println();
        for (String actualClassName : sortedClassNames) {
            System.out.print(actualClassName);
            for (String predictedClassName : sortedClassNames) {
                Double value = confMatrix.get(actualClassName + "_" + predictedClassName);
                System.out.print("\t");
                if (value != null) {
                    System.out.print(value);
                }
            }
            System.out.println();
        }
        // Compute precision per class
        double numeratorMicroPrec = 0.0;
        double denominatorMicroPrec = 0.0;
        double numeratorMicroRec = 0.0;
        double denominatorMicroRec = 0.0;
        double numeratorMacroPrec = 0.0;
        double numeratorMacroRec = 0.0;
        for (int i = 0; i < labelsArr.size(); i++) {
            double tpCnt = confMatrix.get(labelsArr.get(i) + "_" + labelsArr.get(i));
            double precDenominator = tpCnt;
            double recallDenominator = tpCnt;
            for (int j = 0; j < labelsArr.size(); j++) {
                if (i != j) {
                    precDenominator += confMatrix.get(labelsArr.get(j) + "_" + labelsArr.get(i)); // FP
                    recallDenominator += confMatrix.get(labelsArr.get(i) + "_" + labelsArr.get(j)); // FN
                }
            }
            numeratorMacroPrec += tpCnt / precDenominator;
            numeratorMacroRec += tpCnt / recallDenominator;
            System.out.printf("Precision %-20s %-4.3f\n", labelsArr.get(i), (tpCnt / precDenominator));
            System.out.printf("Recall %-23s %-4.3f\n", labelsArr.get(i), (tpCnt / recallDenominator));
            numeratorMicroPrec += tpCnt;
            denominatorMicroPrec += precDenominator;
            denominatorMicroRec += recallDenominator;
        }
        // Compute recall per class
        System.out.println("");
        System.out.println("Macro Precision:" + (numeratorMacroPrec / 5));
        System.out.println("Macro Recall:" + (numeratorMacroRec / 5));
        System.out.println("Macro F1:" + (2 * ((numeratorMacroPrec / 5)) * ((numeratorMacroRec / 5)) / (((numeratorMacroPrec / 5)) + ((numeratorMacroRec / 5)))));
        System.out.println("Micro-F1 : " + numeratorMicroPrec / denominatorMicroPrec);

        // Micro average p
        // Micro average r
        // Micro average F1
    }

    public void evaluateFromJSON(String goldJsonFile, String predictionJsonFile) throws FileNotFoundException, IOException {
        String[] roles = {"undergoer", "enabler", "trigger", "result", "NONE"};
        final ArrayList<JSONData> goldArr = SentenceUtil.readJSONData(goldJsonFile, false);

        final ArrayList<JSONData> predArr = SentenceUtil.readJSONData(predictionJsonFile, true);
        double totalCorrect = 0;
        double totalGSSpan = 0;
        double totalPredictionSpan = 0;
        int totalUniquePrediction = 0;

        for (int i = 0; i < goldArr.size(); i++) {
            JSONData data = goldArr.get(i);
            JSONData predData = ((ArrayList<JSONData>) predArr.stream().filter(d -> d.getProcessName().equalsIgnoreCase(data.getProcessName())).collect(Collectors.toList())).get(0);
            ArrayList<Sentence> goldSentences = data.getSentence();
            ArrayList<Sentence> predictedSentences = predData.getSentence();
            for (Sentence sentence : goldSentences) {
                int sentenceID = sentence.getId();
                Sentence predictedSentence = predictedSentences.stream().filter(s -> s.getId() == sentenceID).collect(toList()).get(0);
                ArrayList<ArgumentSpan> goldSpans = (ArrayList<ArgumentSpan>) sentence.getAnnotatedArgumentSpanJSON().stream().filter(d -> d.getAnnotatedLabel().equalsIgnoreCase("1")).collect(Collectors.toList());
                ArrayList<ArgumentSpan> predictedSpans = (ArrayList<ArgumentSpan>) predictedSentence.getPredictedArgumentSpanJSON().stream().filter(d -> !d.getRolePredicted().equalsIgnoreCase("NONE")).collect(Collectors.toList());

                Map<String, List<ArgumentSpan>> goldRoleSpanPair = goldSpans.stream().collect(Collectors.groupingBy(ArgumentSpan::getAnnotatedRole));
                Map<String, List<ArgumentSpan>> predictedRoleSpanPair = predictedSpans.stream().collect(Collectors.groupingBy(ArgumentSpan::getRolePredicted));
                int totalSRLSpan = 0;
                for (String roleLabel : roles) {
                    ArrayList<ArgumentSpan> roleGoldSpans = new ArrayList<ArgumentSpan>();
                    ArrayList<ArgumentSpan> rolePredictedSpans = new ArrayList<ArgumentSpan>();
                    if (goldRoleSpanPair.get(roleLabel) != null) {
                        roleGoldSpans = (ArrayList<ArgumentSpan>) goldRoleSpanPair.get(roleLabel).stream().filter(d -> d.getAnnotatedLabel().equalsIgnoreCase("1")).collect(toList());
                        roleGoldSpans = makeUnique(roleGoldSpans);
                    }
                    if (predictedRoleSpanPair.get(roleLabel) != null) {
                        rolePredictedSpans = (ArrayList<ArgumentSpan>) predictedRoleSpanPair.get(roleLabel).stream().filter(d -> d.getRolePredicted().equalsIgnoreCase(roleLabel)).collect(toList());
                        rolePredictedSpans = makeUnique(rolePredictedSpans);
                        totalSRLSpan += rolePredictedSpans.size();
                    }
                    if (roleGoldSpans.size() > 0 && rolePredictedSpans.size() > 0) {
                        for (int j = 0; j < rolePredictedSpans.size(); j++) {
                            if (isExist(rolePredictedSpans.get(j), roleGoldSpans)) {
                                totalCorrect++;
                            } else {
                                //System.out.println(rolePredictedSpans.get(j));
                            }
                        }
                        totalPredictionSpan += rolePredictedSpans.size();
                        totalGSSpan += roleGoldSpans.size();
                    } else if (roleGoldSpans.size() > 0 && rolePredictedSpans.size() == 0) {
                        totalGSSpan += roleGoldSpans.size();
                        //System.out.println(roleGoldSpans);
                    } else if (rolePredictedSpans.size() > 0) {
                        totalPredictionSpan += rolePredictedSpans.size();
                        for (int j = 0; j < rolePredictedSpans.size(); j++) {
                            //System.out.println(rolePredictedSpans.get(j));
                        }
                    }
                    for (ArgumentSpan s : rolePredictedSpans) {
                        System.out.println(sentenceID + "\t" + s.getStartIdxJSON() + "\t" + s.getEndIdxJSON());
                    }
                }
                //System.out.println("");
                double precision = totalCorrect / totalPredictionSpan;
                double recall = totalCorrect / totalGSSpan;
                double f1 = (2 * precision * recall) / (precision + recall);
                //System.out.printf("[%.4f %10.4f %10.4f %d spans]", totalCorrect / totalPredictionSpan, totalCorrect / totalGSSpan, (2 * precision * recall) / (precision + recall), totalSRLSpan);
                //System.out.print(sentence.getRawText() + "\n");
            }

        }
        System.out.println(totalCorrect);
        System.out.println(totalPredictionSpan);
        System.out.println(totalGSSpan);
        double precision = totalCorrect / totalPredictionSpan;
        double recall = totalCorrect / totalGSSpan;
        double f1 = (2 * precision * recall) / (precision + recall);
        System.out.printf("\n%s %10s %10s\n", "P", "R", "F1");
        System.out.printf("\n%.4f\t%10.4f\t%10.4f\n", precision, recall, f1);
    }

    public void evaluateFromJSON(String jsonFileName) throws FileNotFoundException, IOException {
        String[] roles = {"undergoer", "enabler", "action", "result"};
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(JSONData.class, new ILPSRLDataDeserializer());
        gsonBuilder.registerTypeAdapter(Sentence.class, new SentenceDeserializer(false));
        gsonBuilder.registerTypeAdapter(ArgumentSpan.class, new ArgumentSpanDeserializer(true));
        final Gson gson = gsonBuilder.create();
        Reader reader = new InputStreamReader(new FileInputStream(jsonFileName));
        final ArrayList<JSONData> arr = gson.fromJson(reader, new com.google.gson.reflect.TypeToken<ArrayList<JSONData>>() {
        }.getType());

        double totalCorrect = 0;
        double totalGSSpan = 0;
        double totalPredictionSpan = 0;
        for (int i = 0; i < arr.size(); i++) {
            JSONData data = arr.get(i);
            ArrayList<Sentence> sentences = data.getSentence();
            for (Sentence sentence : sentences) {
                System.out.println(sentence.getRawText());
                ArrayList<ArgumentSpan> goldSpans = (ArrayList<ArgumentSpan>) sentence.getAnnotatedArgumentSpanJSON().stream().filter(d -> d.getAnnotatedLabel().equalsIgnoreCase("1")).collect(Collectors.toList());
                ArrayList<ArgumentSpan> predictedSpans = (ArrayList<ArgumentSpan>) sentence.getPredictedArgumentSpanJSON().stream().filter(d -> !d.getRolePredicted().equalsIgnoreCase("NONE")).collect(Collectors.toList());
                Map<String, List<ArgumentSpan>> goldRoleSpanPair = goldSpans.stream().collect(Collectors.groupingBy(ArgumentSpan::getAnnotatedRole));
                Map<String, List<ArgumentSpan>> predictedRoleSpanPair = predictedSpans.stream().collect(Collectors.groupingBy(ArgumentSpan::getRolePredicted));
                for (String roleLabel : roles) {
                    ArrayList<ArgumentSpan> roleGoldSpans = new ArrayList<ArgumentSpan>();
                    ArrayList<ArgumentSpan> rolePredictedSpans = new ArrayList<ArgumentSpan>();
                    if (goldRoleSpanPair.get(roleLabel) != null) {
                        roleGoldSpans = (ArrayList<ArgumentSpan>) goldRoleSpanPair.get(roleLabel).stream().filter(d -> d.getAnnotatedLabel().equalsIgnoreCase("1")).collect(toList());
                        roleGoldSpans = makeUnique(roleGoldSpans);
                    }
                    if (predictedRoleSpanPair.get(roleLabel) != null) {
                        rolePredictedSpans = (ArrayList<ArgumentSpan>) predictedRoleSpanPair.get(roleLabel).stream().filter(d -> d.getRolePredicted().equalsIgnoreCase(roleLabel)).collect(toList());
                        rolePredictedSpans = makeUnique(rolePredictedSpans);
                    }
                    if (roleGoldSpans.size() > 0 && rolePredictedSpans.size() > 0) {
                        for (int j = 0; j < rolePredictedSpans.size(); j++) {
                            if (isExist(rolePredictedSpans.get(j), roleGoldSpans)) {
                                totalCorrect++;
                            }
                        }
                        totalPredictionSpan += rolePredictedSpans.size();
                        totalGSSpan += roleGoldSpans.size();
                    } else if (roleGoldSpans.size() > 0 && rolePredictedSpans.size() == 0) {
                        totalGSSpan += roleGoldSpans.size();
                    } else if (rolePredictedSpans.size() > 0) {
                        totalPredictionSpan += rolePredictedSpans.size();
                    }

                }
                double precision = totalCorrect / totalPredictionSpan;
                double recall = totalCorrect / totalGSSpan;
                double f1 = (2 * precision * recall) / (precision + recall);
                System.out.printf("\n%.4f %10.4f %10.4f\n", totalCorrect / totalPredictionSpan, totalCorrect / totalGSSpan, (2 * precision * recall) / (precision + recall));
            }

        }
        double precision = totalCorrect / totalPredictionSpan;
        double recall = totalCorrect / totalGSSpan;
        double f1 = (2 * precision * recall) / (precision + recall);

        System.out.printf("\n%s %10s %10s\n", "P", "R", "F1");
        System.out.printf("\n%.4f %10.4f %10.4f\n", precision, recall, f1);

        reader.close();
    }

    public boolean isExist(ArgumentSpan targetSpan, ArrayList<ArgumentSpan> spans) {
        for (ArgumentSpan span : spans) {
            if (span.getStartIdxJSON() == targetSpan.getStartIdxJSON() && span.getEndIdxJSON() == targetSpan.getEndIdxJSON()) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<ArgumentSpan> makeUnique(ArrayList<ArgumentSpan> spans) {
        ArrayList<ArgumentSpan> unique = new ArrayList<ArgumentSpan>();
        for (int i = 0; i < spans.size(); i++) {
            if (!isExist(spans.get(i), unique)) {
                unique.add(spans.get(i));
            }
        }

        return unique;
    }

    public void evaluateSERFile() throws IOException, FileNotFoundException, ClassNotFoundException {
        /*ArrayList<ArgProcessAnnotationData> goldData = (ArrayList<ArgProcessAnnotationData>) FileUtil.deserializeFromFile(goldFileName);
         ArrayList<ArgProcessAnnotationData> predictedData = (ArrayList<ArgProcessAnnotationData>) FileUtil.deserializeFromFile(predictionFileName);
         int totalData = 0;
         int totalCorrect = 0;
         for (int i = 0; i < goldData.size(); i++)
         {
         ArgProcessAnnotationData currentGoldData = goldData.get(i);
         ArgProcessAnnotationData currentPredictedData = predictedData.get(i);
            
         ArrayList<String> roleNames = currentGoldData.getSentence().getAllUniqueRoleLabel();
         for (String roleName : roleNames)
         {
         ArrayList<ArgumentSpan> goldSpans = currentGoldData.getArgumentSpan(roleName);
         ArrayList<ArgumentSpan> predictedSpans = currentPredictedData.getArgumentSpan(roleName);
         for (int j = 0; j < goldSpans.size() ; j++)
         {
         if (goldSpans.get(j).getAnnotatedLabel().equalsIgnoreCase(predictedSpans.get(j).getAnnotatedLabel()))
         {
         totalCorrect++;
         }
         }
         totalData += goldSpans.size();
         }
         }
        
         // ADD precision, recall, F1
         System.out.println("ACCURACY : "+(totalCorrect/(1.0*totalData)));*/
    }

    public void evaluate(String dirName) throws FileNotFoundException, IOException, Exception {
        ArrayList<Sentence> goldSentences = new BRATReader("").getSentencesFromDir(dirName, "gold");
        ArrayList<Sentence> predictedSentences = new BRATReader("").getSentencesFromDir(dirName, "predict");

        if (goldSentences.size() != predictedSentences.size()) {
            throw new Exception("Number of gold and predicted are not the same!");
        }
        double totalCorrect = 0;
        double totalLabelGS = 0;
        double totalLabelSRL = 0;

        for (int i = 0; i < goldSentences.size(); i++) {
            Sentence gsSentence = goldSentences.get(i);
            Sentence predSentence = predictedSentences.get(i);
            // get all roles from the gold sentence
            for (int j = 1; j <= gsSentence.getNbToken(); j++) {
                ArrayList<String> gsLabels = gsSentence.getUniqueRoleLabel(j);
                ArrayList<String> predLabels = predSentence.getUniqueRoleLabel(j);
                totalLabelGS += gsLabels.size();
                totalLabelSRL += predLabels.size();
                for (int k = 0; k < predLabels.size(); k++) {
                    if (gsLabels.contains(predLabels.get(k))) {
                        totalCorrect++;
                    }
                }
            }
        }
        double precision = totalCorrect / totalLabelSRL;
        double recall = totalCorrect / totalLabelGS;
        double f1 = (2 * precision * recall) / (precision + recall);
        System.out.printf("\n%s %10s %10s\n", "P", "R", "F1");
        System.out.printf("\n%.4f %10.4f %10.4f\n", precision, recall, f1);
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        Evaluator evaluator = new Evaluator("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val/fold-3/test/test.argpredict.ser", "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val/fold-3/test/test.arggold.ser");
        //evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5/fold-5/test/test.srlpredict.json");
        //evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-12-12-5-fold-multiclass/fold-1/test/test.srlout.json",
        //        "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-12-12-5-fold-multiclass/fold-1/test/test.srlpredict.json");
        evaluator.evaluate("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-21-12-5-fold-multiclass-propbank-only/fold-1/test/test.srlout.json",
                           "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-21-12-5-fold-multiclass-propbank-only/fold-1/test/test.srlpredict.json");

        //evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-11-12-5-fold/fold-1/test/test.srlout.json",
        //        "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-11-12-5-fold/fold-1/test/test.srlpredict.json");
        //evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-11-12-5-fold/fold-1/test/test.srlout.json",
        //        "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-11-12-5-fold/fold-1/test/ilp_predict.json");
        //test.srlpredict.json
        //evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-09-12-60-40/fold-1/test/test.srlout.json",
        //        "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-09-12-60-40/fold-1/test/test.srlpredict.json");
        /*evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-60-40/fold-1/test/test.srlout.json",
         "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-60-40/fold-1/test/ilp_predict.json");*/
        /* System.out.println("Cross validation");
         evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-1/test/test.srlout.json",
         "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-1/test/test.srlpredict.json");
         evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-2/test/test.srlout.json",
         "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-2/test/test.srlpredict.json");
         evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-3/test/test.srlout.json",
         "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-3/test/test.srlpredict.json");
         evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-4/test/test.srlout.json",
         "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-4/test/test.srlpredict.json");
         evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-5/test/test.srlout.json",
         "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-5/test/test.srlpredict.json");*/
        //evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-07-12-fold-5-new/fold-1/test/test.srlout.json");
        /* evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-05-12/fold-3/test/1est.srlpredict.json");
         evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-05-12/fold-4/test/test.srlpredict.json");(
         evaluator.evaluateFromJSON("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-05-12/fold-5/test/test.srlpredict.json");*/
    }
}
