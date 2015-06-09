/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import Util.StdUtil;
import edu.stanford.nlp.io.EncodingPrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
public class ConLL09Generator {

    private ProcessFrameProcessor proc;
    private String conll06FileName;

    static final String UNDERGOER = "A0";
    static final String ENABLER = "A1";
    static final String RESULT = "A2";

    static final int CONLL09_NB_FIELD = 20;
    static final int CLEARPARSER_NB_FIELD = 9;
    static final int CONLL09_ID = 0;
    static final int CONLL09_FORM = 1;
    static final int CONLL09_LEMMA = 2;
    static final int CONLL09_PLEMMA = 3;
    static final int CONLL09_POS = 4;
    static final int CONLL09_PPOS = 5;
    static final int CONLL09_FEAT = 6;
    static final int CONLL09_PFEAT = 7;
    static final int CONLL09_HEAD = 8;
    static final int CONLL09_PHEAD = 9;
    static final int CONLL09_DEPREL = 10;
    static final int CONLL09_PDEPREL = 11;
    static final int CONLL09_FILLPRED = 12;
    static final int CONLL09_PRED = 13;
    static final int CONLL09_APRED1 = 14;
    static final int CONLL09_APRED2 = 15;
    static final int CONLL09_APRED3 = 16;
    static final int CONLL09_APRED4 = 17;
    static final int CONLL09_APRED5 = 18;
    static final int CONLL09_APRED6 = 19;

    static final int CONLL06_ID = 0;
    static final int CONLL06_FORM = 1;
    static final int CONLL06_LEMMA = 2;
    static final int CONLL06_CPOSTAG = 3;
    static final int CONLL06_POSTAG = 4;
    static final int CONLL06_FEATS = 5;
    static final int CONLL06_HEAD = 6;
    static final int CONLL06_DEPREL = 7;
    static final int CONLL06_PHEAD = 8;
    static final int CONLL06_PDEPREL = 9;

    static final int CLEARPARSER_ID = 0;
    static final int CLEARPARSER_FORM = 1;
    static final int CLEARPARSER_LEMMA = 2;
    static final int CLEARPARSER_POSTAG = 3;
    static final int CLEARPARSER_PPOSTAG = 4;
    static final int CLEARPARSER_HEAD = 5;
    static final int CLEARPARSER_DEPREL = 6;
    static final int CLEARPARSER_ROLESET = 7;
    static final int CLEARPARSER_SHEADS = 8;

    StanfordLemmatizer slem = new StanfordLemmatizer();

    public ConLL09Generator(String processFrameFileName, String conll06FileName) throws FileNotFoundException, IOException, ClassNotFoundException {
        proc = new ProcessFrameProcessor(processFrameFileName);
        this.conll06FileName = conll06FileName;
        proc.loadProcessData();
    }

    public String getWordSense(int wordID) {
        return "";
    }

    public String getRole() {
        return "";
    }

    public String getConLL09Format(String[] conll06Line) {
        String[] conll09line = new String[CONLL09_NB_FIELD];

        conll09line[CONLL09_ID] = conll06Line[CONLL06_ID];
        conll09line[CONLL09_FORM] = conll06Line[CONLL06_FORM];
        conll09line[CONLL09_LEMMA] = slem.lemmatize(conll06Line[CONLL06_FORM]).get(0);
        conll09line[CONLL09_PLEMMA] = conll09line[CONLL09_LEMMA];
        conll09line[CONLL09_POS] = conll06Line[CONLL06_CPOSTAG];
        conll09line[CONLL09_PPOS] = conll06Line[CONLL06_CPOSTAG];
        conll09line[CONLL09_FEAT] = "_";
        conll09line[CONLL09_PFEAT] = "_";
        conll09line[CONLL09_HEAD] = conll06Line[CONLL06_HEAD];
        conll09line[CONLL09_PHEAD] = conll06Line[CONLL06_HEAD];
        conll09line[CONLL09_DEPREL] = conll06Line[CONLL06_DEPREL];
        conll09line[CONLL09_PDEPREL] = conll06Line[CONLL06_DEPREL];
        conll09line[CONLL09_FILLPRED] = "_"; // get from annotated data
        conll09line[CONLL09_PRED] = "_";  // WORD SENSE 
        conll09line[CONLL09_APRED1] = "_"; // Undergoer
        conll09line[CONLL09_APRED2] = "_"; // Result
        conll09line[CONLL09_APRED3] = "_"; // Enabler
        conll09line[CONLL09_APRED4] = "_";
        conll09line[CONLL09_APRED5] = "_";
        conll09line[CONLL09_APRED6] = "_";

        StringBuilder builder = new StringBuilder();
        for (String s : conll09line) {
            builder.append(s + "\t");
        }
        return builder.toString().trim();
    }

    public String arrayOfStringToString(String[] arr) {
        StringBuilder builder = new StringBuilder();
        for (String s : arr) {
            builder.append(s + "\t");
        }
        return builder.toString().trim();
    }

    public ArrayList<String> processSentence(ArrayList<String> tokenProperties, int sentenceCnt) {
        // get trigger idx
        System.out.println("");
        ArrayList<Integer> triggerIdx = proc.getTriggerTokenIdx(sentenceCnt);

        if (triggerIdx.size() > 0) {
            // mark Y for FILLPRED in the tokeProperties for each idx
            for (int i = 0; i < triggerIdx.size(); i++) {
                int triggerTokenID = triggerIdx.get(i) - 1;
                String[] fields = tokenProperties.get(triggerTokenID).split("\t"); // Trigger idx starts from 1 but array List from 
                fields[CONLL09_FILLPRED] = "Y";
                fields[CONLL09_PRED] = fields[CONLL09_LEMMA] + ".01"; // TODO : Might to run WSD on this in the future
                tokenProperties.set(triggerTokenID, arrayOfStringToString(fields));
            }
        }
        // Get the undergoer
        // Set the undergoer in the field
        ArrayList<Integer> undergoerIdx = proc.getUndergoerTokenIdx(sentenceCnt);
        if (undergoerIdx.size() > 0) {
            for (int i = 0; i < undergoerIdx.size(); i++) {
                int undergoerTokenID = undergoerIdx.get(i) - 1;
                String[] fields = tokenProperties.get(undergoerTokenID).split("\t");
                for (int j = CONLL09_APRED1; j < CONLL09_APRED1 + triggerIdx.size() && j <= CONLL09_APRED1 + 5; j++) {
                    fields[j] = UNDERGOER;
                }
                tokenProperties.set(undergoerTokenID, arrayOfStringToString(fields));
            }
        }

        // Get the enabler
        ArrayList<Integer> enablerIdx = proc.getEnablerTokenIdx(sentenceCnt);
        if (enablerIdx.size() > 0) {
            for (int i = 0; i < enablerIdx.size(); i++) {
                int enablerTokenID = enablerIdx.get(i) - 1;
                String[] fields = tokenProperties.get(enablerTokenID).split("\t");
                for (int j = CONLL09_APRED1; j < CONLL09_APRED1 + triggerIdx.size() && j <= CONLL09_APRED1 + 5; j++) {
                    fields[j] = ENABLER;
                }
                tokenProperties.set(enablerTokenID, arrayOfStringToString(fields));
            }
        }

        ArrayList<Integer> resultIdx = proc.getResultTokenIdx(sentenceCnt);
        if (resultIdx.size() > 0) {
            for (int i = 0; i < resultIdx.size(); i++) {
                int resultTokenID = resultIdx.get(i) - 1;
                String[] fields = tokenProperties.get(resultTokenID).split("\t");
                for (int j = CONLL09_APRED1; j < CONLL09_APRED1 + triggerIdx.size() && j < CONLL09_APRED1 + 5; j++) {
                    fields[j] = RESULT;
                }
                tokenProperties.set(resultTokenID, arrayOfStringToString(fields));
            }
        }

        return tokenProperties;
    }

    public ArrayList<String> getClearParserSRLFormat(ArrayList<String> conll09Tokens, int sentenceCnt) {
        String[][] clearParserField = new String[conll09Tokens.size()][];
        for (int i = 0; i < conll09Tokens.size(); i++) {
            clearParserField[i] = new String[CLEARPARSER_NB_FIELD];
        }

        ArrayList<Integer> triggerIdx = proc.getTriggerTokenIdx(sentenceCnt);
        for (int i = 0; i < conll09Tokens.size(); i++) {
            String[] conl099Field = conll09Tokens.get(i).split("\t");
            clearParserField[i][CLEARPARSER_ID] = conl099Field[CONLL09_ID];
            clearParserField[i][CLEARPARSER_FORM] = conl099Field[CONLL09_FORM];
            clearParserField[i][CLEARPARSER_LEMMA] = conl099Field[CONLL09_LEMMA];
            clearParserField[i][CLEARPARSER_POSTAG] = conl099Field[CONLL09_POS];
            clearParserField[i][CLEARPARSER_PPOSTAG] = "_";
            clearParserField[i][CLEARPARSER_HEAD] = conl099Field[CONLL09_HEAD];
            clearParserField[i][CLEARPARSER_DEPREL] = conl099Field[CONLL09_DEPREL];
            if (conl099Field[CONLL09_FILLPRED].equalsIgnoreCase("Y")) {
                clearParserField[i][CLEARPARSER_ROLESET] = conl099Field[CONLL09_PRED];
            } else {
                clearParserField[i][CLEARPARSER_ROLESET] = "_";
            }
            clearParserField[i][CLEARPARSER_SHEADS] = "";
            String[] arguments = {"A0", "A1", "A2"};
            boolean first = true;
            for (int j = 0; j < arguments.length; j++) {
                if (conll09Tokens.get(i).indexOf(arguments[j]) != -1) {
                    for (int k = 0; k < triggerIdx.size(); k++) {
                        if (first) {
                            clearParserField[i][CLEARPARSER_SHEADS] += triggerIdx.get(k) + ":" + arguments[j];
                            first = false;
                        } else {
                            clearParserField[i][CLEARPARSER_SHEADS] += ";" + triggerIdx.get(k) + ":" + arguments[j];
                        }
                    }
                }
            }
            if (first) {
                clearParserField[i][CLEARPARSER_SHEADS] = "_";
            }
        }
        ArrayList<String> clearParserTokenFields = new ArrayList<String>();
        for (int i = 0; i < conll09Tokens.size(); i++) {
            String row = "";
            for (int j = 0; j < clearParserField[i].length; j++) {
                row += clearParserField[i][j] + "\t";
            }
            row = row.trim();
            clearParserTokenFields.add(row);
        }

        return clearParserTokenFields;
    }

    public String printArr(ArrayList<String> arr) {

        StringBuilder strB = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            //System.out.println(arr.get(i));
            strB.append(arr.get(i) + "\n");

        }
        return strB.toString();
    }

    public String getString(ArrayList<String> arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            sb.append(arr.get(i) + "\n");
        }
        return sb.toString();
    }

    public void reformatToCONLL09(String clearParserFileName, String conll09FileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(this.conll06FileName));
        int sentenceCnt = 0;
        PrintWriter writer = new PrintWriter(clearParserFileName);
        PrintWriter conll09Writer = new PrintWriter(conll09FileName);
        ArrayList<String> tokenProperties = new ArrayList<String>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                System.out.println(sentenceCnt);
                System.out.println(tokenProperties);
                processSentence(tokenProperties, sentenceCnt);
                conll09Writer.println(getString(tokenProperties));
                ArrayList<String> clearParserTokenFields = getClearParserSRLFormat(tokenProperties, sentenceCnt);
                writer.println(printArr(clearParserTokenFields));
                //System.out.println("");
                tokenProperties.clear();
                sentenceCnt++;
            } else {
                tokenProperties.add(getConLL09Format(line.split("\t")));
            }
        }
        conll09Writer.close();
        writer.close();
        scanner.close();
    }

    public void generateSentenceFile(String processFrameFileName, String sentencefileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(processFrameFileName));
        PrintWriter writer = new PrintWriter(sentencefileName);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            System.out.println("SENTENCE :" + line);
            String[] data = line.split("\t");
            String sentence = data[6].replace(".", " ");
            sentence = sentence.replaceAll("\"", "");
            sentence = sentence.trim();
            System.out.println(sentence);
            if (data[6].charAt(sentence.length() - 1) != '.') {
                writer.println(sentence + ".");
            } else {
                writer.println(sentence);
            }

        }
        writer.close();
        scanner.close();
    }

    public static void doStanfordPipeline(String sentenceFileName, String treeFileName, String conll06FileName) throws IOException, InterruptedException {
        String s = "";
        Process p = Runtime.getRuntime().exec("./script/create_conll06.sh " + sentenceFileName + " " + treeFileName + " " + conll06FileName);

        //BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        //BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        StdUtil.printError(p);
        p.waitFor();
        // read the output from the command
        //System.out.println("Here is the standard output of the command:\n");
        //while ((s = stdInput.readLine()) != null) {
        //    System.out.println(s);
        //}

        // read any errors from the attempted command
        //System.out.println("Here is the standard error of the command (if any):\n");
        //while ((s = stdError.readLine()) != null) {
        //    System.out.println(s);
        //}
    }

    public void generateSentenceFilesFromDir(String dirName) throws FileNotFoundException {
        File folder = new File(dirName);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(".tsv")) {
                System.out.println("File " + listOfFiles[i].getName());
                generateSentenceFile(dirName + "/" + listOfFiles[i].getName(), dirName + "/" + FileUtil.getFileNameWoExt(listOfFiles[i].getName()) + ".sent");
            }
        }
    }

    public void generateCONLLFileFromDir(String dirName) throws FileNotFoundException, IOException, InterruptedException {
        File folder = new File(dirName);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(".sent")) {
                System.out.println("File " + listOfFiles[i].getName());
                String nameWithoutExt = FileUtil.getFileNameWoExt(listOfFiles[i].getName());
                doStanfordPipeline(dirName + "/" + listOfFiles[i].getName(), dirName + "/" + nameWithoutExt + ".tree", dirName + "/" + nameWithoutExt + ".conll06");
            }
        }
    }

    public static void generateCONLLFileFromFile(String fileName) throws IOException, InterruptedException {
        File file = new File(fileName);

        System.out.println("File " + fileName);
        String nameWithoutExt = FileUtil.getFileNameWoExt(file.getName());
        doStanfordPipeline(fileName, nameWithoutExt + ".tree", nameWithoutExt + ".conll06");

    }

    public void generateClearParserFilesFromDir(String dirName) throws FileNotFoundException, IOException, ClassNotFoundException {
        File folder = new File(dirName);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].getName().contains(".tsv")) {
                proc = new ProcessFrameProcessor(dirName + "/" + listOfFiles[i].getName());
                proc.loadProcessData();
                String nameWithoutExt = FileUtil.getFileNameWoExt(listOfFiles[i].getName());
                this.conll06FileName = dirName + "/" + nameWithoutExt + ".conll06";
                reformatToCONLL09(dirName + "/" + nameWithoutExt + ".clearparser", dirName + "/" + nameWithoutExt + ".conll09");
            }
        }
    }

    public void generateClearParserFileFromFile(String fileName) throws FileNotFoundException, IOException, ClassNotFoundException {
        File folder = new File(fileName);

        proc = new ProcessFrameProcessor(fileName);
        proc.loadProcessData();
        String nameWithoutExt = FileUtil.getFileNameWoExt(fileName);
        this.conll06FileName = nameWithoutExt + ".conll06";
        reformatToCONLL09(nameWithoutExt + ".clearparser", nameWithoutExt + ".conll09");

    }

    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException, ClassNotFoundException {
        //Scanner scanner = new Scanner(new File("conll06.txt"));
        ConLL09Generator gen = new ConLL09Generator("./data/all_question_frame.txt", "./data/processes_may/riding.conll06");
        //gen.generateSentenceFile("./data/processes/write.tsv", "./data/processes/write.sent");
        //gen.doStanfordPipeline("./data/testsp/test_processes.sent", "./data/testsp/test_processes.tree", "./data/testsp/test_processes.conll06");
        //gen.generateCONLLFileFromDir("./data/evaporation_ds_234_larger_trigger");
        //gen.reformatToCONLL09("out_question_frame_conll09.txt");
        //gen.generateSentenceFilesFromDir("./data/processes_may");
        //gen.generateClearParserFilesFromDir("./data/evaporation_ds_234_larger_trigger");
        //gen.generateClearParserFileFromFile("/Users/samuellouvan/NetBeansProjects/QA/data/processes_may/riding.tsv");
        //gen.generateSentenceFile("/Users/samuellouvan/NetBeansProjects/QA/data/combined_all/all_process_may_18.tsv", "./data/combined_all/all_process_may_18.sent");
        //gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/evaporation_ds_234_larger_trigger");

       /* gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/absorb");
        gen.generateCONLLFileFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/absorb");
        gen.generateClearParserFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/absorb");

        gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/blinking");
        gen.generateCONLLFileFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/blinking");
        gen.generateClearParserFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/blinking");

        gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/breathe");
        gen.generateCONLLFileFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/breathe");
        gen.generateClearParserFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/breathe");

        gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/chemosynthesis");
        gen.generateCONLLFileFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/chemosynthesis");
        gen.generateClearParserFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/chemosynthesis");*/

        /*gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/friction");
        gen.generateCONLLFileFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/friction");
        gen.generateClearParserFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/friction");*/

        /*gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/grow");
        gen.generateCONLLFileFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/grow");
        gen.generateClearParserFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/grow");*/

        gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/hibernate");
        gen.generateCONLLFileFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/hibernate");
        gen.generateClearParserFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/hibernate");

        gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/meiosis");
        gen.generateCONLLFileFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/meiosis");
        gen.generateClearParserFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/meiosis");

        gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/refract");
        gen.generateCONLLFileFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/refract");
        gen.generateClearParserFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/refract");

        gen.generateSentenceFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/sublimation");
        gen.generateCONLLFileFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/sublimation");
        gen.generateClearParserFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/sublimation");
    }
}
