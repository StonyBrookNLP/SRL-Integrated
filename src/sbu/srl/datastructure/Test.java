/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.datastructure;

import Util.ArgProcessAnnotationDataUtil;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import qa.util.FileUtil;

/**
 *
 * @author slouvan
 */
public class Test {

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException {
// Configure GSON
        /*final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(JSONPredictionData.class, new ILPSRLDataDeserializer());
        gsonBuilder.registerTypeAdapter(Sentence.class, new SentenceDeserializer(false));
        gsonBuilder.registerTypeAdapter(ArgumentSpan.class, new ArgumentSpanDeserializer());
        final Gson gson = gsonBuilder.create();

        Reader reader = new InputStreamReader(new FileInputStream("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val-05-12/fold-1/test/test.srlpredict.json"));
        // Read the JSON data
        // Parse JSON to Java
        final ArrayList<JSONPredictionData> data = gson.fromJson(reader, new com.google.gson.reflect.TypeToken<ArrayList<JSONPredictionData>>() {
        }.getType());
        int x = 0;*/
            //System.out.println(book);

        /*ArrayList<ArgProcessAnnotationData> data = (ArrayList<ArgProcessAnnotationData>) FileUtil.deserializeFromFile("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val/fold-1/test/test.argpredict.ser");
         ArrayList<ArgProcessAnnotationData> aggregatedData = ArgProcessAnnotationDataUtil.getUniqueProcessSentencePair(data);
         Map<String, List<ArgProcessAnnotationData>> groupByProcess = aggregatedData.stream().collect(Collectors.groupingBy(ArgProcessAnnotationData::getProcessName));
         ArrayList<ILPSRLData> ilpData = ArgProcessAnnotationDataUtil.generateILPData(groupByProcess);
         // Configure GSON
         //Type listOfTestObject = new TypeToken<List<ArgProcessAnnotationData>>() {}.getType();
         //List<TestObject> list2 = gson.fromJson(s, listOfTestObject);
         final GsonBuilder gsonBuilder = new GsonBuilder();
         gsonBuilder.registerTypeAdapter(ILPSRLData.class, new ILPSRLDataSerializer());
         gsonBuilder.registerTypeAdapter(Sentence.class, new SentenceSerializer());
         gsonBuilder.registerTypeAdapter(ArgumentSpan.class, new ArgumentSpanSerializer());
         gsonBuilder.setPrettyPrinting();
         final Gson gson = gsonBuilder.create();
         //final String json = gson.toJson(data);
         //gson.to
         System.out.println(gson.toJson(ilpData, new TypeToken<ArrayList<ILPSRLData>>() {
         }.getType()));*/
        
        double x = Math.pow(Math.E, 0.6)/(Math.pow(Math.E, 0.6) + Math.pow(Math.E, 0.8)  + Math.pow(Math.E, 0.2) + Math.pow(Math.E, 0.4) );
        System.out.println(x);
    }
}
