/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import qa.StanfordDepParserSingleton;
import qa.StanfordTokenizerSingleton;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.Sentence;
import sbu.srl.rolextract.SpockDataReader;

/**
 *
 * @author slouvan
 */
public class CCGParserUtil {

    public static String[] PREDICT_ARGS = {"-m",
        "", // model file name
        "-o", // MODEL NAME
        "srl",
        "-f",
        "",
        "-g",
        ""
    };
    public static String[] PREDICT_ARGS_PROCESS_ROLES = {"-m",
        "", // model file name
        "-o", // MODEL NAME
        "srl",
        "-f",
        "",
        "-g",
        "",
        "-n",
        "100",
        "-j",
        ""
    };

    public static String[] roleLabel = {"ARG0", "ARG1", "ARG2", "ARG3", "ARG4", "ARG5", "DIR", "LOC", "MNR", "TMP", "EXT", "REC", "PRD", "PNC", "CAU", "DIS", "ADV", "MOD", "NEG"};

    public static HashMap<String, ArrayList<String>> getArgumentCandidates(String predictionFileName) throws FileNotFoundException, IOException {
        HashMap<String, ArrayList<String>> sentenceArgumentsPair = new HashMap<String, ArrayList<String>>();

        ArrayList<String> rawArguments = new ArrayList<String>();
        String[] lines = FileUtil.readLinesFromFile(predictionFileName);
        boolean insideSent = false;
        String currentSent = "";
        Sentence currentLabeledSent = null;
        ArrayList<String> argumentSpans = new ArrayList<String>();

        int cntSent = 0;
        boolean first = true;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("SENT:")) {
                // jika pertama kali maka 
                if (first) {
                    //      put sent + pattern kosong
                    sentenceArgumentsPair.put(lines[i].split("SENT:")[1], null);
                    //      set currentSet
                    currentSent = lines[i].split("SENT:")[1].intern();
                    currentLabeledSent = new Sentence(currentSent);
                    first = false;
                } else {
                    sentenceArgumentsPair.put(currentSent, argumentSpans);
                    currentSent = lines[i].split("SENT:")[1];
                    currentLabeledSent = new Sentence(currentSent);
                    argumentSpans = new ArrayList<String>();
                    cntSent++;
                }

            } else if (!lines[i].isEmpty()) {
                for (String label : roleLabel) {
                    if (lines[i].contains(label)) {
                        String[] args = lines[i].split(label);
                        for (String arg : args) {
                            if (!argumentSpans.contains(arg.trim())) {
                                argumentSpans.add(arg.trim());
                            }
                        }
                    }
                }

            } else if (lines[i].isEmpty()) {
                insideSent = false;
            }
        }
        sentenceArgumentsPair.put(currentSent, argumentSpans);
        System.out.println("Number of sentences " + cntSent);

        return sentenceArgumentsPair;
    }

    //public static HashMap<> 
    public static HashMap<String, Sentence> getPropBankLabeledSentence(String predictionFileName) throws FileNotFoundException, IOException {
        HashMap<String, Sentence> sentTxtLabeledPair = new HashMap<String, Sentence>();
        ArrayList<String> rawArguments = new ArrayList<String>();
        String[] lines = FileUtil.readLinesFromFile(predictionFileName);
        boolean insideSent = false;
        String currentSent = "";
        Sentence currentLabeledSent = null;
        HashMap<String, ArrayList<ArgumentSpan>> roleArgMap = new HashMap<String, ArrayList<ArgumentSpan>>();
        int cntSent = 0;
        boolean first = true;
        int i;
        for (i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("SENT:")) {
                // jika pertama kali maka 
                if (first) {
                    //      put sent + pattern kosong
                    sentTxtLabeledPair.put(lines[i].split("SENT:")[1], null);
                    //      set currentSet
                    currentSent = lines[i].split("SENT:")[1];
                    currentLabeledSent = new Sentence(currentSent);
                    first = false;
                } else {
                    currentLabeledSent.setRoleArgPropBank(roleArgMap);
                    sentTxtLabeledPair.put(currentSent, currentLabeledSent);
                    currentSent = lines[i].split("SENT:")[1];
                    currentLabeledSent = new Sentence(currentSent);
                    roleArgMap = new HashMap<String, ArrayList<ArgumentSpan>>();
                    rawArguments.clear();
                    //      put sennt + pattern kosong
                    //sentArgPair.put(lines[i].split("SENT:")[1] + "#" + annotations[cntSent - 1].split("\t")[1], new ArrayList<String>());
                    cntSent++;
                }

            } else if (!lines[i].isEmpty()) {
                for (String label : roleLabel) {
                    if (lines[i].contains(label)) {
                        String predicate = lines[i].split(label)[0];
                        List<String> tokens = StanfordTokenizerSingleton.getInstance().tokenize(lines[i].split(label)[1]);
                        List<String> tokenizedRawText = StanfordTokenizerSingleton.getInstance().tokenize(currentSent.trim());
                        String[] pattern = new String[tokens.size()];
                        tokens.toArray(pattern);
                        ArrayList<Integer> matchIdxs = SpockDataReader.getIdxMatchesv2(pattern, tokenizedRawText.toArray(new String[tokenizedRawText.size()]));
                        DependencyTree tree = StanfordDepParserSingleton.getInstance().parse(currentSent.trim());
                        ArrayList<DependencyNode> arrDepNodes = new ArrayList<DependencyNode>();

                        ArgumentSpan span = new ArgumentSpan(arrDepNodes, label);
                        span.pred = predicate;

                        if (matchIdxs != null) {
                            for (int j = 1; j <= tree.lastKey(); j++) {
                                if (matchIdxs.contains(j)) {
                                    arrDepNodes.add(tree.get(j));
                                }
                            }
                            if (roleArgMap.get(label) != null) {
                                ArrayList<ArgumentSpan> spans = roleArgMap.get(label);
                                spans.add(span);
                                roleArgMap.put(label, spans);
                            } else {

                                ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
                                spans.add(span);
                                roleArgMap.put(label, spans);
                            }
                        }

                    }

                }
            } else if (lines[i].isEmpty()) {
                insideSent = false;
            }
        }
        if (lines.length > 0) {
            currentLabeledSent.setRoleArgPropBank(roleArgMap);
            sentTxtLabeledPair.put(currentSent, currentLabeledSent);
        }
        //      put sennt + pattern kosong
        //sentArgPair.put(lines[i].split("SENT:")[1] + "#" + annotations[cntSent - 1].split("\t")[1], new ArrayList<String>());
        cntSent++;
        sentTxtLabeledPair.put(currentSent, currentLabeledSent);
        System.out.println("Number of sentences " + cntSent);

        return sentTxtLabeledPair;
    }

    public static HashMap<String, ArrayList<String>> getArgumentCandidates(String fileName, String annotationFileName) throws FileNotFoundException {
        String[] lines = FileUtil.readLinesFromFile(fileName);
        HashMap<String, ArrayList<String>> sentArgPair = new HashMap<String, ArrayList<String>>();
        boolean insideSent = false;
        ArrayList<String> spans = new ArrayList<String>();
        String currentSent = "";
        int cntSent = 0;
        boolean first = true;
        String[] annotations = FileUtil.readLinesFromFile(annotationFileName, true, "process");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("SENT:")) {

                // jika pertama kali maka 
                if (first) {
                    //      put sent + pattern kosong
                    sentArgPair.put(lines[i].split("SENT:")[1] + "#" + annotations[cntSent].split("\t")[1], new ArrayList<String>());
                    //      set currentSet
                    currentSent = lines[i].split("SENT:")[1];
                    first = false;
                } else {
                    // jika bukan pertama
                    //      overwrite argument span dari currrentSent
                    ArrayList<String> finalSpans = new ArrayList<String>();
                    finalSpans.addAll(spans);
                    sentArgPair.put(currentSent + "#" + annotations[cntSent].split("\t")[1], new ArrayList<String>(finalSpans));
                    //      ubah currentSent
                    spans.clear();
                    currentSent = lines[i].split("SENT:")[1];
                    //      put sennt + pattern kosong
                    //sentArgPair.put(lines[i].split("SENT:")[1] + "#" + annotations[cntSent - 1].split("\t")[1], new ArrayList<String>());
                    cntSent++;
                }

            } else if (!lines[i].isEmpty()) {
                for (String label : roleLabel) {
                    if (lines[i].contains(label)) {
                        String[] args = lines[i].split(label);
                        for (String arg : args) {
                            if (!spans.contains(arg.trim())) {
                                spans.add(arg.trim());
                            }
                        }
                    }
                }
            } else if (lines[i].isEmpty()) {
                insideSent = false;
            }
        }
        System.out.println("Number of sentences " + cntSent);

        // put to HashMap
        sentArgPair.put(currentSent + "#" + annotations[annotations.length - 1].split("\t")[1], spans);

        return sentArgPair;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        //getArgumentCandidates("./data/output.txt", "./data/enabler_training_data.cleaned.tsv");
        //HashMap<String, Sentence> sentLabeledPair = getPropBankLabeledSentence("./data/output.txt");
        HashMap<String, ArrayList<String>> sentLabeledPair = getArgumentCandidates("./data/filtered_patternrole.tsv");
    }
}
