/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

/**
 *
 * @author samuellouvan
 */
public class QuestionData {
    public static final int OPTION_A = 0;
    public static final int OPTION_B = 1;
    public static final int OPTION_C = 2;
    public static final int OPTION_D = 3;
    
    private String questionSentence;
    private String[] answers;
    private String correctAnswer;

    public QuestionData(String questionSentence, String[] answers, String correctAnswer) {
        this.questionSentence = questionSentence;
        this.answers = answers;
        this.correctAnswer = correctAnswer;
    }

    public String getQuestionSentence() {
        return questionSentence;
    }

    public void setQuestionSentence(String questionSentence) {
        this.questionSentence = questionSentence;
    }

    public String[] getAnswers() {
        return answers;
    }

    public void setAnswers(String[] answers) {
        this.answers = answers;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
    
    
}
