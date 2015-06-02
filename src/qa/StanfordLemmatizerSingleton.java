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
public class StanfordLemmatizerSingleton extends StanfordLemmatizer{

    private static StanfordLemmatizerSingleton instance = null;

    protected StanfordLemmatizerSingleton() {
        // Exists only to defeat instantiation.
    }

    public static StanfordLemmatizerSingleton getInstance() {
        if (instance == null) {
            instance = new StanfordLemmatizerSingleton();
        }
        return instance;
    }
}
