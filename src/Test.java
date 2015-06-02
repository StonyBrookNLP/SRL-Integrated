/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author samuellouvan
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Test {

    public static void main(String[] args) throws FileNotFoundException {

        Scanner scanner = new Scanner(new File("./data/frequent.tsv"));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] fields = line.split("\t");
            System.out.println(fields.length);
        }
    }
}
