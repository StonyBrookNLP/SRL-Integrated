/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @author slouvan
 */
public class ConfigUtil {
    
    public static ArrayList<String> getRoleLabels(String fileName) throws FileNotFoundException
    {
        Scanner scanner = new Scanner(new File(fileName));
        ArrayList<String> roleLabels = new ArrayList<String>();
        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            if (line.contains("role"))
            {
                String [] fields = line.split("\t");
                String [] roles = fields[1].split(":");
                roleLabels = new ArrayList(Arrays.asList(roles));
            }
        }
        
        scanner.close();
        
        return roleLabels;
    }
    
    public static void main(String[] args) throws FileNotFoundException
    {
        System.out.println(getRoleLabels("/home/slouvan/NetBeansProjects/SRL-Integrated/configFrameFile/config.txt"));
    }
}
