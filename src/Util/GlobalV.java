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
public class GlobalV {

    public static String PROJECT_DIR = System.getProperty("user.dir");
    public static int sourceIdxStart = -1; // domain adaptation 

    public static void main(String[] args) {
        System.out.println(PROJECT_DIR);
    }

    public static final String[] labels = {"A0", "A1", "T", "A2"};
    public static final String[] roleName = {"undergoer", "enabler", "trigger", "result"};
    public static String A0 = "A0";
    public static String A1 = "A1";
    public static String T = "T";
    public static String A2 = "A2";
    public static String A3 = "A3";
    public static int AO_IDX = 0;
    public static int A1_IDX = 1;
    public static int T_IDX = 2;
    public static int A2_IDX = 3;

    public static int NB_ARG = 3;

}
