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
public class SentenceDeserializer implements JsonDeserializer<Sentence> {

    private boolean predict = false;

    public SentenceDeserializer(boolean predict)
    {
        this.predict = predict;
    }
    @Override
    public Sentence deserialize(JsonElement json, Type type, JsonDeserializationContext jdc) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        int id = jsonObject.get("sentenceId").getAsInt();
        String text = jsonObject.get("text").getAsString();
        ArrayList<ArgumentSpan> annotatedArgumentSpan = new ArrayList<ArgumentSpan>();
        ArrayList<ArgumentSpan> predictedArgumentSpan = new ArrayList<ArgumentSpan>();
        if (!predict)
        annotatedArgumentSpan = jdc.deserialize(jsonObject.get("annotatedArgumentSpan"), new TypeToken<ArrayList<ArgumentSpan>>() {
        }.getType());
        
        predictedArgumentSpan = jdc.deserialize(jsonObject.get("predictionArgumentSpan"), new TypeToken<ArrayList<ArgumentSpan>>() {
        }.getType());
        Sentence sentence = new Sentence();
        sentence.setId(id);
        sentence.setRawText(text);
        if (!predict)
            sentence.setAnnotatedArgumentSpanJSON(annotatedArgumentSpan);
        
        sentence.setPredictedArgumentSpanJSON(predictedArgumentSpan);
        return sentence;
    }

    public boolean isPredict() {
        return predict;
    }

    public void setPredict(boolean predict) {
        this.predict = predict;
    }

}
