/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.brat;

import clear.util.FileUtil;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 *
 * @author slouvan
 */
public class BRATFileGenerator {

    static String[] months = {"jan, feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec"};
    static String[] questionWords = {"what", "who", "where", "how", "when"};

    public static boolean startsWithQuestionWords(String str) {
        for (String word : questionWords) {
            if (str.startsWith(word)) {
                return true;
            }
        }

        return false;
    }

    public static String cleanSentence(String sentence) {
        String[] sentences = sentence.split("\\.");
        if (sentences.length > 2) {
            return ""; // BAD SENTENCE
        } else if (sentences.length > 1) {
            for (String month : months) {
                if (sentences[0].startsWith(month)) {
                    if (!startsWithQuestionWords(sentences[1])) {
                        return sentences[1].trim();
                    } else {
                        return "";
                    }
                }
            }

            if (!startsWithQuestionWords(sentences[0]) && sentences[0].contains("is")) {
                return sentences[0].trim();
            } else if (!startsWithQuestionWords(sentences[1])) {
                return sentences[1].trim();
            } else {
                return "";
            }
        } else {
            if (!startsWithQuestionWords(sentence)) {
                return sentence.trim();
            } else {
                return "";
            }
        }
    }

    // Input is TSV file output is also TSV file with the cleaned version
    // of the sentence
    public static void main(String[] args) throws FileNotFoundException {
        HashMap<String, Integer> processCountPair = new HashMap<String, Integer>();

        String[] lines = FileUtil.readLinesFromFile("./data/google_processes_small.tsv");
        for (int i = 0; i < lines.length; i++) {
            String fields[] = lines[i].split("\t");
            if (!fields[0].equalsIgnoreCase("process")) {
                String processName = fields[0].toLowerCase();
                String rawSentence = fields[3].toLowerCase();
                String cleanSentence = cleanSentence(rawSentence);
                System.out.println(cleanSentence);
                if (cleanSentence.length() > 0) {
                    if (processCountPair.get(processName) != null) {
                        processCountPair.put(processName, processCountPair.get(processName) + 1);
                        FileUtil.dumpToFile(cleanSentence, new PrintWriter(processName + "-" + processCountPair.get(processName) + ".txt"));
                    } else {
                        processCountPair.put(processName, 1);
                        FileUtil.dumpToFile(cleanSentence, new PrintWriter(processName + "-1.txt"));
                    }
                }
            }
        }
    }
}
