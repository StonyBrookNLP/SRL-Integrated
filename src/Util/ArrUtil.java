/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author samuellouvan
 */
public class ArrUtil {

    public static int getIdxMax(double[] arr) {
        double max = Double.MIN_VALUE;
        int idxMax = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                idxMax = i;
            }
        }

        return idxMax;
    }

    public static boolean isExistIntersect(ArrayList<String> str1, String[] str2) {
        for (String str : str2) {
            if (str1.contains(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIntersect(ArrayList<Integer> arr1, ArrayList<Integer> arr2) {
        for (Integer i : arr1) {
            if (arr2.contains(i)) {
                return true;
            }
        }

        return false;
    }

    public static ArrayList<Integer> intersection(List<Integer> list1, List<Integer> list2) {
        ArrayList<Integer> result = new ArrayList<Integer>(list1);

        result.retainAll(list2);

        return result;
    }
    
    public static void main(String[] args)
    {
        ArrayList<Integer> arr1 = new ArrayList<>();
        ArrayList<Integer> arr2 = new ArrayList<>();
        arr1.add(0);
        arr2.add(1);
        System.out.println(intersection(arr1,arr2));
    }
}
