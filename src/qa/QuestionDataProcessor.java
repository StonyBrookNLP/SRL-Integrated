/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author samuellouvan
 */
public class QuestionDataProcessor {

    private String questionFileName;
    private ArrayList<QuestionData> questionArr;

    public QuestionDataProcessor(String questionFileName) {
        this.questionFileName = questionFileName;
    }

    public void loadQuestionData() throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(this.questionFileName));
        questionArr = new ArrayList<QuestionData>();
        while (scanner.hasNextLine()) {
            String[] data = scanner.nextLine().split("\t");
            String questionSent = data[0];
            String[] answers = new String[4];
            answers[0] = data[2];
            answers[1] = data[3];
            answers[2] = data[4];
            answers[3] = data[5];

            String correctAnswer = data[7];
            int correctAnsIDx = -1;
            if (correctAnswer.contains("A")) {
                correctAnsIDx = 0;
            }
            if (correctAnswer.contains("B")) {
                correctAnsIDx = 1;
            }
            if (correctAnswer.contains("C")) {
                correctAnsIDx = 2;
            }
            if (correctAnswer.contains("D")) {
                correctAnsIDx = 3;
            }
            QuestionData qData = new QuestionData(questionSent, answers, answers[correctAnsIDx]);
            questionArr.add(qData);
        }

    }

    public int getNbQuestion()
    {
        return questionArr.size();
    }
    
    public QuestionData getQuestionData(int idx)
    {
        return questionArr.get(idx);
    }
    
    
    public static void main(String[] args) throws FileNotFoundException {
        QuestionDataProcessor proc = new QuestionDataProcessor("./data/question.tsv");
        proc.loadQuestionData();
    }
}
