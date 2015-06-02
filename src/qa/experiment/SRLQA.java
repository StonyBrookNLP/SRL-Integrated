/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import Util.ClearParserUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import qa.ConLL09Generator;
import qa.QADeclarativeData;
import qa.QADeclarativeDataProcessor;
import qa.StanfordLemmatizerSingleton;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
public class SRLQA {

    public static StanfordLemmatizerSingleton ltmzr = StanfordLemmatizerSingleton.getInstance();
    private String fileName;
    private QADeclarativeDataProcessor proc;

    public SRLQA(String fileName) {
        this.fileName = fileName;
        proc = new QADeclarativeDataProcessor(fileName);
    }

    public void loadQuestionData() throws FileNotFoundException {
        proc.loadData();
    }

    public String wrapOldTrigger(ArrayList<String> lst) {
        if (lst.size() == 0) {
            return "";
        }
        if (lst.size() == 1) {
            return lst.get(0);
        }

        String str = "";
        for (int i = 0; i < lst.size(); i++) {
            str += lst.get(i) + "|";
        }
        return str.substring(0, str.length() - 1);

    }

    public String getModel(String modelDir, String choice)
    {
        File[] files = FileUtil.getFilesFromDir(modelDir, ".perprocess.model");
        for (File f : files)
        {
            if (f.getName().contains(choice) && !f.getName().contains("boot"))
                return f.getName();
        }
        return "";
    }
            
    public void evaluate(String fileName) throws IOException, FileNotFoundException, InterruptedException {
        PrintWriter writer = new PrintWriter(fileName);
        ArrayList<QADeclarativeData> questionDatas = proc.getData();
        int nbCorrect = 0;
        for (int i = 0; i < questionDatas.size(); i++) {
            System.out.println(i);
            QADeclarativeData qData = questionDatas.get(i);
            ArrayList<String> choices = qData.getOptions();
            String correctAnswer = qData.getCorrectAnswer();
            String currentTrigger = correctAnswer;
            String decSentence = qData.getDeclarativeSentence();
            String oldTrigger = wrapOldTrigger(qData.getOldTrigger());

            String bestAnswer = "";
            double bestConfidence = -1;
            StringBuilder sb = new StringBuilder();
            while (choices.size() > 0) {
                //System.out.println("SENTENCE " + decSentence);
                generateClearParserFileFromSentence(decSentence, currentTrigger, oldTrigger);
                System.out.println(currentTrigger);
                String modelName = currentTrigger.replaceAll("\\s+", "");
                String choiceModel = getModel("/Users/samuellouvan/NetBeansProjects/QA/data/processes_may",modelName);
                double score = 0;
                if (!choiceModel.equalsIgnoreCase(""))
                    score = ClearParserUtil.clearParserPredict("/Users/samuellouvan/NetBeansProjects/QA/data/processes_may/"+choiceModel, "temp.clearparser", "question"+i+"_"+currentTrigger);
                else
                {
                    System.out.println("Model not found!! :"+currentTrigger);
                }
                //double score = ClearParserUtil.clearParserPredict("/Users/samuellouvan/Downloads/clearparser-read-only/data/evaporate_evaporation.jointmodel.0", "temp.clearparser", "question"+i+"_"+currentTrigger);
                if (score > bestConfidence) {
                    bestConfidence = score;
                    bestAnswer = currentTrigger;
                }
                sb.append(currentTrigger+":");
                sb.append(""+score);
                sb.append(" ");
                choices.remove(choices.indexOf(currentTrigger));
                if (choices.size() > 0) {
                    decSentence = decSentence.replaceAll(currentTrigger, choices.get(0));
                    currentTrigger = choices.get(0);

                }
            }

            System.out.println("i:" + i + " +CORRECT ANSWER : " + correctAnswer + " PREDICTED ANSWER:" + bestAnswer + "SCORE : " + bestConfidence);
            writer.println(qData.getQuestion()+"\t"+qData.getCorrectAnswer()+"\t"+bestAnswer+"\t"+bestConfidence+"\t"+sb.toString());
            writer.flush();
            if (correctAnswer.equalsIgnoreCase(bestAnswer)) {
                System.out.println("CORRECT");
                nbCorrect++;
            }
        }
        System.out.println("NB CORRECT : " + nbCorrect);
        writer.close();
    }   

    public void generateClearParserFileFromSentence(String sentence, String currentTrigger, String oldTrigger) throws FileNotFoundException, IOException, InterruptedException {
        PrintWriter writer = new PrintWriter("temp.sent");
        writer.println(sentence);
        writer.close();
        writer = new PrintWriter("temp.tsv");
        if (oldTrigger.length() > 0) {
            writer.println(" " + "\t" + " " + "\t" + " " + "\t" + currentTrigger + "|" + oldTrigger + "\t" + " " + "\t" + " " + "\t" + sentence + "\t" + " ");
        } else {
            writer.println(" " + "\t" + " " + "\t" + " " + "\t" + currentTrigger + "\t" + " " + "\t" + " " + "\t" + sentence + "\t" + " ");
        }
        writer.close();

        ConLL09Generator.generateCONLLFileFromFile("temp.sent");
        new ConLL09Generator("temp.tsv", "").generateClearParserFileFromFile("temp.tsv");

    }

    public static void main(String[] args) throws IOException, FileNotFoundException, InterruptedException {
        SRLQA srl = new SRLQA("./data/SRL_QA_Declarative_Data_Single_Sentence.tsv");
        srl.loadQuestionData();
        srl.evaluate("eval2.txt");
        //srl.generateClearParserFileFromSentence("Liquid changes to water vapor in the air is called evaporation.", "evaporation","");
        /*File[] files = FileUtil.getFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/processes",".model.0");
        for (File f : files)
        {
            System.out.println(f.getName());
        }*/
    }
}
