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
    public static String PROJECT_DIR = System.getProperty("user.dir");
    
    public static void main(String[] args)
    {
        System.out.println(PROJECT_DIR);
    }
}
