/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import Util.ConfigUtil;
import Util.SentenceUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.JSONData;
import sbu.srl.datastructure.Sentence;

/**
 *
 * @author slouvan
 */
public class KnowledgeViewer {

    private String jsonFileName;
    private String outputFileName;

    public KnowledgeViewer(String jsonFileName, String outputFileName) {
        this.jsonFileName = jsonFileName;
        this.outputFileName = outputFileName;
    }

    public void printData() throws IOException {
        ArrayList<JSONData> jsonData = SentenceUtil.readJSONData(jsonFileName, true);
        PrintWriter writer = new PrintWriter(outputFileName);
        int counter = 0;
        for (int i = 0; i < jsonData.size(); i++) {
            String processName = jsonData.get(i).getProcessName();
            ArrayList<Sentence> sentences = jsonData.get(i).getSentence();
            for (int j = 0; j < sentences.size(); j++) {

                Sentence currentSentence = sentences.get(j);
                ArrayList<ArgumentSpan> predictedArgumentSpan = currentSentence.getPredictedArgumentSpanJSON();
                for (int k = 0; k < predictedArgumentSpan.size(); k++) {
                    System.out.println(counter++);
                    ArgumentSpan currentArgumentSpan = predictedArgumentSpan.get(k);
                    StringBuilder str = new StringBuilder();
                    str.append(processName).append("\t").append(currentArgumentSpan.getTextJSON())
                            .append("\t").append(currentArgumentSpan.getRolePredicted()).append("\t")
                            .append(currentArgumentSpan.getMaxProbScore()).append("\t").append(currentSentence.getRawText());
                    writer.println(str.toString());
                }
            }
        }
        writer.close();
    }

    public void toQAFrame(String qaFileName) throws FileNotFoundException, IOException {
        // Read unique labels
        ArrayList<String> roles = ConfigUtil.getRoleLabels("/home/slouvan/NetBeansProjects/SRL-Integrated/configFrameFile/config.txt");
        ArrayList<JSONData> jsonData = SentenceUtil.readJSONData(jsonFileName, true);
        PrintWriter writer = new PrintWriter(qaFileName);
        writer.println("process\t" + String.join("\t", roles) + "\tsentence");
        int counter = 0;
        for (int i = 0; i < jsonData.size(); i++) {
            String processName = jsonData.get(i).getProcessName();
            ArrayList<Sentence> sentences = jsonData.get(i).getSentence();

            for (int j = 0; j < sentences.size(); j++) {
                Sentence currentSentence = sentences.get(j);
                StringBuilder lineStr = new StringBuilder();
                lineStr.append(processName).append("\t");
                ArrayList<ArgumentSpan> predictedArgumentSpan = currentSentence.getPredictedArgumentSpanJSON();
                String roleStr = "";
                for (String role : roles) {
                    List<ArgumentSpan> spans = predictedArgumentSpan.stream().filter(s -> s.getRolePredicted().equalsIgnoreCase(role)).collect(Collectors.toList());
                    List<String> spanText = spans.stream().map(ArgumentSpan::getTextJSON).collect(Collectors.toList());
                    if (spanText.size() > 0) {
                        roleStr = roleStr.concat(String.join("|", spanText)).concat("\t");
                    } else {
                        roleStr = roleStr.concat("\t");
                    }
                }
                lineStr.append(roleStr);
                lineStr.append(currentSentence.getRawText());
                writer.println(lineStr.toString());
            }

        }
        writer.close();
    }

    public static void main(String[] args) {
        try {
            KnowledgeViewer viewer = new KnowledgeViewer("/home/slouvan/NetBeansProjects/SRL-Integrated/data/knowledgeExtract/test/srlpredict.json",
                    "/home/slouvan/NetBeansProjects/SRL-Integrated/data/knowledgeExtract/test/extracted.tsv");
            //viewer.printData();
            viewer.toQAFrame("/home/slouvan/NetBeansProjects/SRL-Integrated/data/knowledgeExtract/test/KB.tsv");
        } catch (IOException ex) {
            Logger.getLogger(KnowledgeViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
