/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.brat;

import clear.util.FileUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author slouvan
 */
public class AnnotationProcessor {
    static String[] Roles = {"Undergoer", "Enabler", "Trigger", "Result", "Underspecified"};
    
    public static void main(String[] args) throws FileNotFoundException {
        File directory = new File("/home/slouvan/brat-v1.3_Crunchy_Frog/data/QA");
        File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".txt");
            }
        });
        
        PrintWriter questionFrameFile = new PrintWriter("./data/questionFrame.tsv");
        for (File f : files)
        {
            File annotationFile = new File(f.getAbsolutePath().replace(".txt", ".ann"));
            String sentence = FileUtil.readLinesFromFile(f.getAbsolutePath())[0];
            System.out.println(sentence);
            System.out.println(annotationFile.getAbsolutePath());
            String lines[] = FileUtil.readLinesFromFile(annotationFile.getAbsolutePath());
            HashMap<String,String> roleTypeValuesPair = new HashMap<String,String>();
            for (int i = 0; i < lines.length; i++)
            {
                String[] field = lines[i].split("\t");
                String role = field[1].split(" ")[0].trim();
                String values = field[2].trim();
                if (roleTypeValuesPair.get(role) == null)
                {
                    roleTypeValuesPair.put(role, values);
                }
                else
                {
                    roleTypeValuesPair.put(role, roleTypeValuesPair.get(role)+"|"+values);
                }
            }
            for (String key : roleTypeValuesPair.keySet())
            {
                System.out.println(key +" "+roleTypeValuesPair.get(key));
            }
        }
    }
}
