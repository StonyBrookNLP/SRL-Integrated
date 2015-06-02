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
public class SRLType {
    
    public static void main(String[] args) throws IOException
    {
        //ClearParserUtil.clearParserPredictOnly("/Users/samuellouvan/Downloads/clearparser-read-only/data/evaporate_evaporation.jointmodel.0", 
        //                                       "./data/testsp/test_processes.clearparser", 
        //                                       "./data/testsp/test.out");
        ClearParserUtil.clearParserEvalOnly("./data/testsp/test_processes.clearparser", "./data/testsp/test.out");
    }
}
