/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import qa.util.FileUtil;

/**
 *
 * @author slouvan
 */
public class Test {
    public static void main(String[] args) throws FileNotFoundException
    {
        String[] allAnnotation = FileUtil.readLinesFromFile("./data/extracted_sentences.cleaned.tsv", true, "process");
        String[] trainingAnnotation = FileUtil.readLinesFromFile("./data/all_training_data.cleaned.tsv", true, "process");
        PrintWriter writer = new PrintWriter("./data/extracted_sentences.cleaned.unique.tsv");
        int count = 0;
        for (int i = 0; i < allAnnotation.length; i++)
        {
            String[] fields = allAnnotation[i].split("\t");
            boolean found = false;
            for (int j = 0; j <  trainingAnnotation.length; j++)
            {
                String fields2[] = trainingAnnotation[j].split("\t");
                if (fields[0].equalsIgnoreCase(fields2[0]) && fields[1].equalsIgnoreCase(fields2[1])
                    && fields[8].equalsIgnoreCase(fields2[8]))
                {
                    System.out.println("SAMA");
                    count++;
                    found = true;
                    break;
                }
            }
            if (!found)
                writer.println(allAnnotation[i]);
        }
        System.out.println(count);
        writer.close();
    }
}
