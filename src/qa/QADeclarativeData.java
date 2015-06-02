/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import java.util.ArrayList;

/**
 *
 * @author samuellouvan
 */
public class QADeclarativeData {
    private String question;
    private ArrayList<String> options;
    private String correctAnswer;
    private int nbSentence;
    private String declarativeSentence;
    private ArrayList<String> oldTrigger;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public ArrayList<String> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<String> options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public int getNbSentence() {
        return nbSentence;
    }

    public void setNbSentence(int nbSentence) {
        this.nbSentence = nbSentence;
    }

    public String getDeclarativeSentence() {
        return declarativeSentence;
    }

    public void setDeclarativeSentence(String declarativeSentence) {
        this.declarativeSentence = declarativeSentence;
    }

    public ArrayList<String> getOldTrigger() {
        return oldTrigger;
    }

    public void setOldTrigger(ArrayList<String> oldTrigger) {
        this.oldTrigger = oldTrigger;
    }
    
    
}
