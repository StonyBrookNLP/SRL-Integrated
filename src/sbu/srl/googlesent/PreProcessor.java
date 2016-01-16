/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.googlesent;

import Util.GlobalV;
import Util.GoogleSentUtil;
import clear.util.FileUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import qa.ProcessFrameProcessor;
import qa.StanfordDepParserSingleton;

/**
 *
 * @author slouvan
 */
public class PreProcessor {

    // INPUT : Google extracted sentence
    // Field : process, pattern, query, undergoer, enabler, trigger, result, underspecified, sentence, isUndergoer, isEnabler, isTrigger, isResult, isUnderspecified
    // OUT : 
    // Google extracted sentence with processed sentence
    // if dependency parser return error, then it will be skipped
    public void preprocessGoogleData(String inFileName, String outFileName, int sentIdx, boolean defSent) throws FileNotFoundException {
        String[] lines = FileUtil.readLinesFromFile(inFileName);
        StanfordDepParserSingleton parser = StanfordDepParserSingleton.getInstance();
        ArrayList<String> cleanedLines = new ArrayList<String>();
        int cnt  = 1;
        int totalError = 0;
        System.out.println("Original sentence number"+cleanedLines.size());
        for (String line : lines) {
            String field[] = line.split("\t");
            try {
                if (!field[0].equalsIgnoreCase("process")) {
                    String cleanedSentence = GoogleSentUtil.cleanSentence(field[sentIdx].toLowerCase(), defSent);
                    if (cleanedSentence.length() > 0) {
                        parser.parse(cleanedSentence);
                        field[sentIdx] = cleanedSentence;
                        cleanedLines.add(String.join("\t", field));
                    }
                } else {
                    cleanedLines.add(String.join("\t", field));
                }
            } catch (Exception e) {
                System.out.println("PARSING ERROR " + field[sentIdx]);
                totalError++;
            }
            
        }
        System.out.println("Total parse error"+totalError);
        System.out.println("Total sentence after filtering : "+cleanedLines.size());
        FileUtil.dumpToFile(cleanedLines, outFileName);
    }

    // INPUT : Cleaned google extracted sentences
    public void generateProcessFrameFile(String googExtractedFile, String frameFile) throws FileNotFoundException {
        String[] lines = FileUtil.readLinesFromFile(googExtractedFile);
        ArrayList<String> frameLines = new ArrayList<String>();
        for (int i = 0; i < lines.length; i++) {
            StringBuffer strBuff = new StringBuffer();
            String fields[] = lines[i].split("\t");
            for (int j = 0; j < fields.length; j++) {
                if (j != 1 && j != 2) {
                    strBuff.append(fields[j]).append("\t");
                }
            }
            frameLines.add(strBuff.toString().trim());
        }
        FileUtil.dumpToFile(frameLines, frameFile);
    }

    // Input : google extracted file
    //         frame file
    //         roleOfInterest (e.g. Undergoer, Enabler, Trigger, Result, Underspecified etc) separate with "-"
    public void generateCoNLL09File(String googleExtractedFile, String frameFile, String conllFileName, String roleOfInterests) throws FileNotFoundException, IOException, ClassNotFoundException {
        String googSents[] = FileUtil.readLinesFromFile(googleExtractedFile);
        String frameLines[] = FileUtil.readLinesFromFile(frameFile);
        ProcessFrameProcessor proc = new ProcessFrameProcessor(frameFile);
        proc.loadProcessData();
        proc.toConLL2009FormatCleanV(conllFileName, frameLines, roleOfInterests);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {

        PreProcessor procObj = new PreProcessor();
        //procObj.preprocessGoogleData("./data/extracted_sentences.tsv", "./data/extracted_sentences.cleaned.tsv", 8, true);
        //procObj.generateProcessFrameFile("./data/CandidateSpanGeneration.tsv", "./data/CandidateSpanGeneration.frame.tsv");
        //procObj.generateCoNLL09File("./data/trigger_training_data.cleaned.tsv", "./data/trigger_training_data.frame.tsv", "./data/trigger_training_data.conll09", "trigger");
        procObj.preprocessGoogleData("./data/filtered_sentences.tsv", "./data/filtered.cleaned.tsv", 7, true);
    }
}
