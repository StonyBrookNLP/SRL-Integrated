/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import Util.ClearParserUtil;
import Util.StringUtil;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import qa.dep.DependencyTree;
import qa.util.FileUtil;

public class ProcessFrameProcessor {

    private String fileName;
    private StanfordLemmatizer slem = new StanfordLemmatizer();
    private ArrayList<ProcessFrame> procArr;
    private ArrayList<ProcessFrame> procArrA0;
    private ArrayList<ProcessFrame> procArrA1;
    private ArrayList<ProcessFrame> procArrA2;
    static final int PROCESS_NAME_IDX = 0;
    static final int UNDERGOER_IDX = 1;
    static final int ENABLER_IDX = 2;
    static final int TRIGGER_IDX = 3;
    static final int RESULT_IDX = 4;
    static final int UNDERSPECIFIED_IDX = 5;
    static final int SENTENCE_IDX = 6;
    public static final String SEPARATOR = "\\|";
    private HashMap<String, Integer> processCountPair = new HashMap<String, Integer>();

    public ProcessFrameProcessor(String fileName) {
        this.fileName = fileName;
        procArr = new ArrayList<ProcessFrame>();
        procArrA0 = new ArrayList<ProcessFrame>();
        procArrA1 = new ArrayList<ProcessFrame>();
        procArrA2 = new ArrayList<ProcessFrame>();
        //slem = new StanfordLemmatizer();
    }

    public boolean isHeader(String line) {
        String fields[] = line.split("\t");
        if (fields[0].equalsIgnoreCase("process") && fields[1].equalsIgnoreCase("undergoer")) {
            return true;
        }
        return false;
    }

    public void loadProcessData() throws FileNotFoundException, IOException, ClassNotFoundException {
        Scanner scanner = new Scanner(new File(this.fileName));
        procArr.clear();
        int cnt = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!isHeader(line)) {
                String[] columns = line.split("\t");
                ProcessFrame procFrame = new ProcessFrame();
                List<String> tokenized = slem.tokenize(columns[SENTENCE_IDX].trim());
                procFrame.setTokenizedText(tokenized.toArray(new String[tokenized.size()]));
                procFrame.setProcessName(columns[PROCESS_NAME_IDX]);
                procFrame.setUnderGoer(columns[UNDERGOER_IDX]);
                procFrame.setEnabler(columns[ENABLER_IDX]);
                procFrame.setTrigger(columns[TRIGGER_IDX]);
                procFrame.setResult(columns[RESULT_IDX]);
                procFrame.setUnderSpecified(columns[UNDERSPECIFIED_IDX]);
                procFrame.setRawText(columns[SENTENCE_IDX].trim());
                if (!processCountPair.containsKey(procFrame.getProcessName())) {
                    processCountPair.put(procFrame.getProcessName(), 1);
                } else {
                    processCountPair.put(procFrame.getProcessName(), processCountPair.get(procFrame.getProcessName().trim()) + 1);
                }

                procArr.add(procFrame);
                cnt++;
            }
        }
        focusProcessOnRole();
        System.out.println("END OF LOAD SENTENCES");
    }

    public HashMap<String, Integer> getProcessCount() {
        return processCountPair;
    }

    public int getDataCount(String processName) {
        return processCountPair.get(processName) == null ? 0 : processCountPair.get(processName);
    }

    public void focusProcessOnRole() throws IOException, ClassNotFoundException {
        for (ProcessFrame frame : procArr) {
            if (!frame.getUnderGoer().isEmpty()) {
                byte[] data = FileUtil.serialize(frame);
                ProcessFrame clone = (ProcessFrame) FileUtil.deserialize(data);
                clone.setEnabler("");
                clone.setResult("");
                procArrA0.add(clone);
            }
            if (!frame.getEnabler().isEmpty()) {
                byte[] data = FileUtil.serialize(frame);
                ProcessFrame clone = (ProcessFrame) FileUtil.deserialize(data);
                clone.setUnderGoer("");
                clone.setResult("");
                procArrA1.add(clone);
            }
            if (!frame.getResult().isEmpty()) {
                byte[] data = FileUtil.serialize(frame);
                ProcessFrame clone = (ProcessFrame) FileUtil.deserialize(data);
                clone.setUnderGoer("");
                clone.setEnabler("");
                procArrA2.add(clone);
            }
        }
    }

    

    public void toClearParserFormat(String clearParserFileName) throws FileNotFoundException, IOException {

        ArrayList<ProcessFrame> processFrames = getProcArr();
        PrintWriter writer = new PrintWriter(clearParserFileName);
        for (ProcessFrame p : processFrames) {
            String rawText = p.getRawText();

            rawText = rawText.replace(".", " ");
            rawText = rawText.replaceAll("\"", "");
            rawText = rawText.trim();
            rawText += ".";

            // update tokenized text here
            List<String> tokenized = slem.tokenize(rawText);
            p.setTokenizedText(tokenized.toArray(new String[tokenized.size()]));
            try {
                DependencyTree tree = StanfordDepParserSingleton.getInstance().parse(rawText);

                String conLLStr = ClearParserUtil.toClearParserFormat(tree, p);
                writer.println(conLLStr);
                writer.println();
            } catch (Exception e) {

            }

        }
        writer.close();
    }

    public void toClearParserFormat(String clearParserFileName, String role) throws FileNotFoundException, IOException {

        ArrayList<ProcessFrame> processFrames = new ArrayList<ProcessFrame>();
        if (role.equalsIgnoreCase("A0")) {
            processFrames = procArrA0;
        }
        if (role.equalsIgnoreCase("A1")) {
            processFrames = procArrA1;
        }
        if (role.equalsIgnoreCase("A2")) {
            processFrames = procArrA2;
        }

        PrintWriter writer = new PrintWriter(clearParserFileName);
        for (ProcessFrame p : processFrames) {
            String rawText = p.getRawText();

            rawText = rawText.replace(".", " ");
            rawText = rawText.replaceAll("\"", "");
            rawText = rawText.trim();
            rawText += ".";

            // update tokenized text here
            List<String> tokenized = slem.tokenize(rawText);
            p.setTokenizedText(tokenized.toArray(new String[tokenized.size()]));
            try {
                DependencyTree tree = StanfordDepParserSingleton.getInstance().parse(rawText);

                String conLLStr = ClearParserUtil.toClearParserFormat(tree, p);
                writer.println(conLLStr);
                writer.println();
            } catch (Exception e) {

            }

        }
        writer.close();
    }

    public ProcessFrame getProcessFrame(int idx) {
        return procArr.get(idx);
    }

    public String getTrigger(int sentenceCnt) {
        return procArr.get(sentenceCnt).getTrigger();
    }

    public String[] getTokenized(int sentenceCnt) {
        return procArr.get(sentenceCnt).getTokenizedText();
    }

    public ArrayList<ProcessFrame> getProcArr() {
        return procArr;
    }

    public ArrayList<ProcessFrame> getProcessFrameByName(String processName) {
        ArrayList<ProcessFrame> results = new ArrayList<ProcessFrame>();
        for (ProcessFrame p : this.getProcArr()) {
            String[] name = StringUtil.getTokenAsArr(p.getProcessName(), SEPARATOR);
            if (StringUtil.contains(processName, name)) {
                results.add(p);
            }
        }
        return results;
    }

    public ArrayList<Integer> getIdxMatches(String[] targetPattern, String[] tokenizedSentence) {
        boolean inRegion = false;
        int matchStart = 0;
        int matchEnd = targetPattern.length;
        ArrayList<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < tokenizedSentence.length && matchStart < matchEnd; i++) {
            if (tokenizedSentence[i].equalsIgnoreCase(targetPattern[matchStart])) {
                idx.add(i + 1); // because ConLL index starts from 1 
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
                System.out.println(Arrays.toString(tokenizedSentence));
                System.out.println("ERROR : CANNOT FIND \"" + Arrays.toString(targetPattern) + "\" IN THE SENTENCE");
            }
            return null;
        }
    }

    public ArrayList<Integer> getTriggerTokenIdx(int sentenceCnt) {
        String[] triggerItem = getTrigger(sentenceCnt).split("\\|");
        String[] tokenized = getTokenized(sentenceCnt);
        ArrayList<Integer> matchIdx = new ArrayList<Integer>();
        for (int i = 0; i < triggerItem.length; i++) {
            List<String> ls = slem.tokenize(triggerItem[i].trim());
            String[] triggerValues = ls.toArray(new String[ls.size()]);
            if (getIdxMatches(triggerValues, tokenized) != null) {
                matchIdx.addAll(getIdxMatches(triggerValues, tokenized));
            }
        }

        return matchIdx;
    }

    //TODO : add validity checking of the process frame file
    /*public boolean isValidData()
     {
        
     }*/
    public String getUndergoer(int sentenceCnt) {
        return procArr.get(sentenceCnt).getUnderGoer();
    }

    public ArrayList<Integer> getUndergoerTokenIdx(int sentenceCnt) {
        String[] undergoerItem = getUndergoer(sentenceCnt).split("\\|");
        String[] tokenized = getTokenized(sentenceCnt);
        ArrayList<Integer> matchIdx = new ArrayList<Integer>();
        for (int i = 0; i < undergoerItem.length; i++) {
            List<String> ls = slem.tokenize(undergoerItem[i].trim());
            String[] undergoerValues = ls.toArray(new String[ls.size()]);
            if (getIdxMatches(undergoerValues, tokenized) != null) {
                matchIdx.addAll(getIdxMatches(undergoerValues, tokenized));
            }
        }

        return matchIdx;
    }

    public String getEnabler(int sentenceCnt) {
        return procArr.get(sentenceCnt).getEnabler();
    }

    public ArrayList<Integer> getEnablerTokenIdx(int sentenceCnt) {
        String[] enablerItem = getEnabler(sentenceCnt).split("\\|");
        String[] tokenized = getTokenized(sentenceCnt);
        ArrayList<Integer> matchIdx = new ArrayList<Integer>();
        for (int i = 0; i < enablerItem.length; i++) {
            List<String> ls = slem.tokenize(enablerItem[i].trim());
            String[] enablerValues = ls.toArray(new String[ls.size()]);
            if (getIdxMatches(enablerValues, tokenized) != null) {
                matchIdx.addAll(getIdxMatches(enablerValues, tokenized));
            }
        }

        return matchIdx;
    }

    public String getResult(int sentenceCnt) {
        return procArr.get(sentenceCnt).getResult();
    }

    public ArrayList<Integer> getResultTokenIdx(int sentenceCnt) {
        String[] resultItem = getResult(sentenceCnt).split("\\|");
        String[] tokenized = getTokenized(sentenceCnt);
        ArrayList<Integer> matchIdx = new ArrayList<Integer>();
        for (int i = 0; i < resultItem.length; i++) {
            List<String> ls = slem.tokenize(resultItem[i].trim());
            String[] resultValues = ls.toArray(new String[ls.size()]);; // TOKENIZED STANFORD
            if (getIdxMatches(resultValues, tokenized) != null) {
                matchIdx.addAll(getIdxMatches(resultValues, tokenized));
            }
        }

        return matchIdx;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        ProcessFrameProcessor proc = new ProcessFrameProcessor("/Users/samuellouvan/NetBeansProjects/QA/data/process_50.tsv");
        proc.loadProcessData();
        proc.toClearParserFormat("/Users/samuellouvan/NetBeansProjects/QA/data/process_50.clearparser");
        //proc.loadSentences();
        //System.out.println(proc.getIdxMatches("samuel student".split("\\s+"),"samuel louvan is the most stupid phd samuel student".split("\\s+")));
    }
}
