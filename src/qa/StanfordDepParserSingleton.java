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
public class StanfordDepParserSingleton extends StanfordDepParser{

    private static StanfordDepParserSingleton instance = null;

    protected StanfordDepParserSingleton() {
        // Exists only to defeat instantiation.
    }

    public static StanfordDepParserSingleton getInstance() {
        if (instance == null) {
            instance = new StanfordDepParserSingleton();
        }
        return instance;
    }
}
