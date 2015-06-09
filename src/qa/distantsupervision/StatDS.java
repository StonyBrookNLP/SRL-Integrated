/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.distantsupervision;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;

/**
 *
 * @author samuellouvan
 */
public class StatDS {
    
    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException
    {
        ProcessFrameProcessor proc = new ProcessFrameProcessor("./data/ds_most_frequent/ds_combined.tsv");
        proc.loadProcessData();
        ArrayList<ProcessFrame> frames = proc.getProcArr();
        
        int enablerCnt = 0;
        int resultCnt = 0;
        int undergoerCnt = 0;
        for (int i = 0; i < frames.size(); i++)
        {
            ProcessFrame frame = frames.get(i);
            String enabler = frame.getEnabler();
            String result = frame.getResult();
            String undergoer = frame.getUnderGoer();
            if (!enabler.isEmpty())
                enablerCnt++;
            if (!result.isEmpty())
                resultCnt++;
            if (!undergoer.isEmpty())
                undergoerCnt++;
        }
        
        System.out.println("A0 "+undergoerCnt);
        System.out.println("A1 "+enablerCnt);
        System.out.println("A2 "+resultCnt);
        HashMap<String, Integer> procCount = proc.getProcessCount();
        int sum = 0;
        for (String processName : procCount.keySet())
        {
            sum += procCount.get(processName);
        }
        System.out.println(sum/(procCount.size() * 1.0));
    }
}
