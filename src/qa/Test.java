/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;


import java.io.File;
import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import org.apache.commons.lang3.StringUtils;

class Test {


    public static void main(String[] args) throws FileNotFoundException, IOException {
        /*String str = "absorb |    samuel";
        String tokens[] = str.split("\\|");
        System.out.println(tokens.length);
        System.out.println(tokens[0].trim().length());
        System.out.println(tokens[1].trim().length());*/
        
       /* Scanner scanner = new Scanner(new File("data/all_question_frame.txt"));
        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            System.out.println(line);
        }*/
        //String s = "process|name";
        //System.out.println(System.getProperty("java.class.path"));
        //System.out.println(StringUtils.getLevenshteinDistance("polluting", "pollution", 3));
        
        /*StanfordLemmatizer slem = new StanfordLemmatizer();
        Scanner scanner = new Scanner(new File("./data/sp/testSetRandom.tsv"));
        PrintWriter writer = new PrintWriter("./data/sp/testSetRandomCleaned.tsv");
        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            String[] fields = line.split("\t");
            String trigger = fields[0];
            String correctUndergoer = slem.lemmatize(fields[1].trim()).get(0);
            String wrongUndergoer = slem.lemmatize(fields[2].trim()).get(0);
            writer.println(trigger+"\t"+correctUndergoer+"\t"+wrongUndergoer);
        }
        writer.close();*/
        File file = new File(".");
        System.out.println(file.getCanonicalPath());
    }
}
