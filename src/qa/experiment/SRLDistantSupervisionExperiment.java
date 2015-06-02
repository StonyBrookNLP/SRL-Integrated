/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import Util.ClearParserUtil;
import java.io.IOException;

/**
 *
 * @author samuellouvan
 */
public class SRLDistantSupervisionExperiment {
    
    public static void main(String[] args) throws IOException, InterruptedException
    {
        ClearParserUtil.clearParserTrain("/Users/samuellouvan/NetBeansProjects/QA/data/evaporation_manual/evaporation-manual.model", 
                                         "/Users/samuellouvan/NetBeansProjects/QA/data/evaporation_manual/evaporation-manual.clearparser");
        
        /*ClearParserUtil.clearParserPredictOnly("/Users/samuellouvan/Downloads/clearparser-read-only/data/evaporate_manual_newdepfeat.model", 
                                               "/Users/samuellouvan/Downloads/clearparser-read-only/data/small-evaporation.clearparser", 
                                               "/Users/samuellouvan/Downloads/clearparser-read-only/data/test.out");*/
        /*ClearParserUtil.clearParserPredictOnly("/Users/samuellouvan/NetBeansProjects/QA/data/evaporation_manual/evaporate_manual_newdepfeat.model", 
                                               "/Users/samuellouvan/NetBeansProjects/QA/data/evaporation_manual/evaporation.clearparser", 
                                               "/Users/samuellouvan/NetBeansProjects/QA/data/evaporation_manual/evaporate_manual_newdepfeat.predict");
        
        ClearParserUtil.clearParserEvalOnly("/Users/samuellouvan/NetBeansProjects/QA/data/evaporation_manual/evaporation.clearparser", 
                                           "/Users/samuellouvan/NetBeansProjects/QA/data/evaporation_manual/evaporate_manual_newdepfeat.predict");*/
        /*ClearParserUtil.clearParserEvalOnly("/Users/samuellouvan/NetBeansProjects/QA/data/evaporation_ds_filtered/evaporation.clearparser", 
                                            "/Users/samuellouvan/NetBeansProjects/QA/data/evaporation_ds_filtered/newAnnotatedFiltered.predict");*/
    }
}
