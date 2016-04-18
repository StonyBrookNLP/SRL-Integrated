/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.sr.eval;

import Util.SentenceUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.JSONData;
import sbu.srl.datastructure.Sentence;

/**
 *
 * @author slouvan
 */
public class Analyzer {

    public static boolean isExist(ArgumentSpan targetSpan, ArrayList<ArgumentSpan> spans) {
        for (ArgumentSpan span : spans) {
            if (span.getStartIdxJSON() == targetSpan.getStartIdxJSON() && span.getEndIdxJSON() == targetSpan.getEndIdxJSON()) {
                return true;
            }
        }

        return false;
    }

    public static ArrayList<ArgumentSpan> makeUnique(ArrayList<ArgumentSpan> spans) {
        ArrayList<ArgumentSpan> unique = new ArrayList<ArgumentSpan>();
        for (int i = 0; i < spans.size(); i++) {
            if (!isExist(spans.get(i), unique)) {
                unique.add(spans.get(i));
            }
        }

        return unique;
    }

    public static void generateAnalyzer(String goldFileName, String srlFileName, String ilpFileName) throws IOException {
        List<JSONData> goldJSONData = SentenceUtil.readJSONData(goldFileName, false);
        List<JSONData> srlJSONData = SentenceUtil.readJSONData(srlFileName, true);
        List<JSONData> ilpJSONData = SentenceUtil.readJSONData(ilpFileName, true);
        // for each process group
        for (int i = 0; i < goldJSONData.size(); i++) {
            JSONData currentData = goldJSONData.get(i);
            ArrayList<Sentence> goldSentences = goldJSONData.get(i).getSentence();
            ArrayList<Sentence> srlSentences = srlJSONData.stream().
                    filter(data -> data.getProcessName().equalsIgnoreCase(currentData.getProcessName()))
                    .collect(toList()).get(0).getSentence();
            ArrayList<Sentence> ilpSentences = ilpJSONData.stream().
                    filter(data -> data.getProcessName().equalsIgnoreCase(currentData.getProcessName()))
                    .collect(toList()).get(0).getSentence();
            for (int j = 0; j < goldSentences.size(); j++) {
                Sentence currentGoldSentence = goldSentences.get(j);
                int sID = goldSentences.get(j).getId();
                Sentence currentSRLSentence = srlSentences.stream().filter(s -> s.getId() == sID).findFirst().get();
                Sentence currentILPSentence = ilpSentences.stream().filter(s -> s.getId() == sID).findFirst().get();
                ArrayList<ArgumentSpan> goldSpans = (ArrayList<ArgumentSpan>) currentGoldSentence.getAnnotatedArgumentSpanJSON().stream().filter(d -> d.getAnnotatedLabel().equalsIgnoreCase("1")).collect(Collectors.toList());
                goldSpans = makeUnique(goldSpans);
                StringBuffer strBuff = new StringBuffer();

                for (ArgumentSpan goldSpan : goldSpans) {
                    strBuff.append(goldSpan.getTextJSON()).append("\t");
                    ArgumentSpan srlArg = currentSRLSentence.getPredictedArgumentSpanJSON().
                            stream().
                            filter(arg -> arg.getStartIdxJSON() == goldSpan.getStartIdx() && arg.getEndIdxJSON() == goldSpan.getEndIdx()).findFirst().get();
                    ArgumentSpan ilpArg = currentILPSentence.getPredictedArgumentSpanJSON().stream().
                            filter(arg -> arg.getStartIdxJSON() == goldSpan.getStartIdx() && arg.getEndIdxJSON() == goldSpan.getEndIdx()).findFirst().get();
                    strBuff.append(goldSpan.getAnnotatedRole()).append("\t");
                    strBuff.append(srlArg.getRolePredicted()).append("\t").append(ilpArg.getRolePredicted()).append("\t").append(currentGoldSentence.getRawText());
                    //strBuff.append(currentSRLSentence.getRawText());
                    System.out.println(strBuff.toString());
                    strBuff.setLength(0);
                }
            }
        }
        //     for each correct argument span in gold
        //        get labels check existence in srl, ilp
        //        NOT_EXTRACTED >> NONE 
    }

    public static void generateAnalyzer(String goldFileName, String srlFileName, boolean recallOriented) throws IOException {
        if (recallOriented) {
            List<JSONData> goldJSONData = SentenceUtil.readJSONData(goldFileName, false);
            List<JSONData> srlJSONData = SentenceUtil.readJSONData(srlFileName, true);
            List<JSONData> ilpJSONData = SentenceUtil.readJSONData("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-09-12-60-40-normalized-with-e/fold-1/test/ilp_predict.json", true);
            // for each process group
            for (int i = 0; i < goldJSONData.size(); i++) {
                JSONData currentData = goldJSONData.get(i);
                ArrayList<Sentence> goldSentences = goldJSONData.get(i).getSentence();
                ArrayList<Sentence> srlSentences = srlJSONData.stream().
                        filter(data -> data.getProcessName().equalsIgnoreCase(currentData.getProcessName()))
                        .collect(toList()).get(0).getSentence();
                ArrayList<Sentence> ilpSentences = ilpJSONData.stream().
                        filter(data -> data.getProcessName().equalsIgnoreCase(currentData.getProcessName()))
                        .collect(toList()).get(0).getSentence();
                for (int j = 0; j < goldSentences.size(); j++) {
                    Sentence currentGoldSentence = goldSentences.get(j);
                    int sID = goldSentences.get(j).getId();
                    Sentence currentSRLSentence = srlSentences.stream().filter(s -> s.getId() == sID).findFirst().get();
                    Sentence currentILPSentence = ilpSentences.stream().filter(s -> s.getId() == sID).findFirst().get();
                    ArrayList<ArgumentSpan> goldSpans = (ArrayList<ArgumentSpan>) currentGoldSentence.getAnnotatedArgumentSpanJSON().stream().filter(d -> d.getAnnotatedLabel().equalsIgnoreCase("1")).collect(Collectors.toList());
                    goldSpans = makeUnique(goldSpans);
                    StringBuffer strBuff = new StringBuffer();

                    for (ArgumentSpan goldSpan : goldSpans) {
                        strBuff.append(goldSpan.getTextJSON()).append("\t");
                        ArgumentSpan srlArg = currentSRLSentence.getPredictedArgumentSpanJSON().
                                stream().
                                filter(arg -> arg.getStartIdxJSON() == goldSpan.getStartIdx() && arg.getEndIdxJSON() == goldSpan.getEndIdx()).findFirst().get();
                        ArgumentSpan ilpArg = currentILPSentence.getPredictedArgumentSpanJSON().stream().
                                filter(arg -> arg.getStartIdxJSON() == goldSpan.getStartIdx() && arg.getEndIdxJSON() == goldSpan.getEndIdx()).findFirst().get();
                        strBuff.append(goldSpan.getAnnotatedRole()).append("\t");
                        strBuff.append(srlArg.getRolePredicted()).append("\t").append(ilpArg.getRolePredicted()).append("\t").append(currentGoldSentence.getRawText());
                        //strBuff.append(currentSRLSentence.getRawText());
                        System.out.println(strBuff.toString());
                        strBuff.setLength(0);
                    }
                    // SPECIAL CASE
                    goldSpans = (ArrayList<ArgumentSpan>) currentGoldSentence.getAnnotatedArgumentSpanJSON().stream().filter(d -> d.getAnnotatedLabel().equalsIgnoreCase("-1")).collect(Collectors.toList());
                    ArrayList<ArgumentSpan> uniqueNONECandidates = makeUnique(goldSpans);
                    for (int k = 0; k < uniqueNONECandidates.size(); k++) {
                        ArgumentSpan currentCandidate = uniqueNONECandidates.get(k);
                        int nbRole = goldSpans.stream().filter(d -> d.getStartIdx() == currentCandidate.getStartIdxJSON() && d.getEndIdx() == currentCandidate.getEndIdxJSON()).collect(Collectors.groupingBy(ArgumentSpan::getAnnotatedRole)).size();
                        if (nbRole == 4) {
                            ArgumentSpan srlArg = currentSRLSentence.getPredictedArgumentSpanJSON().
                                    stream().
                                    filter(arg -> arg.getStartIdxJSON() == currentCandidate.getStartIdx() && arg.getEndIdxJSON() == currentCandidate.getEndIdx()).findFirst().get();
                            ArgumentSpan ilpArg = currentILPSentence.getPredictedArgumentSpanJSON().stream().
                                    filter(arg -> arg.getStartIdxJSON() == currentCandidate.getStartIdx() && arg.getEndIdxJSON() == currentCandidate.getEndIdx()).findFirst().get();
                            strBuff.append(currentCandidate.getTextJSON()).append("\t");
                            strBuff.append("NONE").append("\t");
                            strBuff.append(srlArg.getRolePredicted()).append("\t").append(ilpArg.getRolePredicted()).append("\t").append(currentGoldSentence.getRawText());
                            strBuff.append(currentSRLSentence.getRawText());
                            System.out.println(strBuff.toString());
                            strBuff.setLength(0);
                        }
                    }
                }
            }
        } else {
            List<JSONData> goldJSONData = SentenceUtil.readJSONData(goldFileName, false);
            List<JSONData> srlJSONData = SentenceUtil.readJSONData(srlFileName, true);
            List<JSONData> ilpJSONData = SentenceUtil.readJSONData("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-09-12-60-40-normalized-with-e/fold-1/test/ilp_predict.json", true);
            // for each process group
            for (int i = 0; i < srlJSONData.size(); i++) {
                JSONData currentData = srlJSONData.get(i);
                ArrayList<Sentence> srlSentences = srlJSONData.get(i).getSentence();
                ArrayList<Sentence> goldSentences = goldJSONData.stream().
                        filter(data -> data.getProcessName().equalsIgnoreCase(currentData.getProcessName()))
                        .collect(toList()).get(0).getSentence();
                ArrayList<Sentence> ilpSentences = ilpJSONData.stream().
                        filter(data -> data.getProcessName().equalsIgnoreCase(currentData.getProcessName()))
                        .collect(toList()).get(0).getSentence();
                for (int j = 0; j < srlSentences.size(); j++) {
                    Sentence currentSRLSentence = srlSentences.get(j);
                    int sID = srlSentences.get(j).getId();
                    Sentence currentGoldSentence = goldSentences.stream().filter(s -> s.getId() == sID).findFirst().get();
                    Sentence currentILPSentence = ilpSentences.stream().filter(s -> s.getId() == sID).findFirst().get();
                    ArrayList<ArgumentSpan> srlSpans = currentSRLSentence.getPredictedArgumentSpanJSON();
                    StringBuffer strBuff = new StringBuffer();

                    for (ArgumentSpan srlSpan : srlSpans) {
                        strBuff.append(srlSpan.getTextJSON()).append("\t");
                        ArrayList<ArgumentSpan> goldArgs = (ArrayList<ArgumentSpan>) currentGoldSentence.getAnnotatedArgumentSpanJSON()
                                .stream().filter(gold -> gold.getStartIdx() == srlSpan.getStartIdxJSON() && gold.getEndIdx() == srlSpan.getEndIdxJSON()).collect(toList());
                        boolean existInGold = goldArgs.stream().anyMatch((arg -> arg.getAnnotatedLabel().equalsIgnoreCase("1")));
                        strBuff.append(srlSpan.getRolePredicted()).append("\t");
                        if (existInGold) {
                            ArgumentSpan goldSpan = goldArgs.stream().filter(arg -> arg.getAnnotatedLabel().equalsIgnoreCase("1")).findFirst().get();
                            strBuff.append(goldSpan.getAnnotatedRole()).append("\t").append(currentSRLSentence.getRawText());
                        } else {
                            if (goldArgs.stream().anyMatch((arg -> arg.getAnnotatedLabel().equalsIgnoreCase("-1")))) {
                                Map<String, List<ArgumentSpan>> labels = goldArgs.stream().filter(arg -> arg.getAnnotatedLabel().equalsIgnoreCase("-1")).collect(Collectors.groupingBy(ArgumentSpan::getAnnotatedRole));
                                if (labels.size() == 4) {
                                    strBuff.append("NONE").append("\t").append(currentSRLSentence.getRawText());
                                } else {
                                    strBuff.append("TRUTH_UNKNOWN").append("\t").append(currentSRLSentence.getRawText());
                                }
                            } else {
                                strBuff.append("TRUTH_UNKNOWN").append("\t").append(currentSRLSentence.getRawText());
                            }
                        }

                        //strBuff.append(currentSRLSentence.getRawText());
                        System.out.println(strBuff.toString());
                        strBuff.setLength(0);
                    }
                }
            }
        }
        //     for each correct argument span in gold
        //        get labels check existence in srl, ilp
        //        NOT_EXTRACTED >> NONE 
    }

    public static void main(String[] args) throws IOException {
        //generateAnalyzer("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-10-12-60-40/fold-1/test/test.srlout.json",
        //        "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-10-12-60-40/fold-1/test/test.srlpredict.json", true);
        generateAnalyzer("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-09-12-60-40-normalized-with-e/fold-1/test/test.srlout.json",
                "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-09-12-60-40-normalized-with-e/fold-1/test/test.srlpredict.json", true);
    }
}
