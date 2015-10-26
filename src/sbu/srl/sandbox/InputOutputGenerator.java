/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.sandbox;

import clear.util.FileUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;

/**
 *
 * @author slouvan
 */
public class InputOutputGenerator {

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException {
        // Load verb
        String[] verbs1 = FileUtil.readLinesFromFile("./data/4th-grade-conversion-verbs.txt.linux");
        // Load verb conversion
        String[] verbs2 = FileUtil.readLinesFromFile("./data/4th-grade-verbs.txt.linux");

        // Put it to hashtable
        ArrayList<String> verbsStr = new ArrayList<String>();
        List<String> temp = Arrays.asList(verbs1);
        verbsStr.addAll(temp);
        temp = Arrays.asList(verbs2);
        verbsStr.addAll(temp);

        // load process frame
        ProcessFrameProcessor proc = new ProcessFrameProcessor("./data/process_frame_24_july.tsv");
        proc.loadProcessData();

        // for each verb do
        for (int i = 0; i < verbsStr.size(); i++) {
            //    if verb is a process then extract the undergoer and result
            //System.out.println("CURRENT : "+verbsStr.get(i));
            if (proc.getProcessFrameByNormalizedName(verbsStr.get(i)).size() > 0) {
                //System.out.println("EXIST " + verbsStr.get(i) + " size " + proc.getProcessFrameByNormalizedName(verbsStr.get(i)).size());
                ArrayList<ProcessFrame> res = proc.getProcessFrameByNormalizedName(verbsStr.get(i));
                StringBuilder inputStr = new StringBuilder();
                StringBuilder outputStr = new StringBuilder();
                HashMap<String, String> undergoerHash = new HashMap<String, String>();
                HashMap<String, String> resultHash = new HashMap<String, String>();

                for (int jj = 0; jj < res.size(); jj++) {
                    String[] undergoers = res.get(jj).getUnderGoer().split("\\|");
                    String[] results = res.get(jj).getResult().split("\\|");
                    for (String undergoer : undergoers) {

                        undergoerHash.put(undergoer, undergoer);
                    }
                    for (String result : results) {
                        resultHash.put(result, result);
                    }
                }
                for (String input : undergoerHash.keySet()) {
                    if (!input.isEmpty()) {
                        inputStr.append(input).append(" |");
                    }
                }
                for (String output : resultHash.keySet()) {
                    if (!output.isEmpty()) {
                        outputStr.append(output).append(" |");
                    }
                }
                String input = inputStr.toString();
                String output = outputStr.toString();
                if (!input.isEmpty() && input.charAt(input.length() - 1) == '|') {
                    input = input.substring(0, input.length() - 1);
                }
                if (!output.isEmpty() && output.charAt(output.length() - 1) == '|') {
                    output = output.substring(0, output.length() - 1);

                }
                System.out.println(verbsStr.get(i) + "\t" + input + "\t" + output);
            } else {
                //    else if the verb is a trigger/action then extract the undergoer and result
                String triggerStr = verbsStr.get(i);
                //System.out.println("TRIGGER STR : "+verbsStr.get(i));
                List<ProcessFrame> res = proc.getProcArr().stream().filter(p -> p.getTrigger().contains(triggerStr)).collect(Collectors.toList());
                //System.out.println("EXIST AS TRIGGER");
                StringBuilder inputStr = new StringBuilder();
                StringBuilder outputStr = new StringBuilder();
                HashMap<String, String> undergoerHash = new HashMap<String, String>();
                HashMap<String, String> resultHash = new HashMap<String, String>();

                for (int jj = 0; jj < res.size(); jj++) {
                    String[] undergoers = res.get(jj).getUnderGoer().split("\\|");
                    String[] results = res.get(jj).getResult().split("\\|");
                    for (String undergoer : undergoers) {
                        undergoerHash.put(undergoer, undergoer);
                    }
                    for (String result : results) {
                        resultHash.put(result, result);
                    }
                }
                for (String input : undergoerHash.keySet()) {
                    if (!input.isEmpty()) {
                        inputStr.append(input).append(" |");
                    }
                }
                for (String output : resultHash.keySet()) {
                    if (!output.isEmpty()) {
                        outputStr.append(output).append(" |");
                    }
                }
                String input = inputStr.toString();
                String output = outputStr.toString();
                if (!input.isEmpty() && input.charAt(input.length() - 1) == '|') {
                    input = input.substring(0, input.length() - 1);
                }
                if (!output.isEmpty() && output.charAt(output.length() - 1) == '|') {
                    output = output.substring(0, output.length() - 1);

                }
                System.out.println(verbsStr.get(i) + "\t" + input + "\t" + output);
            }

        }
    }
}
