/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.googlesent;

import Util.GlobalV;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import qa.StanfordDepParser;
import qa.dep.DependencyTree;
import qa.util.FileUtil;

/**
 *
 * @author slouvan
 */
public class SentenceRoleProcessor {

    HashMap<String, Integer> patternIDPair = new HashMap<String, Integer>();
    String[] fileHeaders = {"process", "pattern", "query", "undergoer", "enabler", "trigger", "result", "sentence",
        "isundergoer", "isenabler", "istrigger", "isresult"};

    public void initPatternIDPair() {
        int cnt = 1;
        patternIDPair.put("<process name> is the *", cnt++);
        patternIDPair.put("<process name> occurs when *", cnt++);
        patternIDPair.put("<process name> occurs due to * of *", cnt++);
        patternIDPair.put("<process name> occurs due to *", cnt++);
        patternIDPair.put("<process name> is caused by *", cnt++);
        patternIDPair.put("<process name> is the process of *", cnt++);
        patternIDPair.put("<process name> is the process of * through", cnt++);
        patternIDPair.put("<process name> is the process of * from * to *", cnt++);
        patternIDPair.put("<process name> causes *", cnt++);
        patternIDPair.put("<process name> is the process by which * into *", cnt++);
        patternIDPair.put("* is necessary for <process name>", cnt++);
        patternIDPair.put("<process name> depends on factors such as *", cnt++);
        patternIDPair.put("<process name> helps to *", cnt++);
        patternIDPair.put("the purpose of <process name> is to *", cnt++);
    }

    public HashMap<String, String> extractRole(String sentence, String patternUsed) throws IOException {
        HashMap<String, String> roleValuesPair = new HashMap<String, String>();
        StanfordDepParser parser = new StanfordDepParser();
        try {
            DependencyTree tree = parser.parse(sentence);
            System.out.println(patternUsed + " " + patternUsed.replaceAll("\"", ""));
            if (patternIDPair.get(patternUsed.replaceAll("\"", "").trim()) == null) {
                System.out.println("Cannot find  : " + patternUsed.replaceAll("\"", "").trim() + " Length" + patternUsed.replaceAll("\"", "").trim().length());
                return null;
            } else {
                System.out.println(patternUsed.length());
                int patternID = patternIDPair.get(patternUsed.replaceAll("\"", "").trim());
                RoleExtractor extractor = new RoleExtractor();
                extractor.loadBlackList("./data/roleFillersBlackList.txt");
                extractor.loadNomBank("./data/nombank.1.0.words");

                ArrayList<RoleSpan> roleSpans = extractor.extract(patternID, tree);

                roleValuesPair = getRoleFillers(roleSpans);
                return roleValuesPair;
            }
        } catch (Exception e) {
            System.out.println("ERROR :" + sentence);
            return null;
        }
    }

    public boolean isAllRolesConsumed(HashMap<String, String> extractedRoles) {
        long nbEmpty = extractedRoles.values().stream().filter(s -> s.isEmpty()).count();
        if (nbEmpty == extractedRoles.keySet().size()) {
            return true;
        } else {
            System.out.println(nbEmpty);
            System.out.println("FALSE");
            return false;
        }
    }

    public void processSentences(String fileName, String outputFileName) throws FileNotFoundException, IOException {
        String[] lines = FileUtil.readLinesFromFile(fileName);
        PrintWriter writer = new PrintWriter(outputFileName);
        writer.println(String.join("\t", fileHeaders));
        for (String line : lines) {

            String[] columns = line.split("\t");
            if (!columns[0].toLowerCase().contains("process")) {
                String processName = columns[0];
                String pattern = columns[1];
                String sentence = columns[7];

                String patternTemplate = pattern.replace(processName, "<process name>");
                System.out.println("Sentence : " + sentence);
                    HashMap<String, String> extractedRoles = extractRole(sentence, patternTemplate);
                if (extractedRoles != null && extractedRoles.size() > 0) {
                    while (!isAllRolesConsumed(extractedRoles)) {
                        StringBuilder roleFillers = new StringBuilder();
                        for (String roleLabel : GlobalV.labels) {
                            if (extractedRoles.get(roleLabel) == null) {
                                roleFillers.append("\t");
                            } else {
                                String[] roleArr = extractedRoles.get(roleLabel).split("\\|");
                                if (roleArr.length > 1) {
                                    //System.out.println("Multiple val role fillers");
                                    roleFillers.append(roleArr[0].trim()).append("\t");
                                    String residue = "";
                                    for (int i = 1; i < roleArr.length; i++) {
                                        residue = residue.concat(roleArr[i].trim()).concat("|");
                                    }
                                    residue = residue.substring(0, residue.length() - 1);
                                    extractedRoles.put(roleLabel, residue);
                                } else {
                                    roleFillers.append(extractedRoles.get(roleLabel).trim()).append("\t");
                                    extractedRoles.put(roleLabel, "");
                                }
                            }
                        }
                        writer.println(processName + "\t" + columns[1] + "\t" + columns[2] + "\t" + roleFillers.toString() + sentence);
                    }
                    
                } else {
                    StringBuilder roleFillers = new StringBuilder();
                    for (String roleLabel : GlobalV.labels) {
                        roleFillers.append("\t");
                    }
                    writer.println(processName + "\t" + columns[1] + "\t" + columns[2] + "\t" + roleFillers.toString() + sentence);
                }
                /*if (extractedRoles != null && extractedRoles.size() >= 0) {
                 StringBuilder roleFillers = new StringBuilder();
                 for (String roleLabel : GlobalV.labels) {
                 System.out.println("Role Label :" + roleLabel + " Role Fillers :" + extractedRoles.get(roleLabel));
                 if (extractedRoles.get(roleLabel) == null) {
                 roleFillers.append("\t");
                 } else {
                 roleFillers.append(extractedRoles.get(roleLabel)).append("\t");
                 }
                 }
                 writer.println(processName + "\t" + columns[1] + "\t" + columns[2] + "\t" + roleFillers.toString() + sentence);
                 } else {
                 StringBuilder roleFillers = new StringBuilder();
                 for (String roleLabel : GlobalV.labels) {
                 roleFillers.append("\t");
                 }
                 writer.println(processName + "\t" + columns[1] + "\t" + columns[2] + "\t" + roleFillers.toString() + sentence);
                 }*/

            }
        }

        writer.close();
    }

    public HashMap<String, String> getRoleFillers(ArrayList<RoleSpan> roleSpan) {
        StringBuffer strBuff = new StringBuffer();
        HashMap<String, String> results = new HashMap<String, String>();

        for (int i = 0; i < GlobalV.labels.length; i++) {
            final String currLabel = GlobalV.labels[i];
            List<RoleSpan> roleSpans = roleSpan.stream().filter(el -> el.getRoleLabel().equalsIgnoreCase(currLabel)).collect(Collectors.toList());
            for (int j = 0; j < roleSpans.size(); j++) {
                if (roleSpans.get(j).getRoleLabel().equalsIgnoreCase(GlobalV.labels[i])) {
                    if (j > 0 && roleSpans.get(j - 1).getNodeSpan().getId() == roleSpans.get(j).getNodeSpan().getId() - 1) {
                        strBuff.append(roleSpans.get(j).getNodeSpan().getForm()).append(" ");
                    } else if (j == 0) {
                        strBuff.append(roleSpans.get(j).getNodeSpan().getForm()).append(" ");
                    } else {
                        strBuff.append("|").append(roleSpans.get(j).getNodeSpan().getForm()).append(" ");
                    }
                }
            }
            if (strBuff.length() > 0) {
                results.put(GlobalV.labels[i], strBuff.toString().trim());
                strBuff.setLength(0);
            }

        }

        return results;
    }

    public static void main(String[] args) throws IOException {
        SentenceRoleProcessor processor = new SentenceRoleProcessor();
        processor.initPatternIDPair();
        processor.processSentences("./data/filtered.cleaned.tsv", "./data/filtered_patternrole.tsv");

    }
}
