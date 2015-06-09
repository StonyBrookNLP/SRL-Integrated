/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.distantsupervision;

import Util.FunctionWords;
import Util.GlobalVariable;
import Util.StringUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
            List<String> words = FunctionWords.FUNCTION_WORDS.asList();
            String newString = "";
            String tokens[] = str.split("\\|")[0].split("\\s+");
            for (int i = 0; i < tokens.length; i++) {
                if (!words.contains(tokens[i].toLowerCase())) {
                    newString = newString.concat(" " + tokens[i].toLowerCase());
                }
            }

            if (newString.length() > 0) {
                return "\"" + newString.trim() + "\"";
            }

        }
        return "";
    }

    public void constructQueryWords(String[] fields, String outFileName) throws FileNotFoundException {
        //System.out.println(fields[0].toUpperCase());
        String underGoer = cleanString(fields[1]);
        String enabler = cleanString(fields[2]);
        String trigger = cleanString(fields[3]);
        String result = cleanString(fields[4]);
        ArrayList<String> queries = new ArrayList<String>();
        String pattern5 = "";

        if (trigger.length() <= 0) // If trigger is empty
        {
            trigger = "\"" + fields[0].trim() + "\"";
            pattern5 = trigger;
            queries.add(pattern5);
        } else {
            pattern5 = trigger;
            queries.add(pattern5);

        }
        // Pattern 1, trigger, undergoer
        String pattern1 = "";
        if (underGoer.trim().length() != 0 && trigger.trim().length() != 0) {
            pattern1 = underGoer + "^" + trigger;
        }
        if (!queries.contains(pattern1))
            queries.add(pattern1);
        // Pattern 2, trigger, result
        String pattern2 = "";
        if (result.trim().length() != 0 && trigger.trim().length() != 0) {
            pattern2 = result + "^" + trigger;
        }
        if (!queries.contains(pattern2))
            queries.add(pattern2);
        // Pattern 3, trigger, undergoer, result
        String pattern3 = "";
        if (underGoer.trim().length() != 0 && result.trim().length() != 0 && trigger.trim().length() != 0) {
            pattern3 = underGoer + "^" + trigger + "^" + result;
        }
        if (!queries.contains(pattern3))
            queries.add(pattern3);
        // Pattern 4, trigger, enabler
        String pattern4 = "";

        if (enabler.trim().length() > 0 && trigger.length() > 0) {
            pattern4 = enabler + "^" + trigger;
        } else {
            pattern4 = trigger;
        }
        if (!queries.contains(pattern4))
            queries.add(pattern4);
        PrintWriter writer = new PrintWriter(outFileName + ".query");
        System.out.println(outFileName + ".query");
        for (int i = 0; i <  queries.size(); i++)
        {
            if (queries.get(i).trim().length() > 0){
                String query = "((" + queries.get(i) + ")<[10])" + "";
                System.out.println(queries.get(i));
                writer.println(query);
            }
        }
        /*if (pattern1.trim().length() > 0) {
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
        if (pattern5.trim().length() > 0) {
            System.out.println("Pattern 5" + pattern5);
            writer.println("" + "((" + pattern4 + ")<[10])" + "");
        }*/
        writer.close();
    }

    public static void main(String[] args) throws FileNotFoundException {
        QueryPatternGenerator gen = new QueryPatternGenerator();
        gen.generateQueryPattern(GlobalVariable.PROJECT_DIR + "/data/most_frequent_7_june.tsv");
    }
}
