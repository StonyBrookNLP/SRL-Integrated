/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.train;

import Util.ClearParserUtil;
import java.io.File;
import java.io.IOException;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
public class PerProcessTrain {
    
    public static void main(String[] args) throws IOException, InterruptedException
    {
        File[] files = FileUtil.getFilesFromDir("/Users/samuellouvan/NetBeansProjects/QA/data/processes_may", ".clearparser");
        for (File f: files)
        {
            String nameWithoutExt = FileUtil.getFileNameWoExt(f.getName());
            ClearParserUtil.clearParserTrain("/Users/samuellouvan/NetBeansProjects/QA/data/processes_may/"+nameWithoutExt+".perprocess.model", 
                                             "/Users/samuellouvan/NetBeansProjects/QA/data/processes_may/"+nameWithoutExt+".clearparser");
            
        }
    }
}
