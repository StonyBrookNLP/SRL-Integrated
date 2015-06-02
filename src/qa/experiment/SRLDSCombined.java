/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import clear.engine.SRLPredict;
import java.io.File;

/**
 *
 * @author samuellouvan
 */
public class SRLDSCombined {

    public static void main(String[] args) {
        String dirName = "/Users/samuellouvan/NetBeansProjects/QA/data/";
        String evalDir = dirName + "ds_combined_most_frequent_eval/";
        File[] testFiles = new File(evalDir).listFiles();
        String ds_model = "ds_combined_most_frequent.model";
        
        String[] predictArgs = {"-c", "/Users/samuellouvan/NetBeansProjects/QA/config/config_srl_en.xml",
            "-i", "",
            "-o", "",
            "-m", dirName + ds_model};

        for (int i = 0; i < testFiles.length; i++) {
                if (testFiles[i].getName().contains(".test.cv")) {
                    System.out.println("TESTING!!");
                    predictArgs[5] = evalDir + testFiles[i].getName().replace("test", "dscombinedpredict");
                    predictArgs[3] = evalDir + testFiles[i].getName();
                    new SRLPredict(predictArgs);
                }
            
        }
    }
}
