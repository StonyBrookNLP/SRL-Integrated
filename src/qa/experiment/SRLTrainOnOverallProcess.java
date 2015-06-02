/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import Util.ClearParserUtil;
import Util.StdUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */

/*
 EXPERIMENT SCENARIO :
 collect all the process name and number of fold

 for each process in processes
 if totalFold > 1
 for each fold
 create training data for this fold 
 train on concatenated cv
 test on the current fold
 output to result
 */
public class SRLTrainOnOverallProcess {

    class FoldInstance {

        String name;
        int fold;
        String sentences;

        public FoldInstance(String name, int fold, String sentences) {
            this.name = name;
            this.fold = fold;
            this.sentences = sentences;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getFold() {
            return fold;
        }

        public void setFold(int fold) {
            this.fold = fold;
        }

        public String getSentences() {
            return sentences;
        }

        public void setSentences(String sentences) {
            this.sentences = sentences;
        }

    }

    //create training data for this fold 
    public void createTrainingData(String dirName, String processName, int fold, ArrayList<FoldInstance> instances) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(dirName + "/" + processName + ".jointtrain.cv." + fold);
        for (int i = 0; i < instances.size(); i++) {
            FoldInstance inst = instances.get(i);
            if (inst.getName().equalsIgnoreCase(processName)) {
                if (inst.getFold() == fold) {
                    writer.println(inst.getSentences());
                    writer.flush();
                }
            } else {
                writer.println(inst.getSentences());
                writer.flush();
            }

        }
        writer.close();
    }

    public String getProcessName(String fileName) {
        return fileName.substring(0, fileName.indexOf(".train"));
    }

    public int getFoldNumber(String fileName) {
        return Integer.parseInt(fileName.substring(fileName.lastIndexOf(".") + 1));
    }

    public ArrayList<FoldInstance> loadAllCrossValidationTrainingData(String dirName) throws FileNotFoundException {
        File[] files = FileUtil.getFilesFromDir(dirName, ".train.cv");
        StringBuilder sb = new StringBuilder();
        ArrayList<FoldInstance> trainingInstances = new ArrayList<FoldInstance>();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].getName().contains("\\|")) {
                Scanner scanner = new Scanner(new File(dirName + "/" + files[i].getName()));
                sb.setLength(0);
                System.out.println(files[i].getName());
                String processName = getProcessName(files[i].getName());
                int foldNumber = getFoldNumber(files[i].getName());
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    sb.append(line + "\n");
                }
                FoldInstance instance = new FoldInstance(processName, foldNumber, sb.toString());
                trainingInstances.add(instance);
                scanner.close();
            }
        }
        return trainingInstances;
    }

    public HashMap<String, Integer> collectProcessFoldInfo(String dirName) {
        HashMap<String, Integer> processNameNbFold = new HashMap<String, Integer>();
        File[] files = FileUtil.getFilesFromDir(dirName);

        for (int i = 0; i < files.length; i++) {
            String curFileName = files[i].getName();
            if (curFileName.contains(".train.cv")) {
                String processName = getProcessName(curFileName);
                if (processNameNbFold.containsKey(processName)) {
                    processNameNbFold.put(processName, processNameNbFold.get(processName) + 1);
                } else {
                    processNameNbFold.put(processName, 1);

                }
            }
        }
        return processNameNbFold;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
        SRLTrainOnOverallProcess p = new SRLTrainOnOverallProcess();
        // TEST ON NB FOLD PER PROCESS
        HashMap<String, Integer> procFold = p.collectProcessFoldInfo("./data/processes_18_may");

        // TEST ON COLLECTING THE TRAINING INSTANCES CREATE APPROPRIATE TRAINING DATA
        ArrayList<FoldInstance> instances = p.loadAllCrossValidationTrainingData("./data/processes_18_may");

        // CREATE TRAINING DATA
        for (String process : procFold.keySet()) {
            for (int fold = 0; fold < procFold.get(process); fold++) {
                System.out.println("CREATE TRAINING DATA FOR THIS FOLD");
                p.createTrainingData("./data/processes_18_may", process, fold, instances);
            }
        }
        // TRAIN, PREDICT, EVAL for each fold cross validation part
        int cnt = 0;
        try (PrintWriter resultWriter = new PrintWriter(new File("./data/processes_18_may/jointModelResults.txt"))) {
            for (String process : procFold.keySet()) {
                for (int fold = 0; fold < procFold.get(process); fold++) {
                    System.out.println(cnt + " " + process + " " + fold);
                    String modelName = "./data/processes_18_may" + "/" + process + ".jointmodel." + fold;
                    String trainingFileName = "./data/processes_18_may" + "/" + process + ".jointtrain.cv." + fold;
                    String testingFileName = "./data/processes_18_may" + "/" + process + ".test.cv." + fold;
                    String predictionFileName = "./data/processes_18_may" + "/" + process + ".jointpredict.cv" + fold;

                    System.out.println("TRAIN");
                    ClearParserUtil.clearParserTrain(modelName, trainingFileName); // TRAIN
                    System.out.println("TEST");
                    ClearParserUtil.clearParserPredict(modelName, testingFileName, predictionFileName); // PREDICT
                    System.out.println("EVAL");
                    String results = ClearParserUtil.clearParserEval(process, fold, testingFileName, predictionFileName); //EVAL
                    resultWriter.print(results);
                    resultWriter.flush();
                    cnt++;
                }
            }
        }
    }
}
