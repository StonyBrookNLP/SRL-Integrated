/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author samuellouvan
 */
import Util.ClearParserUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import qa.StanfordDepParserSingleton;
import qa.util.FileUtil;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.io.AllCoNLL09Reader;

public class Test {

    public static void main(String[] args) throws FileNotFoundException, IOException {
       /* String [] arr = {"A0", "A2", "A1"};
        Arrays.sort(arr);
        System.out.println(Arrays.toString(arr));*/
       // FileUtil.fromConll2009ToClearParserFormat("./data/gs.txt", "./data/gs_converted.txt");
        //FileUtil.fromConll2009ToClearParserFormat("./data/srl.txt", "./data/srl_converted.txt");
        //String [] ar;
        //System.out.println(ar);
        
        // AllCoNLL09Reader reader = new AllCoNLL09Reader(new File("./data/srlPredict.srl"));
        // reader.readAll();
        
        String CoNLL06Str = StanfordDepParserSingleton.getInstance().parseCoNLL("Water evaporates into water vapor");
        String CoNLL09Str = ClearParserUtil.fromConLL2006StrToCoNLL2009Str(CoNLL06Str);
        System.out.println(CoNLL09Str);
        
        PrintWriter writer = new PrintWriter("temp.conll09");
        writer.println(CoNLL09Str);
        writer.close();
        
        AllCoNLL09Reader reader = new AllCoNLL09Reader(new File("temp.conll09"));
        List<Sentence> sentences = reader.readAll();
        for (int i = 0; i < sentences.size(); i++)
        {
            System.out.println(sentences.get(i).toString());
        }
    }
}
