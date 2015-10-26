/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.ml;

import Util.GlobalV;
import Util.LibSVMUtil;
import Util.MateParserUtil;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import libsvm.svm;
import libsvm.svm_model;
import se.lth.cs.srl.Learn;

/**
 *
 * @author slouvan
 */
public class LibSVMWrapper {

    public static void doTrain(String trainingFileName, String modelFileName) throws NoSuchMethodException, IllegalAccessException {
        String[] params = new String[LibSVMUtil.TRAIN_ARGS.length];
        System.arraycopy(LibSVMUtil.TRAIN_ARGS, 0, params, 0, LibSVMUtil.TRAIN_ARGS.length);
        try {
            Method onLoaded = svm_train.class.getMethod("main", String[].class);
            params[params.length-2] = trainingFileName;
            params[params.length-1] = modelFileName;
            onLoaded.invoke(null, (Object) params);
        } catch (InvocationTargetException e) {
            System.out.println(e.getCause().toString());
        }
    }

    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, IOException {
        //new LibSVMWrapper().doTrain("/home/slouvan/Downloads/libsvm-3.20/result_training_data.vector", "/home/slouvan/Downloads/libsvm-3.20/model");
       svm_model model =  svm.svm_load_model( GlobalV.PROJECT_DIR + "/data/model" + "/" + "A0"+ ".model");
       double coeff[][] = model.sv_coef;
       
        System.out.println(model.l);
    }
}
