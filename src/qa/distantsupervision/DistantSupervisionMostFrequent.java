/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.distantsupervision;

import Util.ArrUtil;
import Util.GlobalV;
import Util.ProcessFrameUtil;
import Util.StringUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.StanfordDepParser;
import qa.StanfordDocumentProcessor;
import qa.StanfordTokenizer;
import qa.dep.DependencyTree;

/**
 *
 * @author samuellouvan
 */
public class DistantSupervisionMostFrequent {

    private ProcessFrameProcessor proc; // To load all the data from process frame data
    private String processFrameFilename;  // Process frame fileName
    private String corpusFile;    // Target corpus as the source for distant supervision
    private String newAnnotatedFrameFileName;
    private int SENT_LENGTH = 250;
    private ArrayList<String> triggers;
    private ArrayList<String> enablers;
    private ArrayList<String> undergoers;
    private ArrayList<String> results;
    private ArrayList<String> relevantSentences;
    private StanfordDocumentProcessor docProcessor;
    private StanfordTokenizer tokenizer;
    private String targetProcessName = "";
    private StanfordDepParser depParser;
    // Constructor

    public DistantSupervisionMostFrequent(String processFrameFilename, String corpusFile, String newAnnotatedFileName) {
        this.processFrameFilename = processFrameFilename;
        this.corpusFile = corpusFile;
        this.newAnnotatedFrameFileName = newAnnotatedFileName;

    }

    // Initialize
    // Load the data from the process frame file
    public void init() throws FileNotFoundException, IOException, ClassNotFoundException {
        proc = new ProcessFrameProcessor(this.processFrameFilename);
        proc.loadProcessData();
        relevantSentences = new ArrayList<String>();
        docProcessor = new StanfordDocumentProcessor();
        tokenizer = new StanfordTokenizer();
        depParser = new StanfordDepParser();
        undergoers = new ArrayList<String>();
        enablers = new ArrayList<String>();
        triggers = new ArrayList<String>();
        results = new ArrayList<String>();
    }

    // Load all role fillers
    public void loadRoleFillers(String processName) {
        System.out.println("START LOADING ROLE FILLERS");
        ArrayList<ProcessFrame> processFrames = proc.getProcessFrameByName(processName);
        targetProcessName = processFrames.get(0).getProcessName();
        undergoers.clear();
        triggers.clear();
        enablers.clear();
        results.clear();
        for (ProcessFrame p : processFrames) {

            undergoers.addAll(StringUtil.getTokenAsList(StringUtil.removeFunctionWordsFromRoleFillers(p.getUnderGoer()), ProcessFrameProcessor.SEPARATOR));
            triggers.addAll(StringUtil.getTokenAsList(StringUtil.removeFunctionWordsFromRoleFillers(p.getTrigger()), ProcessFrameProcessor.SEPARATOR));
            enablers.addAll(StringUtil.getTokenAsList(StringUtil.removeFunctionWordsFromRoleFillers(p.getEnabler()), ProcessFrameProcessor.SEPARATOR));
            results.addAll(StringUtil.getTokenAsList(StringUtil.removeFunctionWordsFromRoleFillers(p.getResult()), ProcessFrameProcessor.SEPARATOR));

            /*if (undergoers.size() == 1 && undergoers.get(0).length() == 0)
             undergoers.clear();
             undergoers.addAll(StringUtil.getTokenAsList(p.getUnderGoer(), ProcessFrameProcessor.SEPARATOR));*/
            if (triggers.size() == 1 && triggers.get(0).length() == 0) {
                triggers.clear();
            }
            triggers.addAll(StringUtil.getTokenAsList(p.getTrigger(), ProcessFrameProcessor.SEPARATOR));
            /*if (enablers.size() == 1 && enablers.get(0).length() == 0)
             enablers.clear();
             enablers.addAll(StringUtil.getTokenAsList(p.getEnabler(), ProcessFrameProcessor.SEPARATOR));
             if (results.size() == 1 && results.get(0).length() == 0)
             results.clear();
             results.addAll(StringUtil.getTokenAsList(p.getResult(), ProcessFrameProcessor.SEPARATOR));*/
        }
        System.out.println("END LOADING ROLE FILLERS");

    }

    // Get the sentences from the target corpus 
    public ArrayList<String> getSentencesFromCorpus() throws FileNotFoundException {
        StringBuilder docStr = new StringBuilder();
        Scanner scanner = new Scanner(new File(this.corpusFile));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.isEmpty()) {
                docStr.append(line);
            }
        }
        return docProcessor.getSentences(docStr.toString());
    }

    public ArrayList<String> filterSentence(ArrayList<String> sentences) {
        System.out.println("Filtering");
        ArrayList<String> filteredSentence = new ArrayList<String>();
        for (String sentence : sentences) {
            List<String> tokens = tokenizer.tokenize(sentence);
            if (StringUtil.isValidSentence(sentence, tokens)) {
                String cleanedSent = sentence.replaceAll("-LRB-", "(");
                cleanedSent = cleanedSent.replaceAll("-LSB", "(");
                cleanedSent = cleanedSent.replaceAll("-RSB", ")");
                cleanedSent = cleanedSent.replaceAll("-RRB-", ")");

                cleanedSent = cleanedSent.replace(".", " ");
                cleanedSent = cleanedSent.replaceAll("\"", "");
                cleanedSent = cleanedSent.trim();
                for (int j = cleanedSent.length() - 1; j > 0; j--) {
                    if (Character.isAlphabetic(cleanedSent.charAt(j))) {
                        cleanedSent = cleanedSent.substring(0, j + 1);
                        cleanedSent += ".";
                        break;
                    }
                }

                if (!filteredSentence.contains(cleanedSent) && cleanedSent.length() > 5) {
                    filteredSentence.add(cleanedSent);
                }
            }
        }
        System.out.println("End Filtering");
        return filteredSentence;
    }

    public ArrayList<Integer> getIdxMatches(String[] targetPattern, String[] tokenizedSentence) {
        boolean inRegion = false;
        int matchStart = 0;
        int matchEnd = targetPattern.length;
        ArrayList<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < tokenizedSentence.length && matchStart < matchEnd; i++) {
            if (tokenizedSentence[i].equalsIgnoreCase(targetPattern[matchStart])) {
                idx.add(i); // because ConLL index starts from 1 
                if (!inRegion) {
                    inRegion = true;
                }
                matchStart++;
            } else {
                if (inRegion) {
                    inRegion = false;
                    idx.clear();
                    matchStart--;
                }
            }
        }
        if (matchStart == matchEnd) {
            return idx;
        } else {
            if (targetPattern[0].length() > 0) {
                // System.out.println(Arrays.toString(tokenizedSentence));
                //System.out.println("ERROR : CANNOT FIND \"" + Arrays.toString(targetPattern) + "\" IN THE SENTENCE");
            }
            return null;
        }
    }

    public ArrayList<Integer> label(String roleFillers, String sentence, DependencyTree tree) {
        List<String> tokens = tokenizer.tokenize(sentence);
        String[] tokensArr = tokens.toArray(new String[tokens.size()]);
        String[] fillers = roleFillers.split("\\s+");
        ArrayList<Integer> idxMatch = new ArrayList<Integer>();

        idxMatch = getIdxMatches(fillers, tokensArr);
        return idxMatch;

    }

    public boolean isEmptyRoleFillers(ArrayList<ArrayList<String>> roleFillers) {
        return roleFillers.get(0).size() == 0 && roleFillers.get(1).size() == 0 && roleFillers.get(2).size() == 0;
    }

    public int[] determineSequence(ArrayList<String> possibleUndergoers, ArrayList<String> possibleEnablers, ArrayList<String> possibleResults) {
        ArrayList<Integer> largestFromEach = new ArrayList<Integer>();

        if (possibleUndergoers.size() > 0) {
            largestFromEach.add(possibleUndergoers.get(0).length());
        } else {
            largestFromEach.add(-1);
        }
        if (possibleEnablers.size() > 0) {
            largestFromEach.add(possibleEnablers.get(0).length());
        } else {
            largestFromEach.add(-1);
        }
        if (possibleResults.size() > 0) {
            largestFromEach.add(possibleResults.get(0).length());
        } else {
            largestFromEach.add(-1);
        }
        ArrayList<Integer> sortedSeq = new ArrayList<Integer>();
        for (Integer i : largestFromEach) {
            sortedSeq.add(i);
        }

        Collections.sort(sortedSeq, Collections.reverseOrder());
        if (sortedSeq.get(0) == sortedSeq.get(1) && sortedSeq.get(1) == sortedSeq.get(2)) {
            return new int[]{0, 1, 2};
        } else if (sortedSeq.get(0) == sortedSeq.get(1) && sortedSeq.get(1) > sortedSeq.get(2)) {
            int lastIdx = largestFromEach.indexOf(sortedSeq.get(2));
            if (lastIdx == 0) {
                return new int[]{1, 2, 0}; // enabler, result, undergoer
            } else if (lastIdx == 1) {
                return new int[]{0, 2, 1}; // undergoer, enabler, enaber
            } else {
                return new int[]{0, 1, 2}; // result, undergoer, result
            }
        } else {
            if (sortedSeq.get(1) == sortedSeq.get(2)) {
                int firstIdx = largestFromEach.indexOf(sortedSeq.get(0));
                if (firstIdx == 0) {
                    return new int[]{0, 1, 2};
                }
                if (firstIdx == 1) {
                    return new int[]{1, 0, 2};
                }
                if (firstIdx == 2) {
                    return new int[]{2, 0, 1};
                }

            }
            int firstIdx = largestFromEach.indexOf(sortedSeq.get(0));
            int midIdx = largestFromEach.indexOf(sortedSeq.get(1));
            int lastIdx = largestFromEach.indexOf(sortedSeq.get(2));
            return new int[]{firstIdx, midIdx, lastIdx};

        }

    }

    public ArrayList<ArrayList<String>> labelRoleFiller(String sentence, DependencyTree sentTree, ArrayList<String> undergoer, ArrayList<String> enabler, ArrayList<String> result, ArrayList<Integer> trigger) {
        ArrayList<Integer> labeledIdx = new ArrayList<Integer>();
        if (trigger.size() > 0) {
            labeledIdx.addAll(trigger);
        }
        // Generate all possible role fillers
        // Sort them
        ArrayList<ArrayList<String>> roleFillers = new ArrayList<ArrayList<String>>(3);
        ArrayList<ArrayList<Integer>> roleFillersIdx = new ArrayList<ArrayList<Integer>>(3);
        ArrayList<String> possibleUndergoers = StringUtil.generateSubsetFromArr(undergoer);
        ArrayList<String> possibleEnablers = StringUtil.generateSubsetFromArr(enabler);
        ArrayList<String> possibleResults = StringUtil.generateSubsetFromArr(result);
        roleFillers.add(possibleUndergoers);
        roleFillers.add(possibleEnablers);
        roleFillers.add(possibleResults);
        roleFillersIdx.add(new ArrayList<Integer>());
        roleFillersIdx.add(new ArrayList<Integer>());
        roleFillersIdx.add(new ArrayList<Integer>());

        boolean stop = false;
        int count = 0;
        int[] seqs = determineSequence(possibleUndergoers, possibleEnablers, possibleResults);
        while (!isEmptyRoleFillers(roleFillers)) {
            ArrayList<String> fillers = roleFillers.get(seqs[count]);
            if (fillers.size() > 0) {
                ArrayList<Integer> idxs = label(fillers.get(0), sentence, sentTree);
                fillers.remove(0);
                roleFillers.set(seqs[count], fillers);
                if (idxs != null && !ArrUtil.isIntersect(idxs, labeledIdx)) {
                    ArrayList<Integer> currentRoleIdx = roleFillersIdx.get(seqs[count]);
                    currentRoleIdx.addAll(idxs);
                    roleFillersIdx.set(seqs[count], currentRoleIdx);
                    labeledIdx.addAll(idxs);
                }
            }
            count++;
            if (count == 3) {
                count = 0;
            }
        }
        List<String> tokens = tokenizer.tokenize(sentence);
        ArrayList<ArrayList<String>> foundRoleFillers = new ArrayList<ArrayList<String>>();
        foundRoleFillers.add(new ArrayList<String>());
        foundRoleFillers.add(new ArrayList<String>());
        foundRoleFillers.add(new ArrayList<String>());

        for (int i = 0; i < 3; i++) {
            if (roleFillersIdx.get(i).size() > 0) {
                ArrayList<Integer> undergoerIdx = roleFillersIdx.get(i);
                ArrayList<String> arr = new ArrayList<String>();
                String res = "";
                int currentIdx = -1;
                for (int j = 0; j < undergoerIdx.size(); j++) {
                    if (res.equalsIgnoreCase("") || Math.abs(undergoerIdx.get(j) - currentIdx) == 1) {
                        res += tokens.get(undergoerIdx.get(j)) + " ";
                        currentIdx = undergoerIdx.get(j);
                    } else {
                        arr.add(res.trim());
                        res = "";
                    }
                }
                if (!arr.contains(res.trim())) {
                    arr.add(res.trim());
                }
                foundRoleFillers.set(i, arr);
            }
        }
        return foundRoleFillers;
    }

    public void extractSentence() throws FileNotFoundException, IOException {
        ArrayList<String> sentences = getSentencesFromCorpus();
        sentences = filterSentence(sentences);
        PrintWriter writer = new PrintWriter(this.newAnnotatedFrameFileName.replaceAll("_ds.tsv", "_sent"));
        for (String sentence : sentences) {
            writer.println(sentence);
            //System.out.println(sentence);
            /*try {
                DependencyTree depTree = depParser.parse(sentence);

                // If the sentence is related to the target process then check for occurrence of the role fillers
                ArrayList<String> matchesTrigger = StringUtil.getMatchStem(tokenizer.tokenize(sentence), triggers);
                ArrayList<Integer> triggerIdxs = new ArrayList<Integer>();
                for (String s : matchesTrigger) {
                    ArrayList<Integer> idsx = label(s, sentence, depTree);
                    if (idsx != null) {
                        triggerIdxs.addAll(idsx);
                    }
                }
                if (!matchesTrigger.isEmpty() && sentence.length() <= SENT_LENGTH) {

                    writer.println(sentence);
                }
            } catch (Exception e) {
                System.out.println("IGNORE BAD SENTENCE");
            }*/
        }
        writer.close();

    }

    public void annotateSentence() throws FileNotFoundException, IOException {
        ArrayList<String> sentences = getSentencesFromCorpus();
        sentences = filterSentence(sentences);
        ArrayList<ProcessFrame> newAnnotatedFrames = new ArrayList<ProcessFrame>();
        ArrayList<ProcessFrame> filteredOutFrames = new ArrayList<ProcessFrame>();
        PrintWriter writer = new PrintWriter(this.newAnnotatedFrameFileName.replaceAll("_ds.tsv", "_sent"));

        int cnt = 0;
        for (String sentence : sentences) {
            try {
                // Dependency Parse

                //System.out.println(sentence);
                DependencyTree depTree = depParser.parse(sentence);

                // If the sentence is related to the target process then check for occurrence of the role fillers
                ArrayList<String> matchesTrigger = StringUtil.getMatchStem(tokenizer.tokenize(sentence), triggers);
                ArrayList<Integer> triggerIdxs = new ArrayList<Integer>();
                for (String s : matchesTrigger) {
                    ArrayList<Integer> idsx = label(s, sentence, depTree);
                    if (idsx != null) {
                        triggerIdxs.addAll(idsx);
                    }
                }
                if (!matchesTrigger.isEmpty() && sentence.length() <= SENT_LENGTH) {
                    writer.println(sentence);
                    ArrayList<ArrayList<String>> labeledToken = labelRoleFiller(sentence, depTree, undergoers, enablers, results, triggerIdxs);

                    ArrayList<String> matchesUndergoer = labeledToken.get(0);
                    ArrayList<String> matchesEnabler = labeledToken.get(1);
                    ArrayList<String> matchesResult = labeledToken.get(2);

                    boolean validUndergoer = PatternChecker.isValidArgument(matchesUndergoer, matchesTrigger, depTree);
                    boolean validEnabler = PatternChecker.isValidArgument(matchesEnabler, matchesTrigger, depTree);
                    boolean validResult = PatternChecker.isValidArgument(matchesResult, matchesTrigger, depTree);

                    if (validUndergoer || validEnabler || validResult) {
                        System.out.println("SENTENCE :" + sentence);
                        System.out.println("TRIGGER : " + matchesTrigger);
                        if (validUndergoer) {
                            // check whether it meets the dependency relation
                            System.out.println("UNDERGOER :" + matchesUndergoer);
                        }
                        if (validResult) {
                            // check whether it meets the dependency relation
                            System.out.println("RESULTS :" + matchesResult);
                        }
                        if (validEnabler) {
                            // check whether it meets the dependency relation
                            System.out.println("ENABLERS :" + matchesEnabler);
                        }

                        ProcessFrame frame = ProcessFrameUtil.createProcessFrame(targetProcessName, matchesUndergoer, matchesEnabler, matchesTrigger, matchesResult, sentence);
                        newAnnotatedFrames.add(frame);

                        cnt++;
                    } else {
                        System.out.println("Filtered out");
                        System.out.println(sentence);
                        ProcessFrame frame = ProcessFrameUtil.createProcessFrame(targetProcessName, null, null, matchesTrigger, null, sentence);
                        filteredOutFrames.add(frame);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Additional Preprocessing ?
        writer.close();
        System.out.println(cnt);
        ProcessFrameUtil.dumpFramesToFile(newAnnotatedFrames, this.newAnnotatedFrameFileName);
        if (newAnnotatedFrames.size() == 0) {
            ProcessFrameUtil.dumpFramesToFile(filteredOutFrames, this.newAnnotatedFrameFileName + ".filtered.out");
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {

        File dir = new File(GlobalV.PROJECT_DIR + "/data/ds_most_frequent_7_06_2015_copy");
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getName();
            System.out.println(fileName);
            String processName = fileName.substring(0, fileName.indexOf(".")).split("_")[0].toLowerCase();
            if (fileName.contains("_out.txt")) {
                System.out.println(processName.toLowerCase());
                DistantSupervisionMostFrequent labeler = new DistantSupervisionMostFrequent("./data/most_frequent_7_june.tsv",
                        "./data/ds_most_frequent_7_06_2015_copy/" + files[i].getName(),
                        "./data/ds_most_frequent_7_06_2015_copy/" + fileName.substring(0, fileName.indexOf(".")) + "_ds.tsv");
                labeler.init();
                //labeler.loadRoleFillers(processName);
                labeler.extractSentence();
                //labeler.annotateSentence();
            }
        }

    }
}
