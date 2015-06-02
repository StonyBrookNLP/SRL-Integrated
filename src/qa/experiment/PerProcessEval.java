/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author samuellouvan
 */
public class PerProcessEval {

    public static void processResult(String fileName, String outFileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName));
        ArrayList<String> rows = new ArrayList<String>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            rows.add(line);
        }

        String currentProcessName = rows.get(0).split("\t")[0].trim();
        int cnt = 0;
        double accPrec = 0.0;
        double accRecall = 0.0;
        double accF1 = 0.0;
        PrintWriter writer = new PrintWriter(outFileName);
        for (int i = 0; i < rows.size(); i++) {
            String data[] = rows.get(i).split("\t");
            System.out.println(rows.get(i));
            if (!data[0].equals(currentProcessName)) {

                writer.println(currentProcessName + "\t" + (accPrec / cnt) + "\t" + (accRecall / cnt) + "\t" + (accF1) / cnt);
                cnt = 0;
                accPrec = 0.0;
                accRecall = 0.0;
                accF1 = 0.0;
                currentProcessName = data[0].trim();
            }

            accPrec += Double.parseDouble(data[3]);
            accRecall += Double.parseDouble(data[4]);
            accF1 += Double.parseDouble(data[5]);
            cnt++;

        }
        writer.println(currentProcessName + "\t" + (accPrec / cnt) + "\t" + (accRecall / cnt) + "\t" + (accF1) / cnt);
        writer.close();
    }

    public static void main(String[] args) throws FileNotFoundException {
        //processResult("./data/experiment_results/A0-JointModel-CV-Process.txt", "./data/experiment_results/A0-JointModel-CV-Process-PROCESSED.txt");
        //processResult("./data/experiment_results/A1-JointModel-CV-Process.txt", "./data/experiment_results/A1-JointModel-CV-Process-PROCESSED.txt");
        processResult("./data/experiment_results/A2-JointModel-CV-Process.txt", "./data/experiment_results/A2-JointModel-CV-Process-PROCESSED.txt");

    }
}
