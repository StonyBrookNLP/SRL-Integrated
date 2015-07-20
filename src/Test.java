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
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import qa.util.FileUtil;

public class Test {

    public static void main(String[] args) throws FileNotFoundException {
       /* String [] arr = {"A0", "A2", "A1"};
        Arrays.sort(arr);
        System.out.println(Arrays.toString(arr));*/
        FileUtil.fromConll2009ToClearParserFormat("./data/gs.txt", "./data/gs_converted.txt");
        FileUtil.fromConll2009ToClearParserFormat("./data/srl.txt", "./data/srl_converted.txt");
    }
}
