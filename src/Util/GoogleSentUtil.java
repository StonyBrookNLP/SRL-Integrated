/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

/**
 *
 * @author slouvan
 */
public class GoogleSentUtil {
    // Input file format :
    // PROCESS PATTERN QUERY UNDERGOER ENABLER TRIGGER RESULT UNDERSPECIFIED SENTENCE

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

    public static String cleanSentence(String sentence, boolean defSent) {
        String[] sentences = sentence.split("\\.");
        if (sentences.length > 2) {
            return ""; // BAD SENTENCE
        } else if (sentences.length > 1) {
            for (String month : months) {
                if (sentences[0].startsWith(month)) {
                    if (!startsWithQuestionWords(sentences[1])) {
                        return sentences[1].replace("\"", "").trim();
                    } else {
                        return "";
                    }
                }
            }

            if (!startsWithQuestionWords(sentences[0])) {
                if (defSent) {
                    if (sentences[0].contains("is")) {
                        return sentences[0].replace("\"", "").trim();
                    }
                    else
                        return "";
                } else {
                    return sentences[0].replace("\"", "").trim();
                }
            } else if (!startsWithQuestionWords(sentences[1])) {
                return sentences[1].replace("\"", "").trim();
            } else {
                return "";
            }

        } else {
            if (!startsWithQuestionWords(sentence)) {
                return sentence.replace("\"", "").trim();
            } else {
                return "";
            }
        }
    }
}
