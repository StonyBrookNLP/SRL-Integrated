/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.srl;

import Util.Constant;
import Util.StdUtil;
import Util.StringUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import qa.util.FileUtil;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.io.AllCoNLL09Reader;

/**
 *
 * @author samuellouvan
 */
public class SRLEvaluate {

    public void evaluate(ArrayList<String> testFilePath, String testFilePattern, String predictFilePattern, int srlType) throws FileNotFoundException, IOException {
        System.out.println("Evaluating");
        PrintWriter gs_writer = new PrintWriter("gs.txt");
        PrintWriter srl_writer = new PrintWriter("srl.txt");
        for (int i = 0; i < testFilePath.size(); i++) {
            String[] gsTxt = FileUtil.readLinesFromFile(testFilePath.get(i));
            String[] srlTxt = FileUtil.readLinesFromFile(testFilePath.get(i).replace(testFilePattern, predictFilePattern));
            if (gsTxt.length != srlTxt.length) {
                System.out.println(testFilePath.get(i));
                System.out.println("MISMATCH DUE TO CLEARPARSER ERROR");
            } else {
                gs_writer.print(StringUtil.toString(gsTxt));
                srl_writer.print(StringUtil.toString(srlTxt));
            }
        }
        gs_writer.close();
        srl_writer.close();

        if (srlType == Constant.SRL_MATE) {
            // convert to clearparser format
            FileUtil.fromConll2009ToClearParserFormat("gs.txt", "gs_temp.txt");
            new File("gs.txt").delete();
            FileUtil.fromConll2009ToClearParserFormat("srl.txt", "srl_temp.txt");
            new File("srl.txt").delete();

            new File("gs_temp.txt").renameTo(new File("gs.txt"));
            new File("srl_temp.txt").renameTo(new File("srl.txt"));
        }

        // create runtime to execute external command
        String pythonScriptPath = "./script/evaluate.py";
        String[] cmd = new String[4];
        cmd[0] = "python";
        cmd[1] = pythonScriptPath;
        cmd[2] = "gs.txt";
        cmd[3] = "srl.txt";
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(cmd);

        // retrieve output from python script
        BufferedReader bfr = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line = "";
        while ((line = bfr.readLine()) != null) {
            // display each output line form python script
            System.out.println(line);
        }
        StdUtil.printError(pr);
    }

    public void evaluateOverall(ArrayList<String> testFilePath,String testFilePattern, String predictFilePattern, int srlType) throws FileNotFoundException {
        System.out.println("Evaluating");
        PrintWriter gs_writer = new PrintWriter("gs.txt");
        PrintWriter srl_writer = new PrintWriter("srl.txt");
        for (int i = 0; i < testFilePath.size(); i++) {
            String[] gsTxt = FileUtil.readLinesFromFile(testFilePath.get(i));
            String[] srlTxt = FileUtil.readLinesFromFile(testFilePath.get(i).replace(testFilePattern, predictFilePattern));
            if (gsTxt.length != srlTxt.length) {
                System.out.println(testFilePath.get(i));
                System.out.println("MISMATCH DUE TO PARSER ERROR");
            } else {
                gs_writer.print(StringUtil.toString(gsTxt));
                srl_writer.print(StringUtil.toString(srlTxt));
            }
        }
        gs_writer.close();
        srl_writer.close();
        AllCoNLL09Reader gsReader = new AllCoNLL09Reader(new File("gs.txt"));
        AllCoNLL09Reader srlReader = new AllCoNLL09Reader(new File("srl.txt"));

        List<Sentence> gsSentences = gsReader.readAll();
        List<Sentence> srlSentences = srlReader.readAll();
        double totalCorrect = 0;
        double totalLabelGS = 0;
        double totalLabelSRL = 0;
        for (int i = 0; i < gsSentences.size(); i++) {
            Sentence gsSentence = gsSentences.get(i);
            Sentence srlSentence = srlSentences.get(i);

            for (int j = 1; j < gsSentence.size(); j++) {
                Word currentGSWord = gsSentence.get(j);
                Word currentSRLWord = srlSentence.get(j);
                ArrayList<String> gsLabels = gsSentence.getUniqueLabel(currentGSWord);
                ArrayList<String> srlLabels = srlSentence.getUniqueLabel(currentSRLWord);
                totalLabelGS += gsLabels.size();
                totalLabelSRL += srlLabels.size();
                for (int k = 0; k < srlLabels.size(); k++) {
                    if (gsLabels.contains(srlLabels.get(k))) {
                        totalCorrect++;
                    }
                }

            }
        }

        double precision = totalCorrect/totalLabelSRL;
        double recall = totalCorrect/totalLabelGS;
        double f1 = (2 * precision * recall) / (precision + recall);
        System.out.printf("\n%s %10s %10s\n", "P", "R", "F1");
        System.out.printf("\n%.4f %10.4f %10.4f\n", precision, recall, f1);

        gsReader.close();
        srlReader.close();
    }
    
    public void evaluateOverall(String goldFile, String predictionFile)
    {
        AllCoNLL09Reader gsReader = new AllCoNLL09Reader(new File(goldFile));
        AllCoNLL09Reader srlReader = new AllCoNLL09Reader(new File(predictionFile));

        List<Sentence> gsSentences = gsReader.readAll();
        List<Sentence> srlSentences = srlReader.readAll();
        double totalCorrect = 0;
        double totalLabelGS = 0;
        double totalLabelSRL = 0;
        for (int i = 0; i < gsSentences.size(); i++) {
            Sentence gsSentence = gsSentences.get(i);
            Sentence srlSentence = srlSentences.get(i);

            for (int j = 1; j < gsSentence.size(); j++) {
                Word currentGSWord = gsSentence.get(j);
                Word currentSRLWord = srlSentence.get(j);
                ArrayList<String> gsLabels = gsSentence.getUniqueLabel(currentGSWord);
                ArrayList<String> srlLabels = srlSentence.getUniqueLabel(currentSRLWord);
               
                totalLabelGS += gsLabels.size();
                totalLabelSRL += srlLabels.size();
                for (int k = 0; k < srlLabels.size(); k++) {
                    if (gsLabels.contains(srlLabels.get(k))) {
                        totalCorrect++;
                    }
                }
            }
            double tempPrecision = totalCorrect/totalLabelSRL;
            double tempRecall = totalCorrect/totalLabelGS;
            double f1 = (2 * tempPrecision * tempRecall) / (tempPrecision + tempRecall);
            //System.out.println("After sentence "+(i+1));
            //System.out.printf("\n%s %10s %10s\n", "P", "R", "F1");
            //System.out.printf("\n%.4f %10.4f %10.4f\n", tempPrecision, tempRecall, f1);
        }

        double precision = totalCorrect/totalLabelSRL;
        double recall = totalCorrect/totalLabelGS;
        double f1 = (2 * precision * recall) / (precision + recall);
        System.out.printf("\n%s %10s %10s\n", "P", "R", "F1");
        System.out.printf("\n%.4f %10.4f %10.4f\n", precision, recall, f1);

        gsReader.close();
        srlReader.close();
    }
    public static void main(String[] args)
    {
        new SRLEvaluate().evaluateOverall("./data/gs.srl", "./data/srlPredict.srl");
        //new SRLEvaluate().evaluateOverall("./data/gs_manual.txt", "./data/srl_manual.txt");
    }
}
