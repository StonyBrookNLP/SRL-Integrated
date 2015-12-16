/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import Util.CCGParserUtil;
import Util.ClearParserUtil;
import Util.Constant;
import Util.GlobalV;
import Util.ProcessFrameUtil;
import Util.StringUtil;
import com.sun.javafx.scene.text.TextSpan;
import edu.stanford.nlp.trees.Span;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import qa.ProcessFrame;
import static qa.ProcessFrame.getIdxMatchesv2;
import qa.ProcessFrameProcessor;
import qa.StanfordDepParserSingleton;
import qa.StanfordTokenizerSingleton;
import qa.srl.SRLWrapper;
import qa.util.FileUtil;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.corpus.Yield;
import se.lth.cs.srl.io.AllCoNLL09Reader;

/**
 *
 * @author slouvan
 */
public class ArgumentSpanExtractor {

    public static HashMap<String, ArrayList<String>> getArgumentSpanFromFile(ArrayList<ProcessFrame> frames, String annotationFileName, String inputFileName, String outputFileName, String modelFileName, int SRLType) throws FileNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        HashMap<String, ArrayList<String>> sentArgSpansPair = new HashMap<String, ArrayList<String>>();
        if (SRLType == Constant.SRL_MATE) {
            //new SRLWrapper().doPredict(inputFileName, outputFileName, modelFileName, SRLType);
            new SRLWrapper().doPredict(inputFileName, outputFileName, modelFileName, SRLType, true, false);
            AllCoNLL09Reader conLLReader = new AllCoNLL09Reader(new File(outputFileName));
            List<Sentence> sents = conLLReader.readAll();
            int cnt = 0;
            for (Sentence sent : sents) {
                ArrayList<String> spans = new ArrayList<String>();
                for (Predicate pred : sent.getPredicates()) {
                    SortedSet<Yield> yields = new TreeSet<Yield>();
                    spans.add(pred.getForm());
                    Map<Word, String> argmap = pred.getArgMap();
                    ArrayList<String> labels = new ArrayList<String>();
                    for (Word w : argmap.keySet()) {
                        if (!labels.contains(argmap.get(w))) {
                            labels.add(argmap.get(w));
                        }
                    }
                    for (int i = 0; i < labels.size(); i++) {
                        int idx = -1;
                        boolean first = true;
                        StringBuilder strB = new StringBuilder();
                        for (Word w : argmap.keySet()) {
                            if (argmap.get(w).equalsIgnoreCase(labels.get(i))) {
                                if (first) {
                                    idx = w.getIdx();
                                    strB.append(w.getForm()).append(" ");
                                    first = false;
                                } else {
                                    if (w.getIdx() == idx + 1) {
                                        strB.append(w.getForm()).append(" ");
                                    } else {
                                        if (!spans.contains(strB.toString().trim())) {
                                            System.out.println("Adding " + strB.toString().trim());
                                            spans.add(strB.toString().trim());
                                            idx = w.getIdx();
                                            strB.setLength(0);
                                            strB.append(w.getForm()).append(" ");
                                        }
                                    }
                                }
                            }
                        }
                        if (strB.length() > 0) {
                            if (!spans.contains(strB.toString().trim())) {
                                spans.add(strB.toString().trim());
                            }
                        }
                    }
                }
                sentArgSpansPair.put(frames.get(cnt).getRawText(), spans);
                cnt++;
            }
        } else if (SRLType == Constant.SRL_CCG) {
            // output raw text to a file
            new SRLWrapper().doPredict(inputFileName, outputFileName, modelFileName, SRLType, true, false);
            sentArgSpansPair = CCGParserUtil.getArgumentCandidates(outputFileName, annotationFileName);
            //String[] lines = FileUtil.readLinesFromFile(outputFileName);
            //System.out.println(lines.length);
            System.out.println("FINISH");
            // invoke easy SRL
            // read the predicate
            // read the arguments
        }
        return sentArgSpansPair;
    }

    
    
    public static ArrayList<String> getArgumentSpan(String rawText, int SRLType) throws IOException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // if MATE
        // SRL predict
        // convert raw text to CoNLL'09 format
        if (SRLType == Constant.SRL_MATE) {

            String CoNLL06Str = StanfordDepParserSingleton.getInstance().parseCoNLL(rawText);
            String CoNLL09Str = ClearParserUtil.fromConLL2006StrToCoNLL2009Str(CoNLL06Str);

            PrintWriter writer = new PrintWriter("temp.conll09");
            writer.println(CoNLL09Str);
            writer.close();

            // invoke SRL predict
            new SRLWrapper().doPredict("temp.conll09", "predicted.conll09", "./data/MATEPropBankModel/propbank.model", Constant.SRL_MATE, true, false);
            AllCoNLL09Reader conLLReader = new AllCoNLL09Reader(new File("predicted.conll09"));
            List<Sentence> sents = conLLReader.readAll();
            Sentence sent = sents.get(0);
            ArrayList<String> spans = new ArrayList<String>();
            for (Predicate pred : sent.getPredicates()) {
                SortedSet<Yield> yields = new TreeSet<Yield>();
                spans.add(pred.getForm());
                Map<Word, String> argmap = pred.getArgMap();
                ArrayList<String> labels = new ArrayList<String>();
                for (Word w : argmap.keySet()) {
                    if (!labels.contains(argmap.get(w))) {
                        labels.add(argmap.get(w));
                    }
                }
                for (int i = 0; i < labels.size(); i++) {
                    int idx = -1;
                    boolean first = true;
                    StringBuilder strB = new StringBuilder();
                    for (Word w : argmap.keySet()) {
                        if (argmap.get(w).equalsIgnoreCase(labels.get(i))) {
                            if (first) {
                                idx = w.getIdx();
                                strB.append(w.getForm()).append(" ");
                                first = false;
                            } else {
                                if (w.getIdx() == idx + 1) {
                                    strB.append(w.getForm()).append(" ");
                                } else {
                                    if (!spans.contains(strB.toString().trim())) {
                                        System.out.println("Adding " + strB.toString().trim());
                                        spans.add(strB.toString().trim());
                                        idx = w.getIdx();
                                        strB.setLength(0);
                                        strB.append(w.getForm()).append(" ");
                                    }
                                }
                            }
                        }
                    }
                    if (strB.length() > 0) {
                        if (!spans.contains(strB.toString().trim())) {
                            spans.add(strB.toString().trim());
                        }
                    }
                }
            }
            return spans;
        } else if (SRLType == Constant.SRL_CCG) {

        }
        return null;
    }

    public void generateArgumentCandidates(String frameFileName, String annotationFileName, String srlInputFileName, String srlOutputFileName, String srlModelName, int srlType, 
                                           String frameFileOut, String annotationFileOut) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        ProcessFrameProcessor proc = new ProcessFrameProcessor(frameFileName);
        proc.loadProcessData();
        ArrayList<ProcessFrame> frames = proc.getProcArr();
        ProcessFrameUtil.dumpRawTextToFile(frames, srlInputFileName);
        String[] annotations = FileUtil.readLinesFromFile(annotationFileName, true, "process");
        HashMap<String, ArrayList<String>> argSpans = getArgumentSpanFromFile(frames, annotationFileName, srlInputFileName, srlOutputFileName, srlModelName, srlType);
        ArrayList<String> newAnnotations = new ArrayList<String>(Arrays.asList(annotations));

        if (argSpans.size() == frames.size()) {
            System.out.println("ARGUMENT EXTRACTION FROM SRL IS GOOD");
        }
        ArrayList<ProcessFrame> newFrames = new ArrayList<ProcessFrame>();
        for (int i = 0; i < frames.size(); i++) {
            ProcessFrame currentFrame = frames.get(i);
            String rawText = currentFrame.getRawText();
            String pattern = annotations[i].split("\t")[1];
            String query = annotations[i].split("\t")[2];
            ArrayList<String> spans = argSpans.get(rawText + "#" + pattern);
            //newAnnotations.add(annotations[i]);
            boolean match = false;
            for (int j = 0; j < spans.size(); j++) {
                String currentSpan = spans.get(j);
                List<String> tokens = StanfordTokenizerSingleton.getInstance().tokenize(currentSpan);
                StringUtil.removePunctuationStartEnd(tokens);
                String[] targetPattern = new String[tokens.size()];
                tokens.toArray(targetPattern);
                ArrayList<Integer> matches = getIdxMatchesv2(targetPattern, currentFrame.getTokenizedText(), new ArrayList<Integer>());
                if (matches != null) {
                    ProcessFrame newFrame = new ProcessFrame();
                    newFrame.setProcessName(currentFrame.getProcessName());
                    newFrame.setRawText(rawText);
                    boolean notAllDuplicate = false;
                    for (int k = 0; k < GlobalV.labels.length; k++) {
                        // IF NOT EXIST IN THE CURRENT FRAME ON THE SAME ROLE THEN SET
                        if (!currentFrame.getRoleFiller(GlobalV.labels[k]).trim().equalsIgnoreCase(String.join(" ", targetPattern))) {
                            System.out.println("BRAND NEW");
                            newFrame.setRoleFiller(GlobalV.labels[k], String.join(" ", targetPattern));
                            notAllDuplicate = true;
                        }
                        else
                        {
                            System.out.println("DUPLICATE DETECTED");
                        }
                    }
                    if (notAllDuplicate) {
                        newAnnotations.add(newFrame.toStringAnnotation(pattern,query));
                        newFrames.add(newFrame);
                    }
                    match = true;
                }
            }
            if (!match) {
                System.out.println("NO MATCHES");
            }
        }
        frames.addAll(newFrames);

        ProcessFrameUtil.dumpFramesToFile(frames, frameFileOut, FileUtil.getFileHeader(frameFileName));
        FileUtil.dumpToFileWHeader(newAnnotations,annotationFileOut,FileUtil.getFileHeader(annotationFileName));
    }
    public static void main(String[] args) throws IOException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, FileNotFoundException, ClassNotFoundException {
        
        new ArgumentSpanExtractor().generateArgumentCandidates("./data/merged.lowercase.frame.tsv", "./data/merged.lowercase.tsv", "./data/input.txt", "./data/output.txt", 
                                                                "./data/modelCCG", Constant.SRL_CCG, "./data/frames.dump.tsv", "./data/annotations.dump.tsv");      
    }
}
