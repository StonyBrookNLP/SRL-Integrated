/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.datastructure;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 *
 * @author slouvan
 */
public class ILPSRLDataDeserializer implements JsonDeserializer<JSONData> {

    @Override
    public JSONData deserialize(JsonElement json, Type type, JsonDeserializationContext jdc) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        String process = jsonObject.get("process").getAsString();
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();
        
        sentences = jdc.deserialize(jsonObject.get("sentences"), new TypeToken<ArrayList<Sentence>>() {
        }.getType());
        JSONData jsonPredData = new JSONData();
        jsonPredData.setProcessName(process);
        jsonPredData.setSentence(sentences);
        return jsonPredData;
    }

    /**
     * final JsonObject jsonObject = new JsonObject();
     * jsonObject.addProperty("process", data.getProcessName()); final
     * JsonElement jsonSentences = context.serialize(data.getSentence());
     * jsonObject.add("sentences", jsonSentences);
     *
     * return jsonObject;
     */
}
