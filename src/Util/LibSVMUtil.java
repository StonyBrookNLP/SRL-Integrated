/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import sbu.srl.ml.svm_predict;
import libsvm.svm;
import libsvm.svm_node;

/**
 *
 * @author slouvan
 */
public class LibSVMUtil {

    public static String[] TRAIN_ARGS = {"-s",
        "0", // TRAINING FILE INPUT
        "-t",// MODEL NAME
        "0",
        "-c",
        "1",
        "-g",
        "0.1",
        "", // training data
        "" // modelName
};

    public static String sortIndex(String str) {
        String featStr = str.trim();
        String[] idxValPair = featStr.split("\\s+");
        ArrayList<Feature> featureIdxPairArr = new ArrayList<Feature>();
        for (int i = 0; i < idxValPair.length; i++) {
            String[] featIdxvaluePair = idxValPair[i].split(":");
            featureIdxPairArr.add(new Feature(Integer.parseInt(featIdxvaluePair[0]), featIdxvaluePair[1]));
        }
        Collections.sort(featureIdxPairArr);

        StringBuilder sortedFeatStr = new StringBuilder();
        for (int i = 0; i < featureIdxPairArr.size(); i++) {
            sortedFeatStr.append(featureIdxPairArr.get(i).idx).append(":").append(featureIdxPairArr.get(i).val).append(" ");
        }
        return sortedFeatStr.toString().trim();
    }

    public static svm_node[] toSVMNode(String rawVector) {
        StringTokenizer st = new StringTokenizer(rawVector, " \t\n\r\f:");

        double target = svm_predict.atof(st.nextToken());
        int m = st.countTokens() / 2;
        svm_node[] x = new svm_node[m];
        for (int j = 0; j < m; j++) {
            x[j] = new svm_node();
            x[j].index = svm_predict.atoi(st.nextToken());
            x[j].value = svm_predict.atof(st.nextToken());
        }
        return x;
    }

    public static void main(String[] args) {
        System.out.println(sortIndex(" 1:1    10:0          3:0 "));

    }
}

class Feature implements Comparable {

    int idx;
    Object val;

    public Feature(int idx, Object val) {
        this.idx = idx;
        this.val = val;
    }

    @Override
    public int compareTo(Object o) {
        Feature otherFeature = (Feature) o;
        if (idx < otherFeature.idx) {
            return -1;
        }
        if (idx > otherFeature.idx) {
            return 1;
        }
        return 0;
    }

}
