/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import Util.Constant;
import Util.SentenceUtil;
import Util.StdUtil;

import com.google.gson.Gson;
import edu.cmu.cs.lti.ark.fn.data.prep.AllAnnotationsMergingWithoutNE;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import qa.srl.SRLWrapper;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgProcessAnnotationData;
import sbu.srl.datastructure.JSONData;
import sbu.srl.datastructure.Sentence;

/**
 *
 * @author slouvan
 */
public class Test {

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        try {
            for (int i = 1; i <= 5; i++) {
                String outputDir = "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-12-01-2016-byprocess-fold";
                File testFoldDir = new File(outputDir.concat("/fold-").concat("" + i).concat("/test"));
                SBURolePredict.performPredictionEasySRL("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-12-01-2016-byprocess-fold/fold-"+i+"/test".concat("/test.arggold.ser"),
                        outputDir.concat("/fold-" + i).concat("/test/cv." + i + ".test.sentence.sbu"),
                        outputDir.concat("/fold-" + i).concat("/test/cv." + i + ".raw.predict.easysrl"),
                        "./data/modelCCG", outputDir.concat("/fold-" + i));

                ArrayList<Sentence> predictedSentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-12-01-2016-byprocess-fold/fold-"+i+"/test".concat("/test.argeasysrlpredict.ser"));
                Map<String, List<Sentence>> groupByProcess = predictedSentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));

                ArrayList<JSONData> jsonData = SentenceUtil.generateJSONData(groupByProcess);

                SentenceUtil.flushDataToJSON(jsonData, testFoldDir.getAbsolutePath().concat("/test.easysrlpredict.json"), true);
            }
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class Orang {

    private String nama;

    public Orang(String nama) {
        this.nama = nama;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    @Override
    public String toString() {
        return "Orang{" + "nama=" + nama + '}';
    }

}
