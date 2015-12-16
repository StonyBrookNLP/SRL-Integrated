/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.ArgumentSpanDeserializer;
import sbu.srl.datastructure.ArgumentSpanSerializer;
import sbu.srl.datastructure.ILPSRLDataDeserializer;
import sbu.srl.datastructure.ILPSRLDataSerializer;
import sbu.srl.datastructure.JSONData;
import sbu.srl.datastructure.Sentence;
import sbu.srl.datastructure.SentenceDeserializer;
import sbu.srl.datastructure.SentenceSerializer;

/**
 *
 * @author slouvan
 */
public class SentenceUtil {

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

}
