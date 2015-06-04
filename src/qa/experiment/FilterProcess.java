/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import Util.ProcessFrameUtil;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;

/**
 *
 * @author samuellouvan
 */
public class FilterProcess {
    public static void main(String[] args) throws FileNotFoundException
    {
        ProcessFrameProcessor proc = new ProcessFrameProcessor("./data/process_frame_june.tsv");
        proc.loadProcessData();
        ArrayList<String> filteredProcess = new ArrayList<String>();
        HashMap<String,Integer> procCount = proc.getProcessCount();
        for (String processName : procCount.keySet())
        {
            ProcessFrameProcessor procDs = new ProcessFrameProcessor("./data/ds_most_frequent/"+ProcessFrameUtil.normalizeProcessName(processName)+"_ds.tsv");
            procDs.loadProcessData();
            System.out.println("Processing "+processName);
            if (procCount.get(processName) >= 5 && procDs.getDataCount(processName) >= 5 && filteredProcess.size() < 50)
            {
                filteredProcess.add(processName);
            }
        }
        
        Collections.shuffle(filteredProcess);
        ArrayList<ProcessFrame> allProcess = proc.getProcArr();
        ArrayList<ProcessFrame> process_50 = new ArrayList<ProcessFrame>();
        ArrayList<ProcessFrame> process_50_ds = new ArrayList<ProcessFrame>();
        for (int i = 0; i < filteredProcess.size(); i++)
        {
            String currentProcessName = filteredProcess.get(i);
            for (ProcessFrame frame : allProcess)
            {
                if (frame.getProcessName().equalsIgnoreCase(currentProcessName))
                {
                    process_50.add(frame);
                }
            }
        }
        ArrayList<ProcessFrame> process_100 = new ArrayList<ProcessFrame>();
        for (int i = 0; i < allProcess.size(); i++)
        {
            if (!filteredProcess.contains(allProcess.get(i).getProcessName()))
            {
                process_100.add(allProcess.get(i));
            }
        }
        
        ProcessFrameProcessor allprocessDSproc = new ProcessFrameProcessor("./data/ds_most_frequent/ds_combined.tsv");
        allprocessDSproc.loadProcessData();
        ArrayList<ProcessFrame> allProcessDs = allprocessDSproc.getProcArr();
        process_50_ds.addAll(process_50);
        for (int i = 0; i < allProcessDs.size(); i++)
        {
            if (filteredProcess.contains(allProcessDs.get(i).getProcessName()))
            {
                process_50_ds.add(allProcessDs.get(i));
            }
        }
        ProcessFrameUtil.dumpFramesToFile(process_50, "./data/process_50.tsv");
        ProcessFrameUtil.dumpFramesToFile(process_100, "./data/process_100.tsv");
        ProcessFrameUtil.dumpFramesToFile(process_50_ds, "./data/process_50_ds.tsv");
    }
}
