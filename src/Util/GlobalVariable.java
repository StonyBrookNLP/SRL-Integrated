/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

/**
 *
 * @author samuellouvan
 */
public class GlobalVariable {
    public static String PROJECT_DIR =  System.getProperty("user.dir");
    public static int sourceIdxStart = -1; // domain adaptation 
    public static void main(String[] args)
    {
        System.out.println(PROJECT_DIR);
    }
    
    public static String[] argumentLabels = {"A0", "A1", "T", "A2"};
}
