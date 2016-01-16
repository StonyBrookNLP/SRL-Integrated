/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author samuellouvan
 */
public class StdUtil {

    public static void printOutput(Process p) throws IOException {
        String s = "";
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

        System.out.println("Here is the standard output of the command:\n");

        while ((s = stdInput.readLine()) != null) {
            //if (s.contains("A0") || s.contains("A1") || s.contains("A2")) {
                System.out.println(s);

            //}
        }
    }

    public static String getOutput(Process p) throws IOException {
        String s = "";
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

        System.out.println("Here is the standard output of the command:\n");
        String results = "";
        while ((s = stdInput.readLine()) != null) {
            if (s.contains("A0") || s.contains("A1") || s.contains("A2")) {
                System.out.println(s);
                results += s;

            }
        }
        return results;
    }
    
    public static String getOutput(String processName, int fold,Process p) throws IOException {
        String s = "";
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

        System.out.println("Here is the standard output of the command:\n");
        String results = "";
        while ((s = stdInput.readLine()) != null) {
            if (s.contains("A0") || s.contains("A1") || s.contains("A2")) {
                System.out.println(s);
                results += processName+" "+fold+" "+s+"\n";

            }
        }
        return results;
    }

    public static String getRawOutput(Process p) throws IOException
    {
        String s = "";
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

        System.out.println("Here is the standard output of the command:\n");
        String results = "";
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }
        return results;
    }
    
    public static double getPredictionConfidenceScore(Process p) throws IOException
    {
        String s = "";
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

        System.out.println("Here is the standard output of the command:\n");
        double confidenceScore = 0;
        while ((s = stdInput.readLine()) != null) {
            if (s.contains("Confidence score"))
            {
                confidenceScore = Double.parseDouble(s.split(":")[1]);
            }
        }
        return confidenceScore;
    }
    
    public static void printError(Process p) throws IOException {

        String s = "";
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }
    }
}
