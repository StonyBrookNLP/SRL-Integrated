/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.ml;

import Util.LibLinearUtil;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import liblinear.FeatureNode;
import liblinear.Model;

/**
 *
 * @author slouvan
 */
public class LibLinearWrapper {

    public static void doTrain(String trainingFileName, String modelFileName) throws NoSuchMethodException, IllegalAccessException {
        String[] params = new String[LibLinearUtil.TRAIN_ARGS.length];
        System.arraycopy(LibLinearUtil.TRAIN_ARGS, 0, params, 0, LibLinearUtil.TRAIN_ARGS.length);
        try {
            Method onLoaded = liblinear.Train.class.getMethod("main", String[].class);
            params[params.length - 2] = trainingFileName;
            params[params.length - 1] = modelFileName;
            onLoaded.invoke(null, (Object) params);
            System.out.println("Training classifier");
        } catch (InvocationTargetException e) {
            System.out.println("Exception in training");
            System.out.println(e.getCause().toString());
        }
        System.out.println("Finished training classifier");
    }

    public static FeatureNode[] toFeatureNode(String rawVector, Model model) {
        List<FeatureNode> x = new ArrayList<FeatureNode>();
        StringTokenizer st = new StringTokenizer(rawVector, " \t\n");
        int n = 0;
        if (model.getBias() >= 0) {
            n = model.getNrFeature() + 1;
        } else {
            n = model.getNrFeature();
        }

        double target_label;
        try {
            String label = st.nextToken();
            target_label = svm_predict.atof(label);
        } catch (NoSuchElementException e) {
            throw new RuntimeException("Wrong input format");
        }

        while (st.hasMoreTokens()) {
            String[] split = Pattern.compile(":").split(st.nextToken(), 2);
            if (split == null || split.length < 2) {
                throw new RuntimeException("Wrong input format at line ");
            }

            try {
                int idx = svm_predict.atoi(split[0]);
                double val = svm_predict.atof(split[1]);

                // feature indices larger than those in training are not used
                if (idx <= model.getNrFeature()) {
                    FeatureNode node = new FeatureNode(idx, val);
                    x.add(node);
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Wrong input format at line ");
            }
        }

        if (model.getBias() >= 0) {
            FeatureNode node = new FeatureNode(n, model.getBias());
            x.add(node);
        }

        FeatureNode[] nodes = new FeatureNode[x.size()];
        nodes = x.toArray(nodes);
        return nodes;
    }

    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException {
        doTrain("./data/A0.vector", "./data/out");
    }
}
