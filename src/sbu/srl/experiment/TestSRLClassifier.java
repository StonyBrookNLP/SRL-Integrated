/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.experiment;

import Util.ClearParserUtil;
import Util.Constant;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import static libsvm.svm.svm_train;

import qa.StanfordDepParserSingleton;
import qa.srl.SRLWrapper;

/**
 *
 * @author slouvan
 */
public class TestSRLClassifier {

    public static void main(String[] args) throws IOException, FileNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // training file name, model name, srl type, domain adaptation
        //new SRLWrapper().doTrain("./data/trigger_training_data.conll09", "./data/out_model", Constant.SRL_MATE, false);
        // convert to CoNLL'09
        /*String CoNLL06Str = StanfordDepParserSingleton.getInstance().parseCoNLL("Evaporation is the process by which water changed into water vapor.");
        String CoNLL09Str = ClearParserUtil.fromConLL2006StrToCoNLL2009Str(CoNLL06Str);

        PrintWriter writer = new PrintWriter("./data/temp.conll09");
        writer.println(CoNLL09Str);
        writer.close();
        new SRLWrapper().doPredict("./data/trigger_testing_data.conll09", "./data/trigger_testing_data.conll09.predict",
                "./data/out_model", Constant.SRL_MATE, true, false);*/
        

    }
}
