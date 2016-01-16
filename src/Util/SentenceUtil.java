/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.ArgumentSpanDeserializer;
import sbu.srl.datastructure.ArgumentSpanSerializer;
import sbu.srl.datastructure.ILPSRLDataDeserializer;
import sbu.srl.datastructure.ILPSRLDataSerializer;
import sbu.srl.datastructure.JSONData;
import sbu.srl.datastructure.Sentence;
import sbu.srl.datastructure.SentenceDeserializer;
import sbu.srl.datastructure.SentenceSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.stream.JsonReader;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult.Frame;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult.Frame.ScoredRoleAssignment;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult.Frame.Span;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.uw.easysrl.main.ParseResult;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import scala.actors.threadpool.Arrays;

/**
 *
 * @author slouvan
 */
public class SentenceUtil {

    public static int counter = 0;

    public static ArrayList<JSONData> generateJSONData(Map<String, List<Sentence>> mapByProcess) {
        ArrayList<JSONData> ilpDataArr = new ArrayList<>();
        for (String process : mapByProcess.keySet()) {
            JSONData ilpDataItem = new JSONData();
            ilpDataItem.setProcessName(process);
            ilpDataItem.setSentence((ArrayList<Sentence>) mapByProcess.get(process));
            ilpDataArr.add(ilpDataItem);
        }
        return ilpDataArr;
    }

    public static void flushDataToJSON(ArrayList<JSONData> predictionData, String fileName, boolean predict) throws FileNotFoundException {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(JSONData.class, new ILPSRLDataSerializer());
        gsonBuilder.registerTypeAdapter(Sentence.class, new SentenceSerializer(predict));
        gsonBuilder.registerTypeAdapter(ArgumentSpan.class, new ArgumentSpanSerializer());
        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();
        //final String json = gson.toJson(data);
        //gson.to
        String jsonString = gson.toJson(predictionData, new TypeToken<ArrayList<JSONData>>() {
        }.getType());
        PrintWriter writer = new PrintWriter(fileName);
        writer.println(jsonString);
        writer.close();
    }

    public static ArrayList<JSONData> readJSONData(String fileName, boolean isPrediction) throws FileNotFoundException, IOException {
        final GsonBuilder gsonObj = new GsonBuilder();
        gsonObj.registerTypeAdapter(JSONData.class, new ILPSRLDataDeserializer());
        gsonObj.registerTypeAdapter(Sentence.class, new SentenceDeserializer(isPrediction));
        gsonObj.registerTypeAdapter(ArgumentSpan.class, new ArgumentSpanDeserializer(isPrediction));

        final Gson gsonG = gsonObj.create();
        Reader reader = new InputStreamReader(new FileInputStream(fileName));
        ArrayList<JSONData> jsonDataArr = gsonG.fromJson(reader, new com.google.gson.reflect.TypeToken<ArrayList<JSONData>>() {
        }.getType());

        reader.close();
        return jsonDataArr;
    }

    public static List<SemaforParseResult> readSemaforJSONdata(String fileName) throws FileNotFoundException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<String> lines = Files.readAllLines(Paths.get(fileName));

        String jsonString = String.join(",", lines);
        jsonString = "[" + jsonString + "]";
        final SemaforParseResult[] parse = mapper.readValue(jsonString, SemaforParseResult[].class);
        return Arrays.asList(parse);

    }

    public static List<ParseResult> readEasySRLJSONdata(String fileName) throws FileNotFoundException {
        Gson gson = new Gson();
        Type listOfObject = new com.google.gson.reflect.TypeToken<List<ParseResult>>() {
        }.getType();

        String[] lines = FileUtil.readLinesFromFile(fileName);
        return gson.fromJson(String.join("\n", lines), listOfObject);
    }

    public static Pair<String, Double> getSEMAFORLabel(List<Frame.NamedSpanSet> spanSet, int startIdx, int endIdx) {
        double maxScore = Double.MIN_VALUE;
        String label = "NONE";
        for (int i = 0; i < spanSet.size(); i++) {
            Frame.NamedSpanSet currentFrameElmt = spanSet.get(i);
            List<Span> overlappedSpans = currentFrameElmt.spans.stream().filter(s -> s.start >= startIdx - 1 && s.start <= endIdx - 1).collect(toList());
            // iterate through the span, get the max score, return label
            for (Span span : overlappedSpans) {
                if (span.probScore > maxScore) {
                    maxScore = span.probScore;
                    label = currentFrameElmt.name;
                }
            }
        }

        return new Pair(label, maxScore);
        // handle NONE
    }

    public static Pair<String, Double> getEasySRLLabel(String text, int startIdx, int endIdx) {
        /*double maxScore = Double.MIN_VALUE;
         String label = "NONE";
         for (int i = 0; i < spanSet.size(); i++) {
         Frame.NamedSpanSet currentFrameElmt = spanSet.get(i);
         List<Span> overlappedSpans = currentFrameElmt.spans.stream().filter(s -> s.start >= startIdx - 1 && s.start <= endIdx - 1).collect(toList());
         // iterate through the span, get the max score, return label
         for (Span span : overlappedSpans) {
         if (span.probScore > maxScore) {
         maxScore = span.probScore;
         label = currentFrameElmt.name;
         }
         }
         }

         return new Pair(label, maxScore);
         // handle NONE*/
        return null;
    }

    public static void transformSemaforPrediction(String srlPredictionJSON, String rawSentenceSemafor, String semaforJSONPrediction, String outFile) throws FileNotFoundException, IOException {
        List<String> rawSentences = Arrays.asList(FileUtil.readLinesFromFile(rawSentenceSemafor));
        List<SemaforParseResult> semaforResults = readSemaforJSONdata(semaforJSONPrediction);
        List<JSONData> srlJsonData = SentenceUtil.readJSONData(srlPredictionJSON, false);
        List<JSONData> semaforJSONManipulated = (List<JSONData>) FileUtil.deepClone(srlJsonData);

        Set<String> labels = JSONDataUtil.getAllUniqueRoleLabelFromJSON(srlJsonData);
        labels.add("NONE");
        for (int i = 0; i < srlJsonData.size(); i++) {
            JSONData currentData = (JSONData) srlJsonData.get(i);
            JSONData currentSemData = (JSONData) semaforJSONManipulated.get(i);
            System.out.println(currentData.getProcessName());
            ArrayList<Sentence> sentencesInProcess = currentData.getSentence();
            ArrayList<Sentence> sentenceInProcessSemafor = currentSemData.getSentence();
            for (int j = 0; j < sentencesInProcess.size(); j++) {
                Sentence goldSentence = sentencesInProcess.get(j);
                Sentence semSentence = sentenceInProcessSemafor.get(j);
                System.out.println(goldSentence.getRawText());
                // Find corresponding prediction frame for this sentence in SEMAFOR
                int sentId = ArrUtil.getMatchIdx(rawSentences, goldSentence.getRawText());
                SemaforParseResult semaforPrediction = semaforResults.get(sentId);
                Frame targetFrame = null;
                List<Frame> targetFrames = semaforPrediction.frames.stream().filter(f -> f.target.name.equals(currentData.getProcessName())).collect(toList());
                targetFrame = (targetFrames != null && targetFrames.size() > 0) ? targetFrames.get(0) : null;
                System.out.println(sentId);
                System.out.println(goldSentence.getRawText());

                if (targetFrame != null) {
                    ScoredRoleAssignment roleAssignments = targetFrame.annotationSets.get(0);
                    List<Frame.NamedSpanSet> spanSet = roleAssignments.frameElements;
                    ArrayList<ArgumentSpan> spans = goldSentence.getPredictedArgumentSpanJSON();
                    ArrayList<ArgumentSpan> semSpans = semSentence.getPredictedArgumentSpanJSON();
                    for (int k = 0; k < spans.size(); k++) {
                        ArgumentSpan span = spans.get(k);
                        ArgumentSpan semSpan = semSpans.get(k);

                        // Collect all the predictions from SEMAFOR where startIdx and EndIdx intersects
                        int startIdx = span.getStartIdxJSON();
                        int endIdx = span.getEndIdxJSON();
                        Pair<String, Double> labelScorePair = getSEMAFORLabel(spanSet, startIdx, endIdx);
                        System.out.println("Text : " + span.getTextJSON() + " ROLE SEMAFOR : " + labelScorePair.first + " ROLE SRL : " + span.getRolePredicted());
                        semSpan.setRolePredicted(labelScorePair.first); // change role predicted to semafor's label
                        HashMap<String, Double> probPair = semSpan.getRoleProbPair();
                        System.out.println("Size before : " + probPair.size());
                        probPair.put(labelScorePair.first, labelScorePair.second);
                        for (String role : labels) {
                            if (!role.equalsIgnoreCase(labelScorePair.first)) {
                                probPair.remove(role);
                            }
                        }
                        System.out.println("Size : " + probPair.size());
                    }
                } else {
                    System.out.println(counter++);
                    ArrayList<ArgumentSpan> spans = goldSentence.getPredictedArgumentSpanJSON();
                    ArrayList<ArgumentSpan> semSpans = semSentence.getPredictedArgumentSpanJSON();
                    for (int k = 0; k < spans.size(); k++) {
                        ArgumentSpan span = spans.get(k);
                        ArgumentSpan semSpan = semSpans.get(k);

                        // Collect all the predictions from SEMAFOR where startIdx and EndIdx intersects
                        int startIdx = span.getStartIdxJSON();
                        int endIdx = span.getEndIdxJSON();
                        //Pair<String, Double> labelScorePair = getSEMAFORLabel(spanSet, startIdx, endIdx);
                        //System.out.println("Text : " + span.getTextJSON() + " ROLE SEMAFOR : " + labelScorePair.first + " ROLE SRL : " + span.getRolePredicted());
                        semSpan.setRolePredicted("NONE"); // change role predicted to semafor's label
                        HashMap<String, Double> probPair = semSpan.getRoleProbPair();
                        System.out.println("Size before : " + probPair.size());
                        probPair.put("NONE", 1.0);
                        for (String role : labels) {
                            if (!role.equalsIgnoreCase("NONE")) {
                                probPair.remove(role);
                            }
                        }
                        System.out.println("Size : " + probPair.size());
                    }

                }
            }
        }
        flushDataToJSON(new ArrayList<JSONData>(semaforJSONManipulated), outFile, true);
        // FLUSH
    }

    public static void transformEasySrlPrediction(String srlPredictionJSON, String rawSentenceEasySRL, String easySrlJSONPrediction, String outFile) throws FileNotFoundException, IOException {
        List<String> rawSentences = Arrays.asList(FileUtil.readLinesFromFile(rawSentenceEasySRL));
        List<ParseResult> easySrlResults = readEasySRLJSONdata(easySrlJSONPrediction);
        List<JSONData> srlJsonData = SentenceUtil.readJSONData(srlPredictionJSON, false);
        List<JSONData> easySrlJSONManipulated = (List<JSONData>) FileUtil.deepClone(srlJsonData);

        Set<String> labels = JSONDataUtil.getAllUniqueRoleLabelFromJSON(srlJsonData);
        labels.add("NONE");
        for (int i = 0; i < srlJsonData.size(); i++) {
            JSONData currentData = (JSONData) srlJsonData.get(i);
            JSONData currentSemData = (JSONData) easySrlJSONManipulated.get(i);
            System.out.println(currentData.getProcessName());
            ArrayList<Sentence> sentencesInProcess = currentData.getSentence();
            ArrayList<Sentence> sentenceInProcessSemafor = currentSemData.getSentence();
            for (int j = 0; j < sentencesInProcess.size(); j++) {
                Sentence goldSentence = sentencesInProcess.get(j);
                Sentence semSentence = sentenceInProcessSemafor.get(j);
                System.out.println(goldSentence.getRawText());

                int sentId = ArrUtil.getMatchIdx(rawSentences, goldSentence.getRawText());
                ParseResult easySrlPrediction = easySrlResults.get(sentId);
 
                if (easySrlPrediction.getParseScore() != -1.0) {
                    ArrayList<ArgumentSpan> spans = goldSentence.getPredictedArgumentSpanJSON();
                    ArrayList<ArgumentSpan> semSpans = semSentence.getPredictedArgumentSpanJSON();
                    for (int k = 0; k < spans.size(); k++) {
                        ArgumentSpan span = spans.get(k);
                        ArgumentSpan semSpan = semSpans.get(k);
                        
                        // Collect all the predictions from SEMAFOR where startIdx and EndIdx intersects
                        int startIdx = span.getStartIdxJSON();
                        int endIdx = span.getEndIdxJSON();
                        String text = span.getTextJSON();
                        Pair<String, Double> labelScorePair = getEasySRLLabel(text, startIdx, endIdx);
                        System.out.println("Text : " + span.getTextJSON() + " ROLE EASYSRL : " + labelScorePair.first + " ROLE SRL : " + span.getRolePredicted());
                        semSpan.setRolePredicted(labelScorePair.first); // change role predicted to semafor's label
                        HashMap<String, Double> probPair = semSpan.getRoleProbPair();
                        System.out.println("Size before : " + probPair.size());
                        probPair.put(labelScorePair.first, labelScorePair.second);
                        for (String role : labels) {
                            if (!role.equalsIgnoreCase(labelScorePair.first)) {
                                probPair.remove(role);
                            }
                        }
                        System.out.println("Size : " + probPair.size());
                    }
                } else {
                    System.out.println(counter++);
                    ArrayList<ArgumentSpan> spans = goldSentence.getPredictedArgumentSpanJSON();
                    ArrayList<ArgumentSpan> semSpans = semSentence.getPredictedArgumentSpanJSON();
                    for (int k = 0; k < spans.size(); k++) {
                        ArgumentSpan span = spans.get(k);
                        ArgumentSpan semSpan = semSpans.get(k);

                        // Collect all the predictions from SEMAFOR where startIdx and EndIdx intersects
                        int startIdx = span.getStartIdxJSON();
                        int endIdx = span.getEndIdxJSON();
                        //Pair<String, Double> labelScorePair = getSEMAFORLabel(spanSet, startIdx, endIdx);
                        //System.out.println("Text : " + span.getTextJSON() + " ROLE SEMAFOR : " + labelScorePair.first + " ROLE SRL : " + span.getRolePredicted());
                        semSpan.setRolePredicted("NONE"); // change role predicted to semafor's label
                        HashMap<String, Double> probPair = semSpan.getRoleProbPair();
                        System.out.println("Size before : " + probPair.size());
                        probPair.put("NONE", 1.0);
                        for (String role : labels) {
                            if (!role.equalsIgnoreCase("NONE")) {
                                probPair.remove(role);
                            }
                        }
                        System.out.println("Size : " + probPair.size());
                    }
                }
            }
        }
        flushDataToJSON(new ArrayList<JSONData>(easySrlJSONManipulated), outFile, true);
        // FLUSH
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        /*String crossValDir = "/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-08-01-2016-byprocess-fold-process";
         for (int i = 1; i <= 5; i++) {
         transformSemaforPrediction(crossValDir+"/fold-"+i+"/test/test.srlout.json",
         crossValDir+"/fold-"+i+"/test/cv."+i+".test.sentence.sbu",
         crossValDir+"/fold-"+i+"/test/semaforOut",
         crossValDir+"/fold-"+i+"/test/test.semaforpredict.json");
         System.out.println("COUNTER:"+SentenceUtil.counter);
         }*/
        //List<SemaforParseResult>  results = readSemaforJSONdata("/home/slouvan/NetBeansProjects/semafor/output.txt");

        List<ParseResult> results = readEasySRLJSONdata("/home/slouvan/NetBeansProjects/EasySRL/easySrlOut");
        for (ParseResult result : results) {
            System.out.println(result);
        }

    }
}
