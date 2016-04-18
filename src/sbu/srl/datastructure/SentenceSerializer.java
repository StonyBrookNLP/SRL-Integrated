/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.datastructure;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author slouvan
 */
public class SentenceSerializer implements JsonSerializer<Sentence> {

    private boolean predict;
    
    public SentenceSerializer(boolean predict) {
        this.predict = predict;
    }

    @Override
    public JsonElement serialize(Sentence s, Type type, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("sentenceId", s.getId());
        jsonObject.addProperty("text", s.getRawText());
        ArrayList<ArgumentSpan> annotatedArgumentSpans = null;
        ArrayList<ArgumentSpan> predictedArgumentSpans = (s.getAllPredictedArgumentSpan()==null?s.getPredictedArgumentSpanJSON(): s.getAllPredictedArgumentSpan());

       
        JsonElement jsonArgumentSpans = null;
        if (!predict) {
            annotatedArgumentSpans = s.getAllAnnotatedArgumentSpan();
             Collections.sort(annotatedArgumentSpans);
            jsonArgumentSpans = context.serialize(annotatedArgumentSpans);
        }
        if (predictedArgumentSpans == null)
            predictedArgumentSpans = new ArrayList<ArgumentSpan>();
        final JsonElement jsonPredictedArgumentSpans = context.serialize(predictedArgumentSpans);

        // IF
        if (!predict)
            jsonObject.add("annotatedArgumentSpan", jsonArgumentSpans);
        
        jsonObject.add("predictionArgumentSpan", jsonPredictedArgumentSpans);

        return jsonObject;
    }

    
}
