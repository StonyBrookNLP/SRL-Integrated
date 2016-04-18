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
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author slouvan
 */
public class ArgumentSpanSerializer implements JsonSerializer<ArgumentSpan> {

    @Override
    public JsonElement serialize(ArgumentSpan span, Type type, JsonSerializationContext context) {
        System.out.println(span);
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("argId", span.getId());
        jsonObject.addProperty("startIdx", span.getStartIdx());
        jsonObject.addProperty("endIdx", span.getEndIdx());
        jsonObject.addProperty("pattern", span.getPattern());
        jsonObject.addProperty("text", span.getText()==null?span.getTextJSON():span.getText());
        jsonObject.addProperty("annotatedRole", span.getAnnotatedRole());
        jsonObject.addProperty("annotatedLabel", span.getAnnotatedLabel());
        jsonObject.addProperty("rolePredicted", span.getRolePredicted());

        final JsonArray roleProbArray = new JsonArray();
        HashMap<String, Double> roleProbs = span.getRoleProbPair();
        
        List<String> roles = new ArrayList<String>();

        roles.addAll(roleProbs.keySet());
        Collections.sort(roles);

        for (final String roleName : roleProbs.keySet()) {

            final JsonObject jsonRoleProb = new JsonObject();
            jsonRoleProb.add(roleName, new JsonPrimitive(roleProbs.get(roleName)));
            roleProbArray.add(jsonRoleProb);
        }

        jsonObject.add("probRoles", roleProbArray);
        
        final JsonArray roleFeatureVector = new JsonArray();
        HashMap<String, String> roleFeatureVectorMap = span.getRoleFeatureVector();
        for (final String roleName : roleFeatureVectorMap.keySet()) {

            final JsonObject jsonRoleFeatureVector = new JsonObject();
            jsonRoleFeatureVector.add(roleName, new JsonPrimitive(roleFeatureVectorMap.get(roleName)));
            roleFeatureVector.add(jsonRoleFeatureVector);
        }
        jsonObject.add("featVector", roleFeatureVector);
        return jsonObject;
    }

}
