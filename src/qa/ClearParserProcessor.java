/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author samuellouvan
 */
public class ClearParserProcessor {
    
    public ArrayList<String> loadSentences(String dirName, String clearParserFileName) throws FileNotFoundException {
        ArrayList<String> sentences = new ArrayList<String>();
        if (clearParserFileName.equalsIgnoreCase("")) {
            File folder = new File(dirName);
            File[] listOfFiles = folder.listFiles();

            //for (int i = 0; i < listOfFiles.length; i++) {
            //}
        } else {
            Scanner scanner = new Scanner(new File(dirName+"/"+clearParserFileName));
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                if (line.trim().isEmpty())
                {
                    sentences.add(sb.toString().trim());
                    sb.setLength(0);
                }
                sb.append(line+"\n");
            }
        }
        return sentences;
    }
    
    public static void main(String[] args) throws FileNotFoundException
    {
        ClearParserProcessor cp     = new ClearParserProcessor();
        ArrayList<String> sentences = cp.loadSentences("./data/processes", "transpiration.clearparser");
        System.out.println(sentences.get(3));
    }
}
