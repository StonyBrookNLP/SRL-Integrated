/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import org.apache.commons.lang3.StringUtils;
import sbu.srl.datastructure.ArgProcessAnnotationData;
import sbu.srl.datastructure.ArgProcessAnnotationDataSerializer;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.ArgumentSpanSerializer;
import sbu.srl.datastructure.JSONData;
import sbu.srl.datastructure.ILPSRLDataSerializer;
import sbu.srl.datastructure.Sentence;
import sbu.srl.datastructure.SentenceSerializer;
import sbu.srl.rolextract.SpockDataReader;

/**
 *
 * @author slouvan
 */
public class ArgProcessAnnotationDataUtil {
/*
    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public static ArrayList<String> getUniqueSentence(ArrayList<ArgProcessAnnotationData> arr) {
        ArrayList<String> sentences = new ArrayList<String>();

        List<ArgProcessAnnotationData> s = arr.stream().filter(distinctByKey(data -> data.getRawText())).collect(toList());
        for (int i = 0; i < s.size(); i++) {
            sentences.add(s.get(i).getRawText());
        }
        return sentences;
    }

    public static ArrayList<ArgProcessAnnotationData> getUniqueProcessSentencePair(ArrayList<ArgProcessAnnotationData> arr) throws IOException {
        List<ArgProcessAnnotationData> unique = arr.stream().filter(distinctByKey(data -> data.getKey())).collect(toList());
        List<String> keys = unique.stream().map(ArgProcessAnnotationData::getKey).collect(toList());
        ArrayList<ArgProcessAnnotationData> aggregated = new ArrayList<ArgProcessAnnotationData>();

        int sentId = 1;
        for (String key : keys) {
            ArrayList<ArgProcessAnnotationData> matchedKeyData = (ArrayList<ArgProcessAnnotationData>) arr.stream().filter(data -> data.getKey().equalsIgnoreCase(key)).collect(toList());
            Sentence sentence = new Sentence(matchedKeyData.get(0).getRawText());
            sentence.setId(sentId++);
            sentence.setProcess(matchedKeyData.get(0).getProcessName());
            sentence.setRawText(matchedKeyData.get(0).getRawText());
            HashMap<String, ArrayList<ArgumentSpan>> roleSpanPair = aggregateArgumentSpan(matchedKeyData);
            sentence.setRoleArgMap(roleSpanPair);
            ArgProcessAnnotationData data = new ArgProcessAnnotationData();
            data.setProcessName(matchedKeyData.get(0).getProcessName());
            data.setSentence(sentence);
            aggregated.add(data);
        }

        return aggregated;
    }

    public static HashMap<String, ArrayList<ArgumentSpan>> getRoleLabels(ArrayList<ArgProcessAnnotationData> arr) {
        List<Sentence> sentences = arr.stream().map(ArgProcessAnnotationData::getSentence).collect(toList());
        HashMap<String, ArrayList<ArgumentSpan>> roleSpanPair = new HashMap<String, ArrayList<ArgumentSpan>>();
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sent = sentences.get(i);
            ArrayList<String> labels = sent.getAllUniqueRoleLabel();
            for (String roleName : labels) {
                roleSpanPair.put(roleName, new ArrayList<ArgumentSpan>());
            }
        }
        return roleSpanPair;
    }

    public static HashMap<String, ArrayList<ArgumentSpan>> aggregateArgumentSpan(ArrayList<ArgProcessAnnotationData> arr) {
        HashMap<String, ArrayList<ArgumentSpan>> roleArgSpanPair = new HashMap<String, ArrayList<ArgumentSpan>>();
        roleArgSpanPair = getRoleLabels(arr);
        int argID = 1;
        for (int i = 0; i < arr.size(); i++) {
            Sentence currentSentence = arr.get(i).getSentence();

            for (String roleName : roleArgSpanPair.keySet()) {
                ArrayList<ArgumentSpan> roleArgSpans = currentSentence.getArgumentSpan(roleName);
                if (roleArgSpans.size() > 0) {
                    ArrayList<ArgumentSpan> temp = roleArgSpanPair.get(roleName);
                    for (int j = 0; j < roleArgSpans.size(); j++) {
                        if (roleArgSpans.get(j).getAnnotatedLabel().equalsIgnoreCase("1")) {
                            ArgumentSpan sp = roleArgSpans.get(j);
                            sp.setId(argID++);
                            temp.add(sp);
                        }
                    }
                    roleArgSpanPair.put(roleName, temp);
                }
            }
        }

        return roleArgSpanPair;
    }

    public static ArrayList<ILPSRLData> generateILPData(Map<String, List<ArgProcessAnnotationData>> mapByProcess) {
        ArrayList<ILPSRLData> ilpDataArr = new ArrayList<>();
        for (String process : mapByProcess.keySet()) {
            ILPSRLData ilpDataItem = new ILPSRLData();
            ilpDataItem.setProcessName(process);
            ArrayList<Sentence> sentences = new ArrayList<Sentence>();
            for (ArgProcessAnnotationData processData : mapByProcess.get(process)) {
                sentences.add(processData.getSentence());
            }
            ilpDataItem.setSentence(sentences);
            ilpDataArr.add(ilpDataItem);
        }
        return ilpDataArr;
    }

    public static void generateJSONILPData(ArrayList<ILPSRLData> ilpData, String fileName) throws FileNotFoundException {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ILPSRLData.class, new ILPSRLDataSerializer());
        gsonBuilder.registerTypeAdapter(Sentence.class, new SentenceSerializer());
        gsonBuilder.registerTypeAdapter(ArgumentSpan.class, new ArgumentSpanSerializer());
        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();
                //final String json = gson.toJson(data);
        //gson.to
        String jsonString = gson.toJson(ilpData, new TypeToken<ArrayList<ILPSRLData>>() {
        }.getType());
        PrintWriter writer = new PrintWriter(fileName);
        writer.println(jsonString);
        writer.close();
    }

    // FOR ONE ARGUMENT SPAN ONLY

    public static void dumpRolePredictionToFile(ArrayList<ArgProcessAnnotationData> arr, String confFileName, String fileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        SpockDataReader reader = new SpockDataReader();
        reader.readConfig(confFileName);
        HashMap<String, String> fieldMap = reader.fieldMap;
        String[] allRoles = fieldMap.get("role").split(":");

        // PRINT HEADER
        String roleHeader = StringUtils.join(allRoles, "\t");
        writer.print("process" + "\t" + "pattern" + "\t" + "query" + "\t" + roleHeader + "\t" + "sentence" + "\n");

        for (int i = 0; i < arr.size(); i++) {
            ArgProcessAnnotationData currentData = arr.get(i);
            StringBuffer lineStrBuf = new StringBuffer("");
            lineStrBuf.append(currentData.getProcessName()).append("\t");
            lineStrBuf.append(currentData.getPattern()).append("\t");
            lineStrBuf.append(currentData.getQuery()).append("\t");
            HashMap<String, ArrayList<ArgumentSpan>> roles = currentData.getSentence().getRoleArgMap();
            for (String roleName : allRoles) {
                ArrayList<ArgumentSpan> spans = roles.get(roleName);
                StringBuffer roleFillerBuff = new StringBuffer();
                for (ArgumentSpan span : spans) {
                    String text = span.getText();
                    roleFillerBuff.append(text);
                }

                lineStrBuf.append(roleFillerBuff.toString()).append("\t");
            }
            lineStrBuf.append(currentData.getRawText());
            writer.println(lineStrBuf.toString());
        }
        writer.close();
    }*/
}
