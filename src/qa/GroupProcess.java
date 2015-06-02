 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import Util.StringUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author samuellouvan
 */
public class GroupProcess {

    public ArrayList<String> readFrames(String masterFile) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(masterFile));
        ArrayList<String> frames = new ArrayList<String>();
        int cnt = 0;
        while (scanner.hasNextLine()) {
            
            String line = scanner.nextLine();
            if (!StringUtil.isHeader(line))
            {
                frames.add(line.trim());
                cnt++;
            }
            
        }

        return frames;
    }
    /*
    
     Read the overall process/question frame then output files for each process
     */

    public void generateIndividualProcessesFile(String masterFile, String outDir) throws FileNotFoundException {
        File outDirF = new File(outDir);
        if (!outDirF.exists() )
        {
            outDirF.mkdir();
        }
        ArrayList<String> frames = readFrames(masterFile);
        String currentProcessName = frames.get(0).split("\t")[0]; // Get the name
        StringBuilder sb = new StringBuilder();
        PrintWriter writer = null;
        for (int i = 0; i < frames.size(); i++) {
            String[] frame = frames.get(i).trim().split("\t");
            if (!frame[0].equalsIgnoreCase(currentProcessName)) {
                writer = new PrintWriter(outDir+currentProcessName.replaceAll("\\s+","") + ".tsv");
                writer.print(sb.toString().trim());
                writer.close();
                sb.setLength(0);
                System.out.println("PROCESSED");
                currentProcessName = frame[0];
            }
            sb.append(frames.get(i) + "\n");
        }
        // Flush the last one
        System.out.println(currentProcessName);
        writer = new PrintWriter(outDir+currentProcessName + ".tsv");
        writer.print(sb.toString().trim());
        writer.close();
    }

    public static void main(String[] args) throws FileNotFoundException {
        GroupProcess gp = new GroupProcess();
        // TO USE
        // Specify the file name which contains ALL frames and the directory where do you want to store the output files
        gp.generateIndividualProcessesFile("./data/all_processes_23_may_2015.tsv", "./data/processes_23_may_2015/");
    }
}
