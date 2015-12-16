/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.Sentence;

/**
 *
 * @author slouvan
 */
public class SRLFeatureExtractor implements Serializable{

    HashMap<String, Sentence> sentsLabeledPropBank;

    public SRLFeatureExtractor(HashMap<String, Sentence> sentsLabeledPropBank) {
        this.sentsLabeledPropBank = sentsLabeledPropBank;
    }

    public ArrayList<String> generateFeature(String text) {
        ArrayList<String> features = new ArrayList<String>();
        if (sentsLabeledPropBank.get(text) != null) {
            Sentence sent = sentsLabeledPropBank.get(text);
            HashMap<String, ArrayList<ArgumentSpan>> roleSpanPair = sent.getRoleArgPropBank();
            if (roleSpanPair != null) {
                for (String roleLabel : roleSpanPair.keySet()) {
                    ArrayList<ArgumentSpan> spans = roleSpanPair.get(roleLabel);
                    for (ArgumentSpan span : spans) {
                        String feature = "";
                        feature = span.pred.trim();
                        feature = feature + "_" + roleLabel.trim();
                        features.add(feature);
                    }
                }
            }
        }
        return features;
    }

    public ArrayList<String> extractFeature(String text, int headID) {
        ArrayList<String> features = new ArrayList<String>();
        if (sentsLabeledPropBank.get(text) != null) {
            Sentence sent = sentsLabeledPropBank.get(text);
            HashMap<String, ArrayList<ArgumentSpan>> roleSpanPair = sent.getRoleArgPropBank();
            if (roleSpanPair != null) {
                for (String roleLabel : roleSpanPair.keySet()) {
                    ArrayList<ArgumentSpan> spans = roleSpanPair.get(roleLabel);
                    for (ArgumentSpan span : spans) {
                        ArrayList<Integer> idxs = span.getRoleIdx();
                        if (idxs.contains(headID)) {
                            String feature = "";
                            feature = span.pred.trim();
                            feature = feature + "_" + roleLabel.trim();
                            features.add(feature);
                        }
                    }
                }
            }
        }
        return features;
    }
}
