/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.srl;

import Util.Constant;
import Util.StdUtil;
import Util.StringUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
public class SRLEvaluate {

    public void evaluate(ArrayList<String> testFilePath, String testFilePattern, String predictFilePattern, int srlType) throws FileNotFoundException, IOException {
        System.out.println("Evaluating");
        PrintWriter gs_writer = new PrintWriter("gs.txt");
        PrintWriter srl_writer = new PrintWriter("srl.txt");
        for (int i = 0; i < testFilePath.size(); i++) {
            String[] gsTxt = FileUtil.readLinesFromFile(testFilePath.get(i));
            String[] srlTxt = FileUtil.readLinesFromFile(testFilePath.get(i).replace(testFilePattern, predictFilePattern));
            if (gsTxt.length != srlTxt.length) {
                System.out.println(testFilePath.get(i));
                System.out.println("MISMATCH DUE TO CLEARPARSER ERROR");
            } else {
                gs_writer.print(StringUtil.toString(gsTxt));
                srl_writer.print(StringUtil.toString(srlTxt));
            }
        }
        gs_writer.close();
        srl_writer.close();

        if (srlType == Constant.SRL_MATE) {
            // convert to clearparser format
            FileUtil.fromConll2009ToClearParserFormat("gs.txt", "gs_temp.txt");
            new File("gs.txt").delete();
            FileUtil.fromConll2009ToClearParserFormat("srl.txt", "srl_temp.txt");
            new File("srl.txt").delete();

            new File("gs_temp.txt").renameTo(new File("gs.txt"));
            new File("srl_temp.txt").renameTo(new File("srl.txt"));
        }
        
        // create runtime to execute external command
        String pythonScriptPath = "./script/evaluate.py";
        String[] cmd = new String[4];
        cmd[0] = "python";
        cmd[1] = pythonScriptPath;
        cmd[2] = "gs.txt";
        cmd[3] = "srl.txt";
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(cmd);

        // retrieve output from python script
        BufferedReader bfr = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line = "";
        while ((line = bfr.readLine()) != null) {
            // display each output line form python script
            System.out.println(line);
        }
        StdUtil.printError(pr);
    }
}
