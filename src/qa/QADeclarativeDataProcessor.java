/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
public class QADeclarativeDataProcessor {

    private ArrayList<QADeclarativeData> data = new ArrayList<QADeclarativeData>();
    private String fileName;
    private int QUESTION = 0;
    private int OPTIONS = 1;
    private int OPTION_A = 2;
    private int OPTION_B = 3;
    private int OPTION_C = 4;
    private int OPTION_D = 5;
    private int NB_SENTENCE = 6;
    private int DEC_SENTENCE = 7;
    private int OLD_TRIGGER = 8;
    private int CORRECT_ANS = 9;

    public QADeclarativeDataProcessor(String fileName) {
        this.fileName = fileName;
    }

    public ArrayList<QADeclarativeData> getData() {
        return data;
    }

    
    public void loadData() throws FileNotFoundException {
        String[] lines = FileUtil.readLinesFromFile(this.fileName);
        for (int i = 0; i < lines.length; i++) {
            String[] fields = lines[i].split("\t");
            QADeclarativeData qData = new QADeclarativeData();
            qData.setQuestion(fields[QUESTION]);
            ArrayList<String> options = new ArrayList<String>();
            for (int j = OPTION_A; j < OPTION_D + 1; j++) {
                if (fields[j].trim().length() > 0) {
                    options.add(fields[j].trim().toLowerCase());
                }
            }
            qData.setOptions(options);
            qData.setNbSentence(Integer.parseInt(fields[NB_SENTENCE]));
            if (fields[CORRECT_ANS].equalsIgnoreCase("OPTION_A")) {
                qData.setCorrectAnswer(fields[OPTION_A].trim().toLowerCase());
            } else if (fields[CORRECT_ANS].equalsIgnoreCase("OPTION_B")) {
                qData.setCorrectAnswer(fields[OPTION_B].trim().toLowerCase());
            } else if (fields[CORRECT_ANS].equalsIgnoreCase("OPTION_C")) {
                qData.setCorrectAnswer(fields[OPTION_C].trim().toLowerCase());
            } else {
                qData.setCorrectAnswer(fields[OPTION_D].trim().toLowerCase());
            }
            qData.setDeclarativeSentence(fields[DEC_SENTENCE].trim().toLowerCase());

            ArrayList<String> oldTriggers = new ArrayList<String>();
            String[] triggerTokens = fields[OLD_TRIGGER].split("\\|");
            for (int j = 0; j < triggerTokens.length; j++) {
                if (triggerTokens[j].trim().length() > 0) {
                    oldTriggers.add(triggerTokens[j].trim().toLowerCase());
                }
            }
            qData.setOldTrigger(oldTriggers);
            data.add(qData);
        }

    }

    public int countPeriod(String sentence)
    {
        int cnt = 0;
        for (int i = 0; i < sentence.length(); i++)
        {
            if (sentence.charAt(i) == '.')
                cnt++;
        }
        
        return cnt;
    }
    public boolean isDataValid() {
        for (int i = 0; i < data.size(); i++) {
            QADeclarativeData qData = data.get(i);
            String correctAnswer = qData.getCorrectAnswer();
            if (!qData.getDeclarativeSentence().contains(correctAnswer)) {
                System.out.println(qData.getQuestion());
                System.out.println("Correct answer is not in the declarative sentence :"+correctAnswer);
                System.out.println("");
                
            }
            ArrayList<String> oldTriggers = qData.getOldTrigger();
            for (int j = 0; j < oldTriggers.size(); j++) {
                if (!qData.getDeclarativeSentence().contains(oldTriggers.get(j))) {
                    System.out.println(qData.getDeclarativeSentence());
                    System.out.println("One of the triggers is not in the sentence :" + oldTriggers.get(j));
                    System.out.println("");
                }
            }
            int nbPeriod = countPeriod(qData.getDeclarativeSentence());
            if (nbPeriod != qData.getNbSentence())
                System.out.println(qData.getQuestion());
                System.out.println("Missing period");
        }
        return true;
    }

    public static void main(String[] args) throws FileNotFoundException {
        QADeclarativeDataProcessor proc = new QADeclarativeDataProcessor("./data/SRL_QA_Declarative_Data.tsv");
        proc.loadData();
        proc.isDataValid();
    }
}
