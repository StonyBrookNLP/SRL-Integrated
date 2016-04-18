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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import qa.StanfordDepParser;
import qa.StanfordDepParserSingleton;
import qa.dep.DependencyTree;
import qa.srl.SRLWrapper;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgProcessAnnotationData;
import sbu.srl.datastructure.JSONData;
import sbu.srl.datastructure.Sentence;
import scala.actors.threadpool.Arrays;

/**
 *
 * @author slouvan
 */
public class Test {

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        try {
            /*try {
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
            }*/
            
            //SpockDataReader reader = new SpockDataReader("/home/slouvan/NetBeansProjects/SRL-Integrated/data/training_4_roles.tsv",
            //        "/home/slouvan/NetBeansProjects/SRL-Integrated/configFrameFile/config.txt", false);
            //reader.readData();
            ArrayList<Sentence> sentences = (ArrayList<Sentence>)FileUtil.deserializeFromFile("./data/training_4_roles.ser");
            
            //FileUtil.serializeToFile(sentences, "./data/training_4_roles.ser");
            //PrintWriter writer = new PrintWriter("sentences.temp");
            FrameNetFeatureExtractor fNetExtractor = new FrameNetFeatureExtractor();
            HashMap<String, List<String>> lemmaVerbFramePair = new HashMap<String,List<String>>();
            HashMap<String, DependencyTree> sentDepTreePair = new HashMap<String,DependencyTree>();
            int cnt = 0 ;
            for (Sentence sentence : sentences) {
                System.out.println("Sent : "+(cnt++));
                sentDepTreePair.put(sentence.getRawText(), StanfordDepParserSingleton.getInstance().parse(sentence.getRawText()));
               /*System.out.println("Sent : "+(cnt++));
               DependencyTree tree = sentence.getDepTree();
               for (int i = tree.firstKey(); i <= tree.lastKey() ;i++)
               {
                    String lemmaVerb = tree.getLemmaVerb(false, 3, tree.get(i).getId());
                    String[] framesInvoked = fNetExtractor.getFrame(lemmaVerb + ".v");
                    if (!lemmaVerb.isEmpty() && framesInvoked != null && framesInvoked.length > 0) {
                        lemmaVerbFramePair.put(lemmaVerb, Arrays.asList(framesInvoked));
                    }
               }*/
            }
            FileUtil.serializeToFile(sentDepTreePair, "./data/depTree.ser");
            int x = 0;
            
            //new SRLWrapper().doPredict("sentences.temp", "sentences.args.temp", "./data/modelCCG", Constant.SRL_CCG, true, false);
        } catch (IllegalArgumentException ex) {
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
