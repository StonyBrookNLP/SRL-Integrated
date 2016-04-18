/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import qa.StanfordDepParserSingleton;
import qa.StanfordTokenizerSingleton;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgProcessAnnotationData;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.Sentence;
import se.lth.cs.srl.io.SentenceWriter;

/**
 *
 * @author slouvan
 */
// FROM BRAT AND GOOGLE SHEET IDEALLY
// For now, from Google Sheet
public class SpockDataReader {

    public HashMap<String, String> fieldMap = new HashMap<String, String>();
    HashMap<String, Integer> fieldIdxMap = new HashMap<String, Integer>();
    String processFileName;
    ArrayList<Sentence> sentences = new ArrayList<Sentence>();
    boolean skipNotAnnotated = true;
    boolean isTestingFile = false;

    public SpockDataReader(String processFileName, String configFileName, boolean isTestingFile) throws FileNotFoundException {
        this.processFileName = processFileName;
        readConfig(configFileName);
        this.isTestingFile = isTestingFile;
    }

    public SpockDataReader() {

    }

    public void readConfig(String configFileName) throws FileNotFoundException {
        String[] fields = FileUtil.readLinesFromFile(configFileName);
        for (int i = 0; i < fields.length; i++) {
            fieldMap.put(fields[i].split("\t")[0], fields[i].split("\t")[1]);
        }
    }

    private void mapFieldIdx(String fileHeader[]) {
        for (int i = 0; i < fileHeader.length; i++) {
            fieldIdxMap.put(fileHeader[i], i);
        }
    }

    public static ArrayList<Integer> getIdxMatchesv2(String[] targetPattern, String[] tokenizedSentence) {
        boolean inRegion = false;
        int matchStart = 0;
        int matchEnd = targetPattern.length;
        ArrayList<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < tokenizedSentence.length && matchStart < matchEnd; i++) {
            if (tokenizedSentence[i].equalsIgnoreCase(targetPattern[matchStart])) {//&& !idxs.contains(i + 1)) {
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
                //System.out.println("ERROR : CANNOT FIND \"" + Arrays.toString(targetPattern) + "\" IN THE SENTENCE");
            }
            return null;
        }
    }

    public Set<String> getRoleLabels() {
        ArrayList<String> roleLabels = new ArrayList<String>();
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sent = sentences.get(i);
            roleLabels.addAll(sent.getAllUniqueRoleLabel());
        }
        Set<String> uniqueRoleLabels = new HashSet<String>(roleLabels);

        return uniqueRoleLabels;
    }

    public static Set<String> getRoleLabels(ArrayList<Sentence> arrSentence) {
        ArrayList<String> roleLabels = new ArrayList<String>();
        for (int i = 0; i < arrSentence.size(); i++) {
            Sentence sent = arrSentence.get(i);
            roleLabels.addAll(sent.getAllUniqueRoleLabel());
        }
        Set<String> uniqueRoleLabels = new HashSet<String>(roleLabels);

        return uniqueRoleLabels;
    }

    public void dumpData(String fileName) throws FileNotFoundException {

    }

    public void readData() throws FileNotFoundException, IOException {
        List<String[]> data = new ArrayList<>();
        data = FileUtil.readDataObject(processFileName, "\t");
        mapFieldIdx(data.get(0));
        data = data.subList(1, data.size());
        final Map<String, List<String[]>> sentenceMap = data.stream().collect(Collectors.groupingBy(row -> row[fieldIdxMap.get("sentence")]));
        String roles[] = fieldMap.get("role").split(":");

        int totalUniqueSentence = sentenceMap.keySet().size();
        int sentProcessed = 0;
        System.out.println("TOTAL UNIQUE SENTENCE : " + totalUniqueSentence);
        for (String sentenceStr : sentenceMap.keySet()) {
            Sentence sentence = new Sentence(sentenceStr);
            boolean isAnnotated = false;
            sentence.setRawText(sentenceStr);
            sentence.setProcess(sentenceMap.get(sentenceStr).get(0)[fieldIdxMap.get("process")]);
            HashMap<String, ArrayList<ArgumentSpan>> roleAnnotationSpan = new HashMap<String, ArrayList<ArgumentSpan>>();
            for (String[] sentenceData : sentenceMap.get(sentenceStr)) {
                // Adding argument span here
                for (int i = 0; i < roles.length; i++) {
                    int roleColumnIdx = fieldIdxMap.get(roles[i]);
                    ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
                    if (sentenceData[roleColumnIdx].length() > 0) { // IF role filler is not empty
                        // Set role filler
                        List<String> tokens = StanfordTokenizerSingleton.getInstance().tokenize(sentenceData[roleColumnIdx].trim());
                        List<String> tokenizedRawText = StanfordTokenizerSingleton.getInstance().tokenize(sentence.getRawText());
                        String[] pattern = new String[tokens.size()];
                        tokens.toArray(pattern);
                        ArrayList<Integer> matchIdxs = getIdxMatchesv2(pattern, tokenizedRawText.toArray(new String[tokenizedRawText.size()]));
                        DependencyTree tree = StanfordDepParserSingleton.getInstance().parse(sentence.getRawText());
                        ArrayList<DependencyNode> arrDepNodes = new ArrayList<DependencyNode>();

                        if (matchIdxs != null) {
                            for (int j = 1; j <= tree.lastKey(); j++) {
                                if (matchIdxs.contains(j)) {
                                    arrDepNodes.add(tree.get(j));
                                }
                            }
                        }
                        ArgumentSpan span = new ArgumentSpan(arrDepNodes, roles[i]);
                        int annotationIdx = fieldIdxMap.get("is" + roles[i]);
                        if (sentenceData[annotationIdx].length() > 0) { // IF contains annotation
                            if (matchIdxs != null) {
                                isAnnotated = true;
                                if (sentenceData[annotationIdx].equalsIgnoreCase("1")) {
                                    span.setAnnotatedLabel("1");
                                } else {
                                    span.setAnnotatedLabel("-1");
                                }
                            }
                            span.setPattern(sentenceMap.get(sentenceStr).get(0)[fieldIdxMap.get("pattern")]);
                            spans.add(span);
                        }
                        if (isTestingFile) {
                            if (matchIdxs != null) {
                                isAnnotated = true;
                                span.setAnnotatedLabel("-1");
                                span.setPattern(sentenceMap.get(sentenceStr).get(0)[fieldIdxMap.get("pattern")]);
                                spans.add(span);
                            }
                        }
                        // IF THIS IS A TESTING FILE THEN LABEL IT AS -1 
                    }
                    if (roleAnnotationSpan.get(roles[i]) == null) {
                        roleAnnotationSpan.put(roles[i], spans);
                    } else {
                        ArrayList<ArgumentSpan> existingSpans = roleAnnotationSpan.get(roles[i]);
                        existingSpans.addAll(spans);
                        roleAnnotationSpan.put(roles[i], existingSpans);
                    }
                }
            }
            if (skipNotAnnotated) {
                if (isAnnotated) {
                    sentence.setAnnotated(isAnnotated);
                    sentence.setRoleArgAnnotation(roleAnnotationSpan);
                    sentences.add(sentence);
                }
            } else {
                sentence.setRoleArgAnnotation(roleAnnotationSpan);
                sentences.add(sentence);
            }
            System.out.println("Sentence processed : " + (++sentProcessed));
        }

        // SET THE ID
        for (int i = 0; i < sentences.size(); i++) {
            sentences.get(i).setId(i);
            int argId = 0;
            ArrayList<ArgumentSpan> spans = sentences.get(i).getAllAnnotatedArgumentSpan();
            for (ArgumentSpan span : spans) {
                span.setId(argId++);
            }
        }
    }

    public ArrayList<Sentence> getSentences() {
        return sentences;
    }

    public void setSentences(ArrayList<Sentence> sentences) {
        this.sentences = sentences;
    }

    public void dumpFrameOriginalMap(String fileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        HashMap<String, HashSet<String>> processTargetPairs = new HashMap<>();
        for (Sentence sentence : sentences) {
            HashSet<String> currentTarget = new HashSet<>();
            if (processTargetPairs.get(sentence.getProcessName()) != null) {
                currentTarget = processTargetPairs.get(sentence.getProcessName());
            }
            currentTarget.add(sentence.getLexicalUnitAndPOSTag());
            processTargetPairs.put(sentence.getProcessName(), currentTarget);
        }
        for (String process : processTargetPairs.keySet()) {
            String frameName = process;
            HashSet<String> lexTargetsSet = processTargetPairs.get(process);
            String lexTarget = String.join(":", lexTargetsSet);
            writer.println(frameName + "\t" + lexTarget);
        }
        writer.close();
    }

    public void dumpFrameElements(String fileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        Set<String> labels = getRoleLabels();
        Map<String, List<Sentence>> processSentPair = sentences.stream().collect(Collectors.groupingBy(s -> s.getProcessName()));
        for (String process : processSentPair.keySet()) {
            writer.println(process + "\t" + String.join(":", labels));
        }
        writer.close();
    }

    public void generateLexicalUnitFile(String dirName, int frameStartID, int luStartID) throws IOException {
        boolean success = FileUtil.mkDir(dirName);
        if (success) {
            // iterate through each process, give them ID
            int frameID = frameStartID;
            int luID = luStartID;
            Map<String, List<Sentence>> procSentPair = sentences.stream().collect(Collectors.groupingBy(s -> s.getProcessName()));
            for (String process : procSentPair.keySet()) {
                System.out.println(process + "frameID " + (frameID));
                List<Sentence> sentenceArr = procSentPair.get(process);
                for (int i = 0; i < sentenceArr.size(); i++) {
                    Sentence currentSent = sentenceArr.get(i);
                    String lu = currentSent.getLexicalUnitFrame();
                    System.out.println("luID " + (luID));

                    // Create file here
                    PrintWriter xmlWriter = new PrintWriter(dirName + "/lu" + luID + ".xml");
                    xmlWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                            + "<?xml-stylesheet type=\"text/xsl\" href=\"lexUnit.xsl\"?>\n"
                            + "<lexUnit status=\"Finished_Initial\" POS=\"N\" name=\"" + lu + "\" ID=\"" + luID + "\" frame=\"" + process + "\" frameID=\"" + frameID + "\" totalAnnotated=\"13\" xsi:schemaLocation=\"../schema/lexUnit.xsd\" xmlns=\"http://framenet.icsi.berkeley.edu\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                            + "    <header>\n"
                            + "        <frame>\n"
                            + "            <FE fgColor=\"FFFFFF\" bgColor=\"9400D3\" type=\"Core\" abbrev=\"res\" name=\"result\"/>\n"
                            + "            <FE fgColor=\"FFFFFF\" bgColor=\"00008B\" type=\"Core\" abbrev=\"trig\" name=\"trigger\"/>\n"
                            + "            <FE fgColor=\"FFFFFF\" bgColor=\"FFA500\" type=\"Core\" abbrev=\"ena\" name=\"enabler\"/>\n"
                            + "            <FE fgColor=\"FFFFFF\" bgColor=\"0000FF\" type=\"Core\" abbrev=\"und\" name=\"undergoer\"/>\n"
                            + "        </frame>\n"
                            + "    </header>\n"
                            + "</lexUnit>");
                    xmlWriter.close();
                    luID++;
                }
                frameID++;
            }

        }
        // extract the lexical unit, give them ID
        // create the XML file as well
    }

    public static void dumpRawSentences(ArrayList<Sentence> arrSentence, String outFileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(outFileName);
        for (Sentence sent : arrSentence) {
            writer.println(sent.getRawText().trim());
        }
        writer.close();
    }

    public static void dumpSentenceLexTargetIdxs(ArrayList<Sentence> arrSentence, String outFileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(outFileName);
        for (Sentence sent : arrSentence) {
            System.out.println(sent.getRawText());
            Pair<List<String>, List<Integer>> targetIdxPair = null;
            boolean error = false;
            try {
                targetIdxPair = sent.getTargetLexAndIdxs();
            } catch (Exception e) {
                //System.out.println("Error : "+sent.getRawText());
                error = true;
            }
            if (!error) {
                String targets = Joiner.on("_").join(targetIdxPair.first);
                String idxs = Joiner.on("_").join(targetIdxPair.second);
                writer.println(sent.getRawText().trim() + "\t" + sent.getProcessName() + "\t" + targets + "\t" + idxs);
            }
        }
        writer.close();
    }

    public static void generateSEMAFORFrameAnnotation(ArrayList<Sentence> arrSentence, String frameElementsFileName, String rawSentencesFileName, int offset) throws FileNotFoundException {
        Set<String> roles = getRoleLabels(arrSentence); // without NONE!
        PrintWriter writer = new PrintWriter(frameElementsFileName);
        PrintWriter sentWriter = new PrintWriter(rawSentencesFileName);
        int sentCounter = 0;
        //int offset = 2780;
        System.out.println("Generating semafor frame annotation , size :" + arrSentence.size());
        for (int i = 0; i < arrSentence.size(); i++) {
            Sentence currentSentence = arrSentence.get(i);
            if (currentSentence.isAnnotated()) {
                // Is there a positive role?
                int cntRole = 0; // Number of roles
                String frame = currentSentence.getProcessName();
                String lexicalUnitFrames = currentSentence.getLexicalUnitFrame();
                String lexicalUnitIndexRange = currentSentence.getLexicalUnitFrameRange();
                String formLexicalUnitFrame = currentSentence.getLexicalUnitFormFrame();

                StringBuilder roleSpanStrBuilder = new StringBuilder();
                for (String role : roles) {
                    ArrayList<ArgumentSpan> spans = currentSentence.getMultiClassAnnotatedArgumentSpan(role, -1);
                    if (!spans.isEmpty()) {
                        cntRole++;
                        ArgumentSpan maxSpan = spans.stream().max(Comparator.comparing(arg -> arg.getEndIdx() - arg.getStartIdx() + 1)).get();
                        if (maxSpan.getStartIdx() != maxSpan.getEndIdx()) {
                            roleSpanStrBuilder.append(role).append("\t").append((maxSpan.getStartIdx() - 1) + ":" + (maxSpan.getEndIdx() - 1)).append("\t");
                        } else {
                            roleSpanStrBuilder.append(role).append("\t").append((maxSpan.getStartIdx() - 1)).append("\t");
                        }
                    }
                    // if there is more than one then select the longer one
                    // group by start + end id
                    // count number of roles
                    // lexical unit == process name
                    // sentence number
                    // role span pairs
                }
                if (cntRole > 0) {
                    StringBuilder frameElementsStrB = new StringBuilder();
                    frameElementsStrB.append(cntRole + +1).append("\t").append(frame).append("\t").append(lexicalUnitFrames).append("\t").
                            append(lexicalUnitIndexRange).append("\t").append(formLexicalUnitFrame).append("\t").append((sentCounter + offset)).
                            append("\t").append(roleSpanStrBuilder.toString().trim());
                    writer.println(frameElementsStrB.toString());
                    sentWriter.println(currentSentence.getRawText().trim());
                    sentCounter++;
                }
            }
        }
        writer.close();
        sentWriter.close();
    }

    public void generateSEMAFORFrameAnnotation(String frameElementsFileName, String rawSentencesFileName, int offset) throws FileNotFoundException {
        Set<String> roles = getRoleLabels(); // without NONE!
        PrintWriter writer = new PrintWriter(frameElementsFileName);
        PrintWriter sentWriter = new PrintWriter(rawSentencesFileName);
        int sentCounter = 0;
        //int offset = 2780;
        for (int i = 0; i < sentences.size(); i++) {
            Sentence currentSentence = sentences.get(i);
            if (currentSentence.isAnnotated()) {
                // Is there a positive role?
                int cntRole = 0; // Number of roles
                String frame = currentSentence.getProcessName();
                String lexicalUnitFrames = currentSentence.getLexicalUnitFrame();
                String lexicalUnitIndexRange = currentSentence.getLexicalUnitFrameRange();
                String formLexicalUnitFrame = currentSentence.getLexicalUnitFormFrame();

                StringBuilder roleSpanStrBuilder = new StringBuilder();
                for (String role : roles) {
                    ArrayList<ArgumentSpan> spans = currentSentence.getMultiClassAnnotatedArgumentSpan(role, -1);
                    if (!spans.isEmpty()) {
                        cntRole++;
                        ArgumentSpan maxSpan = spans.stream().max(Comparator.comparing(arg -> arg.getEndIdx() - arg.getStartIdx() + 1)).get();
                        if (maxSpan.getStartIdx() != maxSpan.getEndIdx()) {
                            roleSpanStrBuilder.append(role).append("\t").append((maxSpan.getStartIdx() - 1) + ":" + (maxSpan.getEndIdx() - 1)).append("\t");
                        } else {
                            roleSpanStrBuilder.append(role).append("\t").append((maxSpan.getStartIdx() - 1)).append("\t");
                        }
                    }
                    // if there is more than one then select the longer one
                    // group by start + end id
                    // count number of roles
                    // lexical unit == process name
                    // sentence number
                    // role span pairs
                }
                if (cntRole > 0) {
                    StringBuilder frameElementsStrB = new StringBuilder();
                    frameElementsStrB.append(cntRole + +1).append("\t").append(frame).append("\t").append(lexicalUnitFrames).append("\t").
                            append(lexicalUnitIndexRange).append("\t").append(formLexicalUnitFrame).append("\t").append((sentCounter + offset)).
                            append("\t").append(roleSpanStrBuilder.toString().trim());
                    writer.println(frameElementsStrB.toString());
                    sentWriter.println(currentSentence.getRawText().trim());
                    sentCounter++;
                }
            }
        }
        writer.close();
        sentWriter.close();
    }

    public void readProcessData() throws FileNotFoundException, IOException {
        /*String[] lines = FileUtil.readLinesFromFile(processFileName);

         Scanner scanner = new Scanner(new File(processFileName));
         String[] header = scanner.nextLine().split("\t");
         mapFieldIdx(header);
         while (scanner.hasNextLine()) {
         String line = scanner.nextLine();
         String fields[] = line.split("\t");
         ArgProcessAnnotationData procDat = new ArgProcessAnnotationData();
         procDat.setProcessName(fields[fieldIdxMap.get("process")]);
         procDat.setRawText(fields[fieldIdxMap.get("sentence")].trim());
         procDat.setQuery(fields[fieldIdxMap.get("query")]);
         procDat.setPattern(fields[fieldIdxMap.get("pattern")]);
         Sentence sentence = new Sentence(procDat.getRawText());
         // Role 
         String roleFields[] = fieldMap.get("role").split(":");

         // for each role
         // strictly one arg per role
         boolean annotated = false;
         HashMap<String, ArrayList<ArgumentSpan>> roleSpanMap = new HashMap<String, ArrayList<ArgumentSpan>>();
         for (int i = 0; i < roleFields.length; i++) {
         int roleColumnIdx = fieldIdxMap.get(roleFields[i]);
         ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
         if (fields[roleColumnIdx].length() > 0) { // IF role filler is not empty
         // Set role filler
         List<String> tokens = StanfordTokenizerSingleton.getInstance().tokenize(fields[roleColumnIdx].trim());
         List<String> tokenizedRawText = StanfordTokenizerSingleton.getInstance().tokenize(procDat.getRawText());
         String[] pattern = new String[tokens.size()];
         tokens.toArray(pattern);
         ArrayList<Integer> matchIdxs = getIdxMatchesv2(pattern, tokenizedRawText.toArray(new String[tokenizedRawText.size()]));
         DependencyTree tree = StanfordDepParserSingleton.getInstance().parse(procDat.getRawText());
         ArrayList<DependencyNode> arrDepNodes = new ArrayList<DependencyNode>();

         if (matchIdxs != null) {
         for (int j = 1; j <= tree.lastKey(); j++) {
         if (matchIdxs.contains(j)) {
         arrDepNodes.add(tree.get(j));
         }
         }
         }
         ArgumentSpan span = new ArgumentSpan(arrDepNodes, roleFields[i]);
         int annotationIdx = fieldIdxMap.get("is" + roleFields[i]);
         if (annotationIdx < fields.length) { // IF contains annotation
         if (matchIdxs != null) {

         if (fields[annotationIdx].equalsIgnoreCase("1")) {
         span.setAnnotatedLabel("1");
         } else {
         span.setAnnotatedLabel("-1");
         }
         annotated = true;
         }
         }
         spans.add(span);
         }
         roleSpanMap.put(roleFields[i], spans);
         }
         //sentence.setRoleArgMap(roleSpanMap);
         procDat.setSentence(sentence);
         procDat.setAnnotated(annotated);
         //      if not empty then set the role filler
         //      check whether annotation exists or not
         //          if annotation exist then
         //              record its indexes and annotation type
         //              add to array

         // Role annotation
         //procDataArr.add(procDat);
         }*/
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        SpockDataReader reader = new SpockDataReader("/home/slouvan/NetBeansProjects/SRL-Integrated/data/training_4_roles.tsv",
                "/home/slouvan/NetBeansProjects/SRL-Integrated/configFrameFile/config.txt", false);
        reader.readData();
        reader.generateSEMAFORFrameAnnotation("./data/cv.train.sentences.frame.elements", "./data/cv.train.sentences.txt", 2780);
        //reader.dumpFrameOriginalMap("./data/frame.original.sbu.map");
        //reader.dumpFrameElements("./data/frame.element.sbu.map");
        //reader.generateLexicalUnitFile("./data/lu", 2500, 16100);
        /*SpockDataReader reader = new SpockDataReader("/home/slouvan/NetBeansProjects/SRL-Integrated/data/training_4_roles.tsv",
         "/home/slouvan/NetBeansProjects/SRL-Integrated/configFrameFile/config.txt", true); // process, config, is testing
         reader.readData();
         ArrayList<Sentence> testSentences = reader.getSentences();
         Map<String, List<Sentence>> map = testSentences.stream().collect(Collectors.groupingBy(s -> s.getProcessName()));
         for (String process : map.keySet()) {
         System.out.println(process + "\t" + map.get(process).size());
         }*/
    }

}
