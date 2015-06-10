/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import Util.GlobalVariable;
import Util.ProcessFrameUtil;
import clear.dep.DepTree;
import clear.reader.SRLReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;

/**
 *
 * @author samuellouvan
 */
public class SRLToAligner {
    
    public void generateTsvForAligner(String sourceTsvFile, String clearparserPrediction, String outTsvFile) throws IOException, FileNotFoundException, ClassNotFoundException
    {
        ProcessFrameProcessor proc = new ProcessFrameProcessor(sourceTsvFile);
        proc.loadProcessData();
        ArrayList<ProcessFrame> frames = proc.getProcArr();
        
        for (int i = 0; i < frames.size(); i++)
        {
            frames.get(i).setEnabler("");
            frames.get(i).setUnderGoer("");
            frames.get(i).setResult("");
        }
        SRLReader srlReader = new SRLReader(clearparserPrediction, true);
        ArrayList<DepTree> trees = new ArrayList<DepTree>();
        DepTree currentTree = null;
        while ( (currentTree = srlReader.nextTree()) != null)
        {
            trees.add(currentTree);
        }
        
        System.out.println(frames.size());
        System.out.println(trees.size());
        if (frames.size() != trees.size() )
        {
            System.out.println("NIGHTMARE");
            System.exit(0);
        }
        for (int i = 0; i < frames.size(); i++)
        {
            DepTree tree = trees.get(i);
            ArrayList<String> underGoers = tree.getRoleFillers("A0");
            ArrayList<String> enablers = tree.getRoleFillers("A1");
            ArrayList<String> results = tree.getRoleFillers("A2");
            frames.get(i).setUnderGoer(String.join(" ", underGoers));
            frames.get(i).setEnabler(String.join(" ", enablers));
            frames.get(i).setResult(String.join(" ", results));
        }
        
        ProcessFrameUtil.dumpFramesToFile(frames, outTsvFile);
        
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException
    {
        SRLToAligner srlTA = new SRLToAligner();
        String outFile = "framesAutoDS.tsv";
        srlTA.generateTsvForAligner(GlobalVariable.PROJECT_DIR+"/data/SRLQAPipeDS/question.predict.cv.0.tsv", 
                                    GlobalVariable.PROJECT_DIR+"/data/SRLQAPipeDS/question.predict.cv.0.clearparser", 
                                    GlobalVariable.PROJECT_DIR+"/data/SRLQAPipeDS/"+outFile);
    }
}
