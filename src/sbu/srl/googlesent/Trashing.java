/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.googlesent;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import qa.util.FileUtil;

/**
 *
 * @author slouvan
 */
public class Trashing {

    public static void main(String[] args) throws FileNotFoundException {
        /*String[] linesSource = FileUtil.readLinesFromFile("./data/all_training_data.cleaned.lowercase.tsv", true, "process");
         HashMap<String, String> frameAnnoPair = new HashMap<String, String>();
         for (int i = 0; i < linesSource.length; i++) {
         String[] processFrame = new String[9];
            
         System.arraycopy(linesSource[i].split("\t"), 0, processFrame, 0, 9);
         int len = linesSource[i].split("\t").length;
         String[] annotation = new String[len-9];
         System.arraycopy(linesSource[i].split("\t"), 9, annotation, 0, len - 9);
         if (!frameAnnoPair.containsKey(String.join("\t", processFrame))) {
         frameAnnoPair.put(String.join("\t", processFrame), String.join("\t", annotation));
         } else {
         String[] annotation1 = frameAnnoPair.get(String.join("\t", processFrame)).split("\t");
         int newSize = Math.max(annotation.length, annotation1.length);
         String mergedAnnotation[] = new String[newSize];

         for (int j = 0; j < newSize; j++) {
         if (j < annotation1.length && j < annotation.length) {
         mergedAnnotation[j] = annotation1[j].isEmpty()?annotation[j]:annotation1[j];
         }
         else if ( j < annotation1.length)
         {
         mergedAnnotation[j] = annotation1[j];
         }
         else if (j < annotation.length )
         {
         mergedAnnotation[j] = annotation[j];
         }
         }
         frameAnnoPair.put(String.join("\t", processFrame),  String.join("\t", mergedAnnotation));
         }
         }
         PrintWriter writer = new PrintWriter("./data/merged.lowercase.tsv");
         for (String key : frameAnnoPair.keySet())
         {
         writer.println(key+"\t"+frameAnnoPair.get(key));
         }
         writer.close();*/

        String[] all = FileUtil.readLinesFromFile("./data/extracted_sentences.tsv", true, "process");
        HashMap<String, Integer> allFrame = new HashMap<String, Integer>();
        String[] processFrame = new String[9];
        for (int i = 0; i < all.length; i++) {
            System.arraycopy(all[i].split("\t"), 0, processFrame, 0, 9);
            allFrame.put(String.join("\t", processFrame), i);
        }
        
        String[] subset = FileUtil.readLinesFromFile("./data/merged.lowercase.tsv", true, "process");
        HashMap<String, Integer> subsetFrame = new HashMap<String, Integer>();
        processFrame = new String[9];
        for (int i = 0; i < subset.length; i++) {
            System.arraycopy(subset[i].split("\t"), 0, processFrame, 0, 9);
            subsetFrame.put(String.join("\t", processFrame), i);
        }
        int cnt = 0;
        for (String frame : subsetFrame.keySet())
        {
            if (!allFrame.containsKey(frame))
            {
                System.out.println(subset[subsetFrame.get(frame)].split("\t")[8]);
                cnt++;
            }
        }
        System.out.println(cnt);
    }
}
