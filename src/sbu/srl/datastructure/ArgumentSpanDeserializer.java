/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.datastructure;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author slouvan
 */
public class ArgumentSpanDeserializer implements JsonDeserializer<ArgumentSpan> {

    private boolean predict;

    public ArgumentSpanDeserializer(boolean predict) {
        this.predict = predict;
    }

    @Override
    public ArgumentSpan deserialize(JsonElement json, Type type, JsonDeserializationContext jdc) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        int argId = jsonObject.get("argId").getAsInt();
        int startIdx = jsonObject.get("startIdx").getAsInt();
        int endIdx = jsonObject.get("endIdx").getAsInt();
        String pattern = null; //jsonObject.get("pattern").getAsString();
        if (!predict) {
            pattern = jsonObject.get("pattern").getAsString();
        }
        String text = "";

        text = jsonObject.get("text").getAsString();

        String annotatedRole = null;
        String annotatedLabel = null;
        if (!predict) {
            annotatedRole = jsonObject.get("annotatedRole").getAsString();
            annotatedLabel = jsonObject.get("annotatedLabel").getAsString();
        }

        String rolePredicted = jsonObject.get("rolePredicted").getAsString();
        JsonArray roleProbArr = jsonObject.get("probRoles").getAsJsonArray();

        HashMap<String, Double> roleProbPair = new HashMap<String, Double>();

        for (int i = 0; i < roleProbArr.size(); i++) {
            Set<Entry<String, JsonElement>> entrySet = roleProbArr.get(i).getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> entry : entrySet) {
                if (roleProbPair.get(entry.getKey()) == null) {
                    roleProbPair.put(entry.getKey(), entry.getValue().getAsDouble());
                }
            }
        }

        HashMap<String, String> roleFeatureVectorPair = new HashMap<String, String>();

        /*for (int i = 0; i < roleFeatureVector.size(); i++) {
         Set<Entry<String, JsonElement>> entrySet = roleFeatureVector.get(i).getAsJsonObject().entrySet();
         for (Map.Entry<String, JsonElement> entry : entrySet) {
         if (roleFeatureVectorPair.get(entry.getKey()) == null) {
         roleFeatureVectorPair.put(entry.getKey(), entry.getValue().getAsString());
         }
         }
         }*/
        ArgumentSpan span = new ArgumentSpan();
        span.setId(argId);
        if (!predict) {
            span.setPattern(pattern);
            span.setAnnotatedLabel(annotatedLabel);
            span.setAnnotatedRole(annotatedRole);
        }
        span.setTextJSON(text);
        span.setStartIdxJSON(startIdx);
        span.setEndIdxJSON(endIdx);

        span.setRolePredicted(rolePredicted);
        span.setRoleProbPair(roleProbPair);
        //span.setRoleFeatureVector(roleFeatureVectorPair);
        return span;
    }

}
