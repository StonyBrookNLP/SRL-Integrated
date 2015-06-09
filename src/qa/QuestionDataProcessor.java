/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import qa.util.FileUtil;

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

    public ArrayList<QuestionData> getQuestionData() {
        return questionArr;
    }

    public void loadQuestionData() throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(this.questionFileName));
        questionArr = new ArrayList<QuestionData>();
        while (scanner.hasNextLine()) {
            String[] data = scanner.nextLine().split("\t");
            if (!data[0].equalsIgnoreCase("QUESTION")) {
                System.out.println(Arrays.toString(data));
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

    }

    public int getNbQuestion() {
        return questionArr.size();
    }

    public QuestionData getQuestionData(int idx) {
        return questionArr.get(idx);
    }

    public static void main(String[] args) throws FileNotFoundException {
        QuestionDataProcessor proc = new QuestionDataProcessor("./data/question.tsv");
        proc.loadQuestionData();
        ArrayList<QuestionData> questionsData = proc.questionArr;
        ArrayList<String> trainingProcesses = new ArrayList<String>();
        ArrayList<String> testProcesses = new ArrayList<String>();
        for (int i = 0; i < 60; i++) {
            QuestionData qDat = questionsData.get(i);
            String[] choices = qDat.getAnswers();
            for (String choice : choices) {
                if (!trainingProcesses.contains(choice) && choice.trim().length() > 0) {
                    trainingProcesses.add(choice);
                }
            }
        }
        System.out.println("TRAINING");
        for (int i = 0; i < trainingProcesses.size(); i++) {
            System.out.println(trainingProcesses.get(i));
        }

        ArrayList<Integer> validQuestions = new ArrayList<Integer>();

        for (int i = 60; i < questionsData.size(); i++) {
            QuestionData qDat = questionsData.get(i);
            String[] choices = qDat.getAnswers();
            boolean valid = true;
            for (String choice : choices) {
                if (trainingProcesses.contains(choice)) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                System.out.println(i);
                validQuestions.add(i);
                for (String choice : choices) {
                    if (!testProcesses.contains(choice) && choice.trim().length() > 0) {
                        testProcesses.add(choice);
                    }
                }
            }
        }
        System.out.println("TESTING");

        for (int i = 0; i < testProcesses.size(); i++) {
            System.out.println(testProcesses.get(i));
        }

        String[] questions = FileUtil.readLinesFromFile("./data/question.tsv");
        PrintWriter writer = new PrintWriter("./data/questionForTesting.tsv");
        for (int i = 0; i < questions.length; i++) {
            if (validQuestions.contains(i)) {
                writer.println(questions[i]);
            }
        }
        writer.close();
    }
}
