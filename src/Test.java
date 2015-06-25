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
        String str = "As. Tiny drops of liquid form clouds in this process called";
        String [] strs = str.split("\\.");
        System.out.println(strs.length);
    }
}
