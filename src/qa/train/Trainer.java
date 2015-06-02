/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.train;

import Util.GlobalVariable;
import clear.engine.SRLPredict;
import clear.engine.SRLTrain;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashMap;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import qa.GroupProcess;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;

import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
public class Trainer {

    private String PROJECT_PATH = "/Users/samuellouvan/NetBeansProjects/QA";

    String[] trainingArgs = {"-c", GlobalVariable.PROJECT_DIR.concat("/config/config_srl_en.xml"),
        "-i", "",
        "-t", GlobalVariable.PROJECT_DIR.concat("/config/feature_srl_en_conll09.xml"),
        "-m", ""};

    String[] predictArgs = {"-c", GlobalVariable.PROJECT_DIR.concat("/config/config_srl_en.xml"),
        "-i", "",
        "-o", "",
        "-m", ""};

    /**
     *
     * @param tsvFileName the source of the process file data
     * @param modelName the model name
     */
    public void train(String dirName, String tsvFileName, String clearParserFileName, String modelName) throws FileNotFoundException, IOException {
        // load data from tsv
        //ProcessFrameProcessor proc = new ProcessFrameProcessor(dirName + tsvFileName);
        //proc.loadProcessData();
        //proc.toClearParserFormat(dirName + clearParserFileName); // it will create tsvFileName.clearparser

        SRLTrain srlTrain = new SRLTrain();
        CmdLineParser cmd = new CmdLineParser(srlTrain);

        try {
            trainingArgs[3] = dirName + clearParserFileName; // -i
            trainingArgs[7] = dirName + FileUtil.getFileNameWoExt(tsvFileName).replaceAll("\\|", "_").replaceAll("\\s+", "") + "." + modelName; // -m
            System.out.println(Arrays.toString(trainingArgs));
            cmd.parseArgument(trainingArgs);
            srlTrain.init();
            srlTrain.train();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            cmd.printUsage(System.err);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void train(String dirName, String clearParserFileName, String modelName) throws FileNotFoundException, IOException {
        // load data from tsv

        SRLTrain srlTrain = new SRLTrain();
        CmdLineParser cmd = new CmdLineParser(srlTrain);

        try {
            trainingArgs[3] = dirName + clearParserFileName; // -i
            trainingArgs[7] = dirName + modelName; // -m
            System.out.println(Arrays.toString(trainingArgs));
            cmd.parseArgument(trainingArgs);
            srlTrain.init();
            srlTrain.train();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            cmd.printUsage(System.err);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void predict(String dirName, String testFileName, String modelFileName, String predictionFileName) {
        predictArgs[3] = dirName + testFileName; // -i
        predictArgs[5] = dirName + predictionFileName; // -o
        predictArgs[7] = dirName + modelFileName;// -m
        new SRLPredict(predictArgs);
    }

    public void train(String dirName, String modelName) throws IOException {
        File[] tsvFiles = FileUtil.getFilesFromDir(dirName, ".tsv");
        for (File tsvFile : tsvFiles) {
            System.out.println(tsvFile.getName());
            train(dirName, tsvFile.getName(), FileUtil.getFileNameWoExt(tsvFile.getName()) + ".clearparser", modelName);
        }
    }

    // This is actually can be used for per process model
    public void crossValidation(String dirName, String tsvFileName, String modelName) throws FileNotFoundException, IOException {
        ProcessFrameProcessor proc = new ProcessFrameProcessor(dirName + tsvFileName);
        proc.loadProcessData();
        HashMap<String, Integer> processFold = new HashMap<String, Integer>();
        for (int i = 0; i < proc.getProcArr().size(); i++) {
            ProcessFrame frame = proc.getProcArr().get(i);
            String processName = frame.getProcessName().trim();
            if (proc.getDataCount(processName) > 1) {
                int fold;
                if (processFold.containsKey(processName)) {
                    fold = processFold.get(processName) + 1;
                    processFold.put(processName, fold);
                } else {
                    fold = 0;
                    processFold.put(processName, fold);
                }
                processName = processName.replaceAll("\\|", "_");
                processName = processName.replaceAll("\\s+", "");
                FileUtil.dumpToFile(frame.toString(), dirName + "temp");
                ProcessFrameProcessor procTemp = new ProcessFrameProcessor(dirName + "temp");
                procTemp.loadProcessData();
                procTemp.toClearParserFormat(dirName + processName + ".test.cv." + fold);
                File file = new File(dirName + "temp");
                file.delete();

                // create train data
                ArrayList<String> trainingData = new ArrayList<String>();
                for (int j = 0; j < proc.getProcArr().size(); j++) {
                    if (i != j) {
                        trainingData.add(proc.getProcessFrame(j).toString());
                    }
                }
                FileUtil.dumpToFile(trainingData, dirName + "temp", "\n");
                procTemp = new ProcessFrameProcessor(dirName + "temp");
                procTemp.loadProcessData();
                procTemp.toClearParserFormat(dirName + processName + ".train.cv." + fold);
                file = new File(dirName + "temp");
                file.delete();

                train(dirName, tsvFileName, processName + ".train.cv." + fold, modelName + ".cv." + fold);
            }
        }

    }

    public void crossValidationPerProcessTrain(String dirName, String modelName) throws IOException {
        File dir = new File(dirName);
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains("tsv") && !name.startsWith("all_");
            }
        });

        for (File f : files) {
            crossValidation(dirName, f.getName(), modelName);
        }
    }

    public void crossValidationPerProcessPredict(String dirName, String modelName) {
        File dir = new File(dirName);
        File files[] = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains("test.cv");
            }
        });

        for (int i = 0; i < files.length; i++) {
            predict(dirName, files[i].getName(), files[i].getName().replace("test", modelName), files[i].getName().replace("test", "predict"));
        }

    }

    public void crossValidationCombinedTrain(String dirName, String modelName) throws FileNotFoundException, IOException {
        File dir = new File(dirName);
        File[] testFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains("test.cv") && !name.contains("all_");
            }
        });

        for (int i = 0; i < testFiles.length; i++) {
            PrintWriter writer = new PrintWriter(dirName + testFiles[i].getName().replace("test", "train.combined"));
            for (int j = 0; j < testFiles.length; j++) {
                if (i != j) {
                    String[] lines = FileUtil.readLinesFromFile(dirName + testFiles[j].getName());
                    for (String line : lines) {
                        writer.println(line);
                    }
                }

            }
            writer.close();
            String fileName = testFiles[i].getName();
            int fold = Integer.parseInt(fileName.substring(fileName.lastIndexOf(".") + 1));
            String processName = fileName.substring(0, fileName.indexOf("."));
            train(dirName, testFiles[i].getName().replace("test", "train.combined"), processName + "." + modelName + ".cv." + fold);
        }
    }

    public void crossValidationCombinedPredict(String dirName) {
        File dir = new File(dirName);
        File[] testFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains("test.cv");
            }
        });

        for (int i = 0; i < testFiles.length; i++) {
            //predict(String dirName, String testFileName, String modelFileName, String predictionFileName) {
            predict(dirName, testFiles[i].getName(), testFiles[i].getName().replace("test", "combined"), testFiles[i].getName().replace("test", "combinedpredict"));
        }
    }

    public static void main(String[] args) throws IOException {
        // For PER PROCESS MODEL
        GroupProcess proc = new GroupProcess();
        proc.generateIndividualProcessesFile("./data/all_processes_23_may_2015.tsv", "./data/processes_23_may_2015/");
        Trainer trainer = new Trainer();
        //trainer.crossValidationPerProcessTrain(GlobalVariable.PROJECT_DIR + "/data/processes_23_may_2015/", "perprocess");
        trainer.crossValidationPerProcessPredict(GlobalVariable.PROJECT_DIR + "/data/processes_23_may_2015/", "perprocess");

        // For COMBINED MODEL
        // Assume everything like test, train tsv for PER PROCESS exist
        //Trainer trainer = new Trainer();
        //trainer.crossValidationCombinedTrain(GlobalVariable.PROJECT_DIR + "/data/processes_23_may_2015/","combined");
        //trainer.crossValidationCombinedPredict(GlobalVariable.PROJECT_DIR + "/data/processes_23_may_2015/");
        

        //DEBUGGGGGGGGGGG
        //trainer.crossValidation(GlobalVariable.PROJECT_DIR + "/data/processes_23_may_2015/", "all_processes_23_may_2015.tsv", "");
        //trainer.train("/Users/samuellouvan/NetBeansProjects/QA/data/processes_23_may_2015/", "revising.tsv", "revising.clearparser");
        //trainer.train(GlobalVariable.PROJECT_DIR + "/data/processes_23_may_2015/", "singlemodel");
    }
}
