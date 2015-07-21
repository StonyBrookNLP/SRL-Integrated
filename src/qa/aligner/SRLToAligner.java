/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.aligner;

import qa.experiment.*;
import Util.ArrUtil;
import Util.GlobalVariable;
import Util.ProcessFrameUtil;
import clear.dep.DepTree;
import clear.reader.SRLReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.RoleSpan;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.corpus.WordProbsPair;
import se.lth.cs.srl.io.AllCoNLL09Reader;
import se.lth.cs.srl.io.AllCoNLL09ReaderWScore;

/**
 *
 * @author samuellouvan
 */
public class SRLToAligner {

    String[] blackListProcess = {"Salivating", "composted", "decant_decanting", "dripping", "magneticseparation", "loosening", "momentum", "seafloorspreadingtheory", "sedimentation",
        "spear_spearing", "retract",
        "drop_dropping", "Feelsleepy", "harden", "positivetropism", "Resting", "separated",
        "revising", "sight"};
    /*  Generate the TSV format of the SRL prediction 
     Basically it will read every row from the source TSV, pair it with the SRL's prediction
     Then set the undergoer, enabler, result, and trigger
     After that dump it to TSV file
     */

    public void generateTsvForAligner(String sourceTsvFile, String clearparserPrediction, String outTsvFile) throws IOException, FileNotFoundException, ClassNotFoundException {
        ProcessFrameProcessor proc = new ProcessFrameProcessor(sourceTsvFile);
        proc.loadProcessData();
        ArrayList<ProcessFrame> frames = proc.getProcArr();

        for (int i = 0; i < frames.size(); i++) {
            frames.get(i).setEnabler("");
            frames.get(i).setUnderGoer("");
            frames.get(i).setResult("");

        }

        //TODO  USE MATE READER
        // 
        SRLReader srlReader = new SRLReader(clearparserPrediction, true);
        ArrayList<DepTree> trees = new ArrayList<DepTree>();
        DepTree currentTree = null;
        while ((currentTree = srlReader.nextTree()) != null) {
            trees.add(currentTree);
        }

        System.out.println(frames.size());
        System.out.println(trees.size());
        if (frames.size() != trees.size()) {
            System.out.println("NIGHTMARE");
            System.exit(0);
        }
        for (int i = 0; i < frames.size(); i++) {
            DepTree tree = trees.get(i);
            ArrayList<String> underGoers = tree.getRoleFillers("A0");
            ArrayList<String> enablers = tree.getRoleFillers("A1");
            ArrayList<String> results = tree.getRoleFillers("A2");
            frames.get(i).setUnderGoer(String.join(" ", underGoers));
            frames.get(i).setEnabler(String.join(" ", enablers));
            frames.get(i).setResult(String.join(" ", results));
        }

        ProcessFrameUtil.dumpFramesToFile(frames, outTsvFile);

    }

    public Sentence getCorrespondingSentence(String[] tokenizedText, ArrayList<Sentence> sentences) {

        StringBuffer sbTokenized = new StringBuffer();
        sbTokenized.append(String.join(" ", tokenizedText));
        StringBuffer sbSentence = new StringBuffer();
        double sim = 0.0;
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);
            sbSentence.setLength(0);
            for (int j = 1; j < sentence.size(); j++) {
                String form = sentence.get(j).getForm();
                if (form.equalsIgnoreCase("-RRB-")) {
                    sbSentence.append(") ");
                } else if (form.equalsIgnoreCase("-LRB-")) {
                    sbSentence.append("( ");
                } else {
                    sbSentence.append(sentence.get(j).getForm() + " ");
                }
            }

            double distance = StringUtils.getLevenshteinDistance(sbTokenized.toString(), sbSentence.toString());
            //System.out.println(distance+ " "+ distance /Math.max(sbTokenized.length(), sbSentence.length()));
            sim = (1 - (distance / Math.max(sbTokenized.length(), sbSentence.length()))) * 100;
            //System.out.println("Sent : " + sbSentence.toString());
            if (sim > 80) {
                //System.out.println(sbTokenized.toString());
                //System.out.println(sbSentence.toString());
                return sentence;
            }
            /*if (StringUtils.getLevenshteinDistance(sbTokenized.toString(), sbSentence.toString()) < 0.3 * sbTokenized.toString().length()) {
             return sentence;
             }*/
        }
        System.out.println(sbTokenized.toString());
        return null;
        /*for (int i = 0; i < sentences.size(); i++) {
         Sentence sentence = sentences.get(i);
         ArrayList<Word> words = new ArrayList<Word>();
         for (int j = 1; j < sentence.size(); j++) {
         words.add(sentence.get(j));
         }
         boolean equal = true;
         for (int k = 0; k < words.size() - 2; k++) {
         if (Pattern.matches("\\p{Punct}", tokenizedText[k]))
         {
                   
         }
         else if (!words.get(k).getDeprel().equalsIgnoreCase("punct") && !tokenizedText[k].equalsIgnoreCase(words.get(k).getForm())) {
         equal = false;
         break;
         }
         }
         if (equal) {
         return sentence;
         }
         }
         return null;*/
    }

    public void generateTsvForAlignerMergeVersion(String sourceTsvFile, String clearparserPrediction, String outTsvFile, boolean isQuestionFrame, boolean strictMode, boolean blacklistMode) throws IOException, FileNotFoundException, ClassNotFoundException {
        ProcessFrameProcessor proc = new ProcessFrameProcessor(sourceTsvFile);
        if (isQuestionFrame) {
            proc.setQuestionFrame(true);
        }
        proc.loadProcessData();
        ArrayList<ProcessFrame> frames = proc.getProcArr();

        AllCoNLL09Reader reader = new AllCoNLL09Reader(new File(clearparserPrediction));
        ArrayList<Sentence> sentences = new ArrayList<Sentence>(reader.readAll());
        if (strictMode) {
            if (frames.size() != sentences.size()) {
                System.out.println("NIGHTMARE 1");
                System.exit(0);
            }
        }
        ArrayList<String> blacklist = new ArrayList<String>();
        if (blacklistMode) {
            blacklist = new ArrayList(Arrays.asList(blackListProcess));
        }

        for (int i = 0; i < frames.size(); i++) {
            if (blacklistMode) {
                String normName = ProcessFrameUtil.normalizeProcessName(frames.get(i).getProcessName());
                if (blacklist.contains(normName)) {
                    continue;
                }
            }
            frames.get(i).setEnabler("");
            frames.get(i).setUnderGoer("");
            frames.get(i).setResult("");
            frames.get(i).setUnderSpecified("");
            String[] tokenized = frames.get(i).getTokenizedText();
            // get the corresponding sentence
            Sentence correspondingSent = getCorrespondingSentence(tokenized, sentences);
            if (correspondingSent == null) {
                System.out.println(Arrays.toString(tokenized));
                System.out.println("NIGHTMARE 2");
                //System.exit(0);
            } else {

                List<Predicate> predicates = correspondingSent.getPredicates();
                 ArrayList<Integer> undergoer = new ArrayList<Integer>();
                 ArrayList<Integer> enabler = new ArrayList<Integer>();
                 ArrayList<Integer> trigger = new ArrayList<Integer>();
                 ArrayList<Integer> result = new ArrayList<Integer>();

                 if (predicates != null) {
                 for (int j = 0; j < predicates.size(); j++) {
                 Predicate currentPred = predicates.get(j);
                 undergoer.addAll(currentPred.getRoleFillersIdxs("A0"));
                 enabler.addAll(currentPred.getRoleFillersIdxs("A1"));
                 result.addAll(currentPred.getRoleFillersIdxs("A2"));
                 trigger.add(currentPred.getIdx());
                 }
                 String undergoerValues = getRoleValues(undergoer, correspondingSent);
                 String enablerValues = getRoleValues(enabler, correspondingSent);
                 String triggerValues = getRoleValues(trigger, correspondingSent);
                 String resultValues = getRoleValues(result, correspondingSent);
                 frames.get(i).setUnderGoer(undergoerValues);
                 frames.get(i).setEnabler(enablerValues);
                 frames.get(i).setResult(resultValues);
                 frames.get(i).setTrigger(triggerValues);
                 }
            }
        }
        ProcessFrameUtil.dumpFramesToFile(frames, outTsvFile);
    }

    /*
    
     sasasas
    
     */
    public void generateQuestionAnswerFrameWithScore(String goldFrame, String parserPrediction, String predictionFrame, boolean isQuestionFrame,
            boolean strictMode, boolean blacklistMode) throws IOException, FileNotFoundException, ClassNotFoundException {
        ProcessFrameProcessor proc = new ProcessFrameProcessor(goldFrame);
        if (isQuestionFrame) {
            proc.setQuestionFrame(true);
        }
        proc.loadProcessData();
        ArrayList<ProcessFrame> frames = proc.getProcArr();

        AllCoNLL09ReaderWScore reader = new AllCoNLL09ReaderWScore(new File(parserPrediction));
        ArrayList<Sentence> sentences = new ArrayList<Sentence>(reader.readAll());
        if (strictMode) {
            if (frames.size() != sentences.size()) {
                System.out.println("NIGHTMARE 1");
                System.exit(0);
            }
        }
        ArrayList<String> blacklist = new ArrayList<String>();
        if (blacklistMode) {
            blacklist = new ArrayList(Arrays.asList(blackListProcess));
        }

        ArrayList<ProcessFrame> predictedFrames = new ArrayList<ProcessFrame>();
        for (int i = 0; i < frames.size(); i++) {
            if (blacklistMode) {
                String normName = ProcessFrameUtil.normalizeProcessName(frames.get(i).getProcessName());
                if (blacklist.contains(normName)) {
                    continue;
                }
            }
            frames.get(i).setEnabler("");
            frames.get(i).setUnderGoer("");
            frames.get(i).setResult("");
            String[] tokenized = frames.get(i).getTokenizedText();
            // get the corresponding sentence
            Sentence correspondingSent = getCorrespondingSentence(tokenized, sentences);
            if (correspondingSent == null) {
                System.out.println(Arrays.toString(tokenized));
                System.out.println("NIGHTMARE 2");
                //System.exit(0);
            } else {
                System.out.println(Arrays.toString(tokenized));
                List<Predicate> predicates = correspondingSent.getPredicates();
                if (predicates != null) {
                    for (int j = 0; j < predicates.size(); j++) {
                        Predicate currentPred = predicates.get(j);
                        HashMap<String, ArrayList<RoleSpan>> roleRoleSpanPair = new HashMap<String, ArrayList<RoleSpan>>();
                        for (String roleType : GlobalVariable.argumentLabels) {
                            ArrayList<RoleSpan> span = getRoleSpan(correspondingSent, currentPred, roleType);
                            if (span != null) {
                                roleRoleSpanPair.put(roleType, span);
                            }
                        }
                        ProcessFrame predictedFrame = constructProcessFrame(frames.get(i), roleRoleSpanPair);
                        predictedFrames.add(predictedFrame);
                    }
                }

            }
        }
        ProcessFrameUtil.dumpFramesToFileWScore(predictedFrames, predictionFrame);
    }

    private String getRoleValues(ArrayList<Integer> arr, Sentence sentence) {
        if (arr.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Collections.sort(arr);
        Set<Integer> uniqueIdx = new HashSet<Integer>(arr);
        ArrayList<Integer> idxs = new ArrayList<Integer>(uniqueIdx);
        Collections.sort(idxs);

        int prevIdx = idxs.get(0);
        String prevVal = sentence.get(prevIdx).getForm();
        sb.append(prevVal);
        for (int i = 1; i < idxs.size(); i++) {
            if (idxs.get(i) == prevIdx + 1) {
                prevIdx = idxs.get(i);
                sb.append(" ").append(sentence.get(idxs.get(i)).getForm());
            } else {
                prevIdx = idxs.get(i);
                sb.append("|").append(sentence.get(idxs.get(i)).getForm());
            }
        }

        return sb.toString();
    }

    private ArrayList<RoleSpan> getRoleSpan(Sentence correspondingSent, Predicate pred, String roleType) {
        System.out.println("Getting rolespan for " + roleType + " , Predicate : " + pred.getForm());
        ArrayList<RoleSpan> spans = new ArrayList<RoleSpan>();
        ArrayList<Integer> roleIdx = pred.getRoleFillersIdxs(roleType);
        if (roleType.equalsIgnoreCase("T")) {
            spans.add(new RoleSpan(pred.getForm(), new double[]{1.0, 0.0}, roleType));
            return spans;
        }
        if (roleIdx.size() == 0) {
            return null;
        }

        Collections.sort(roleIdx);
        int prevIdx = roleIdx.get(0);
        ArrayList<Word> currentSpan = new ArrayList<Word>();
        currentSpan.add(correspondingSent.get(prevIdx));

        for (int i = 1; i < roleIdx.size(); i++) {
            if (roleIdx.get(i) == prevIdx + 1) {
                currentSpan.add(correspondingSent.get(roleIdx.get(i)));
                prevIdx = roleIdx.get(i);
            } else {
                StringBuilder textSpan = new StringBuilder();
                double[] scores = new double[3];
                Arrays.fill(scores, 1);

                for (int j = 0; j < currentSpan.size(); j++) {
                    textSpan.append(currentSpan.get(j).getForm() + " ");
                    int currentWordIdx = currentSpan.get(j).getIdx();
                    ArrayList<WordProbsPair> wProbsArr = correspondingSent.argProbs.get(roleType);
                    WordProbsPair collect = wProbsArr.stream().filter(e -> e.getWord().getIdx() == currentWordIdx).collect(Collectors.toList()).get(0);
                    for (int k = 0; k < scores.length; k++) {
                        scores[k] *= collect.getScore(k);
                    }
                }
                spans.add(new RoleSpan(textSpan.toString().trim(), scores, roleType));
                currentSpan.clear();
                currentSpan.add(correspondingSent.get(roleIdx.get(i)));
                prevIdx = roleIdx.get(i);
            }
        }

        if (currentSpan.size() > 0) {
            StringBuilder textSpan = new StringBuilder();
            double[] scores = new double[3];
            Arrays.fill(scores, 1);
            for (int j = 0; j < currentSpan.size(); j++) {
                textSpan.append(currentSpan.get(j).getForm() + " ");
                int currentWordIdx = currentSpan.get(j).getIdx();
                ArrayList<WordProbsPair> wProbsArr = correspondingSent.argProbs.get(roleType);
                WordProbsPair collect = wProbsArr.stream().filter(e -> e.getWord().getIdx() == currentWordIdx).collect(Collectors.toList()).get(0);
                for (int k = 0; k < scores.length; k++) {
                    scores[k] *= collect.getScore(k);
                }
            }

            spans.add(new RoleSpan(textSpan.toString().trim(), scores, roleType));
            currentSpan.clear();

        }
        return spans;
    }

    private ProcessFrame constructProcessFrame(ProcessFrame frame, HashMap<String, ArrayList<RoleSpan>> roleRoleSpanPair) {
        final Comparator<RoleSpan> comp = (r1, r2) -> Double.compare(r1.getRoleScore(), r2.getRoleScore());
        ProcessFrame res = new ProcessFrame();
        res.setProcessName(frame.getProcessName());
        res.setTokenizedText(frame.getTokenizedText());
        res.setRawText(frame.getRawText());

        for (String argLabel : GlobalVariable.argumentLabels) {
            if (roleRoleSpanPair.get(argLabel) != null) {
                ArrayList<RoleSpan> spans = roleRoleSpanPair.get(argLabel);
                RoleSpan maxSpan = spans.stream().max(comp).get();
                if (argLabel.equalsIgnoreCase("A0")) {
                    res.setUnderGoer(maxSpan.getTextSpan());
                    res.setScores(0, maxSpan.getScores());
                }
                if (argLabel.equalsIgnoreCase("A1")) {
                    res.setEnabler(maxSpan.getTextSpan());
                    res.setScores(1, maxSpan.getScores());
                }
                if (argLabel.equalsIgnoreCase("T")) {
                    res.setTrigger(maxSpan.getTextSpan());
                    res.setScores(2, maxSpan.getScores());
                }
                if (argLabel.equalsIgnoreCase("A2")) {
                    res.setResult(maxSpan.getTextSpan());
                    res.setScores(3, maxSpan.getScores());
                }
            }
        }
        return res;
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException {
        SRLToAligner srlTA = new SRLToAligner();
        /*String outFile = "framesAutoDS.tsv";
         srlTA.generateTsvForAligner(GlobalVariable.PROJECT_DIR + "/data/SRLQAPipeDS/question.predict.cv.0.tsv",
         GlobalVariable.PROJECT_DIR + "/data/SRLQAPipeDS/question.predict.cv.0.clearparser",
         GlobalVariable.PROJECT_DIR + "/data/SRLQAPipeDS/" + outFile);*/

        //    public void generateQuestionAnswerFrameWithScore(String goldFrame, String parserPrediction, String predictionFrame, boolean isQuestionFrame,
        //    boolean strictMode, boolean blacklistMode) 
        // Test input : process_frame.tsv , SRL prediction w Score
        srlTA.generateQuestionAnswerFrameWithScore("./data/process_frame_june.tsv", "./data/all_scores_per_process.srl", "./data/predictedWScore.tsv", false, false, true);
        //srlTA.generateQuestionAnswerFrameWithScore("./data/question_frame_23_june.tsv", "./data/questionFramePredicted.parser.scores", "./data/predictedQWScore.tsv", true, false, true);
    }
}
