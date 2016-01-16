/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import java.util.ArrayList;
import org.kohsuke.args4j.Option;
import sbu.srl.datastructure.Sentence;

/**
 *
 * @author slouvan
 */

/* This class will use a trained model then apply to the unknown sentences and perform extraction of roles*/
public class KnowledgeHarvester {
    
    SpockDataReader dataReader;
    @Option(name = "-m", usage = "trained model name", required = true)
    private String modelName;
    
    @Option(name = "-f", usage = "file name that contains target sentences", required = true)
    private String sentFileName;
    
    @Option(name = "-c", usage = "configFileName", required = true)
    private String configFileName;
    
    public void extractKnowledge()
    {
        // read 
        // load model
        //SBURolePredict predictor = new SBURolePredict(modelName, sentFileName, true);
        // for each sentence perform prediction
            // for this one call method in SBU role
    }
    
    
    public static void main(String[] args)
    {
        
    }
}
