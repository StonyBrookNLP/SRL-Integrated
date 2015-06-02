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
public class StanfordTokenizerSingleton extends StanfordTokenizer{

    private static StanfordTokenizerSingleton instance = null;

    protected StanfordTokenizerSingleton() {
        // Exists only to defeat instantiation.
    }

    public static StanfordTokenizerSingleton getInstance() {
        if (instance == null) {
            instance = new StanfordTokenizerSingleton();
        }
        return instance;
    }
}
