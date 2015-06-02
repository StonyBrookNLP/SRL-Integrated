package qa.experiment;

import Util.ClearParserUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import qa.ClearParserProcessor;
import qa.evaluate.Evaluator;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
public class SRLPerProcessCrossValidation {

    ClearParserProcessor cp = new ClearParserProcessor();
    Evaluator eval;

    public void generateNFoldFiles(String dirName, String fileName, ArrayList<String> sentences) throws FileNotFoundException {
        for (int i = 0; i < sentences.size(); i++) {
            String nameWithoutExt = FileUtil.getFileNameWoExt(fileName);
            nameWithoutExt = nameWithoutExt.replaceAll("\\|", "_");
            PrintWriter trainWriter = new PrintWriter(dirName + "/" + nameWithoutExt + ".train.cv." + i);
            PrintWriter testWriter = new PrintWriter(dirName + "/" + nameWithoutExt + ".test.cv." + i);
            for (int j = 0; j < sentences.size(); j++) {
                if (i == j) // Test instances
                {
                    testWriter.println(sentences.get(j));
                } else // Training instances
                {
                    trainWriter.println(sentences.get(j));
                    trainWriter.println();
                }
            }
            trainWriter.close();
            testWriter.close();
        }
    }

    /*
     INPUT:
     dirName         :the directory name which contains process.clearparser 
     resultFileName : output the result to resultFileName
     */
    public void experimentNFoldCrossVal(String dirName, String fileName, String resultFileName) throws FileNotFoundException, IOException, InterruptedException {
        /*
         Generate all the fold file for each process
         E.g. evaporation.train.cv.0 evaporation.test.cv.1
         */
        HashMap<String, Integer> processes = new HashMap<String, Integer>();
        if (fileName.equals("")) {
            File folder = new File(dirName);
            File[] listOfFiles = folder.listFiles();
            System.out.println("Hai");
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].getName().contains(".clearparser")) {
                    ArrayList<String> sentences = cp.loadSentences(dirName, listOfFiles[i].getName());
                    String processName = FileUtil.getFileNameWoExt(listOfFiles[i].getName());
                    processName = processName.replaceAll("\\|", "_");
                    processes.put(processName, sentences.size());
                    generateNFoldFiles(dirName, listOfFiles[i].getName(), sentences);
                }
            }
        } else {

        }

        /* Run N-Fold cross validation for each process that has sentences more than 1 */
        PrintWriter resultWriter = new PrintWriter(new BufferedWriter(new FileWriter(resultFileName, true)));
        int cnt = 0;
        for (String processName : processes.keySet()) {
            System.out.println(cnt);
            int fold = processes.get(processName);
            for (int i = 0; i < fold; i++) {
                if (fold > 1) {
                    System.out.println(i);
                    String s = "";
                    String modelName = dirName + "/" + processName + ".model." + i;
                    String trainingFileName = dirName + "/" + processName + ".train.cv." + i;
                    String testingFileName = dirName + "/" + processName + ".test.cv." + i;
                    String predictionFileName = dirName + "/" + processName + ".predict.cv" + i;
                    //String resultFileName = dirName + "/" + processName + ".result";
                    // System.out.println(modelName);
                    // System.out.println(trainingFileName);

                    /* Train a fold */
                    ClearParserUtil.clearParserTrain(modelName, trainingFileName);
                    ClearParserUtil.clearParserPredict(modelName, testingFileName, predictionFileName);
                    String results = ClearParserUtil.clearParserEval(processName, i, testingFileName, predictionFileName);
                    resultWriter.print(results);
                    resultWriter.flush();

                } else {
                    System.out.println(processName + " ONLY ONE SENTENCE");
                }
            }
            cnt++;
        }
        //resultWriter.close();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
        SRLPerProcessCrossValidation experiment = new SRLPerProcessCrossValidation();
        experiment.experimentNFoldCrossVal("/Users/samuellouvan/NetBeansProjects/QA/data/conduction_manual_cv", "",
                                           "/Users/samuellouvan/NetBeansProjects/QA/data/conduction_manual_cv/results.tsv");
    }
}
