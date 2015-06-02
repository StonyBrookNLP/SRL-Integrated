/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.distantsupervision;

import Util.GlobalVariable;
import Util.StringUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
public class QueryPatternGenerator {

    public void generateQueryPattern(String fileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName));
        int cnt = 10;
        int currentDirCnt = cnt;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!StringUtil.isHeader(line)) {
                String fields[] = line.split("\t");
                String processName = fields[0].replaceAll("\\|", "_");
                processName = processName.replaceAll("\\s+", "");
                // mkdir 
                if (cnt % 10 == 0) {
                    FileUtil.mkDir(GlobalVariable.PROJECT_DIR + "/data/queries" + cnt + "/");
                    currentDirCnt = cnt;
                }
                constructQueryWords(fields, GlobalVariable.PROJECT_DIR + "/data/queries" + currentDirCnt + "/" + processName);
                cnt++;
            }
        }
        System.out.println(cnt);
    }

    public static String cleanString(String str) {
        if (str.length() > 0) {
            String newString = str.replaceAll(" in ", " ");
            newString = newString.replaceAll(" the ", " ");
            newString = newString.replaceAll(" to ", " ");
            newString = newString.replaceAll(" is ", " ");
            newString = newString.replaceAll(" a ", " ");
            return "\"" + newString + "\"";
        }
        return "";
    }

    public void constructQueryWords(String[] fields, String outFileName) throws FileNotFoundException {
        String underGoer = cleanString(fields[1]);
        String enabler = cleanString(fields[2]);
        String trigger = cleanString(fields[3]);
        String result = cleanString(fields[4]);

        if (trigger.length() <= 0) // If trigger is empty
        {
            trigger = "\"" + fields[0].trim() + "\"";
        }
        // Pattern 1, trigger, undergoer
        String pattern1 = "";
        if (underGoer.trim().length() != 0) {
            pattern1 = underGoer + "^" + trigger;
        }
        // Pattern 2, trigger, result
        String pattern2 = "";
        if (result.trim().length() != 0) {
            pattern2 = result + "^" + trigger;
        }

        // Pattern 3, trigger, undergoer, result
        String pattern3 = "";
        if (underGoer.trim().length() != 0 && result.trim().length() != 0) {
            pattern3 = underGoer + "^" + trigger + "^" + result;
        }

        // Pattern 4, trigger, enabler
        String pattern4 = "";

        if (enabler.trim().length() > 0) {
            pattern4 = enabler + "^" + trigger;
        } else {
            pattern4 = trigger;
        }
        PrintWriter writer = new PrintWriter(outFileName + ".query");
        
        
        
        
        if (pattern1.trim().length() > 0) {
            System.out.println("Pattern 1" + pattern1);
            writer.println("((" + pattern1 + ")<[10])" + "");
        }
        if (pattern2.trim().length() > 0) {
            System.out.println("Pattern 2" + pattern2);
            writer.println("" + "((" + pattern2 + ")<[10])" + "");
        }
        if (pattern3.trim().length() > 0) {
            System.out.println("Pattern 3" + pattern3);
            writer.println("" + "((" + pattern3 + ")<[10])" + "");
        }
        if (pattern4.trim().length() > 0) {
            System.out.println("Pattern 4" + pattern4);
            writer.println("" + "((" + pattern4 + ")<[10])" + "");
        }
        writer.close();
    }

    public static void main(String[] args) throws FileNotFoundException {
        QueryPatternGenerator gen = new QueryPatternGenerator();
        gen.generateQueryPattern(GlobalVariable.PROJECT_DIR + "/data/most_frequent.tsv");
    }
}
