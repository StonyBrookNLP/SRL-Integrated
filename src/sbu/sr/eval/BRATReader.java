/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.sr.eval;

import edu.stanford.nlp.ling.CoreLabel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.io.FileUtils;
import qa.StanfordTokenizerSingleton;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.Sentence;

/**
 *
 * @author slouvan
 */
// INPUT    : BRAT files that contain a sentence and the annotation
// OUTPUT   : Sentence and argument spans
public class BRATReader {

    private String rawText;
    private String[] rawAnnotation;

    public BRATReader(String sentenceFileName, String annotationFileName) throws FileNotFoundException {
        rawText = FileUtil.readLinesFromFile(sentenceFileName)[0];
        rawAnnotation = FileUtil.readLinesFromFile(annotationFileName);
    }

    public BRATReader(String dirName) {

    }

    public Sentence getAnnotatedSentence() throws IOException {
        Sentence sent = new Sentence(rawText);
        HashMap<String, ArrayList<ArgumentSpan>> roleSpanMap = new HashMap<String, ArrayList<ArgumentSpan>>();
        // determine all role names
        for (int i = 0; i < rawAnnotation.length; i++) {
            String roleName = rawAnnotation[i].split("\t")[1];
            if (!rawAnnotation[i].startsWith("#")) {
                int startIndex = Integer.parseInt(rawAnnotation[i].split("\t")[1].split("\\s+")[1]);
                int endIndex = Integer.parseInt(rawAnnotation[i].split("\t")[1].split("\\s+")[2]);
                ArrayList<DependencyNode> argumentSpanNode = new ArrayList<DependencyNode>();

                int countIndex = 0;
                DependencyTree tree = sent.getDepTree();
                List<CoreLabel> tokenLabel = StanfordTokenizerSingleton.getInstance().getTokenLabel(rawText);
                for (int j = 1; j < tree.size(); j++) {
                    DependencyNode node = tree.get(j);
                    CoreLabel token = tokenLabel.get(j - 1);
                    if (token.beginPosition() >= startIndex && token.endPosition() <= endIndex) {
                        argumentSpanNode.add(node);
                    }
                }
                // Create argument span here
                ArgumentSpan argSpan = new ArgumentSpan(argumentSpanNode, roleName);
                if (roleSpanMap.get(roleName) == null) {
                    ArrayList<ArgumentSpan> arrArgSpan = new ArrayList<ArgumentSpan>();
                    arrArgSpan.add(argSpan);
                    roleSpanMap.put(roleName, arrArgSpan);
                } else {
                    ArrayList<ArgumentSpan> arrArgSpan = roleSpanMap.get(roleName);
                    arrArgSpan.add(argSpan);
                    roleSpanMap.put(roleName, arrArgSpan);
                }
            }
        }
//        sent.setRoleArgMap(roleSpanMap);
        return sent;
    }

    public ArrayList<Sentence> getSentencesFromDir(String dirName, String extension) throws FileNotFoundException, IOException {
        List<File> files = (List<File>) FileUtils.listFiles(new File(dirName), new String[]{extension}, true);
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();
        for (File f : files) {
            System.out.println("Processing " + f.getName());
            rawText = FileUtil.readLinesFromFile(f.getAbsolutePath())[0];
            rawAnnotation = FileUtil.readLinesFromFile(f.getAbsolutePath().replaceAll(".txt", ".ann"));
            sentences.add(getAnnotatedSentence());
        }

        return sentences;
    }

    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        //BRATReader bratReader = new BRATReader("/home/slouvan/brat-v1.3_Crunchy_Frog/data/QA/question104.txt", "/home/slouvan/brat-v1.3_Crunchy_Frog/data/QA/question104.ann");
        //bratReader.getAnnotatedSentence();
        BRATReader bratReader = new BRATReader("");
        bratReader.getSentencesFromDir("/home/slouvan/brat-v1.3_Crunchy_Frog/data/QA","txt");

    }
}
