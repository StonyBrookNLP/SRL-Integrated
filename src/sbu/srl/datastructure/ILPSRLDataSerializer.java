/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.datastructure;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author slouvan
 */
public class ILPSRLDataSerializer implements JsonSerializer<JSONData> {

    @Override
    public JsonElement serialize(JSONData data, Type type, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("process", data.getProcessName());
        final JsonElement jsonSentences = context.serialize(data.getSentence());
        jsonObject.add("sentences", jsonSentences);
        
        return jsonObject;
    }

    
}
