/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import qa.util.FileUtil;

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

    public static String[] roleLabel = {"ARG0", "ARG1", "ARG2", "ARG3", "ARG4", "ARG5", "DIR", "LOC", "MNR", "TMP", "EXT", "REC", "PRD", "PNC", "CAU", "DIS", "ADV", "MOD", "NEG"};

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

    public static void main(String[] args) throws FileNotFoundException {
        getArgumentCandidates("./data/output.txt", "./data/enabler_training_data.cleaned.tsv");
    }
}
