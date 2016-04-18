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
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
/**
 *
 * @author slouvan
 */
public class ArgProcessAnnotationDataSerializer implements JsonSerializer<ArgProcessAnnotationData>{

    @Override
    public JsonElement serialize(ArgProcessAnnotationData data, Type type, JsonSerializationContext context) {
        //return context.serialize(data.getSentence());
        return null;
    }
    
}
