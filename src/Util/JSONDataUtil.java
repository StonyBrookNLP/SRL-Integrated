/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import sbu.srl.datastructure.JSONData;

/**
 *
 * @author slouvan
 */
public class JSONDataUtil {

    public static Set<String> getAllUniqueRoleLabelFromJSON(ArrayList<JSONData> data) {
        ArrayList<String> labels = new ArrayList<String>();
        for (JSONData item : data) {
            labels.addAll(item.getUniqueAnnotatedRoleLabelFromJSON());
        }

        Set<String> uniqueLabel = new HashSet<String>(labels);

        return uniqueLabel;
    }

    public static Set<String> getAllUniqueRoleLabelFromJSON(List<JSONData> data) {
        ArrayList<String> labels = new ArrayList<String>();
        for (JSONData item : data) {
            labels.addAll(item.getUniqueAnnotatedRoleLabelFromJSON());
        }

        Set<String> uniqueLabel = new HashSet<String>(labels);

        return uniqueLabel;
    }
}
