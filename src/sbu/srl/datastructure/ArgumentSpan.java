/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.datastructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import qa.dep.DependencyNode;

/**
 * @author slouvan
 */
public class ArgumentSpan implements Serializable, Comparable {

    static final long serialVersionUID = 2106L;
    int id = 0;
    ArrayList<DependencyNode> argNodes;
    String annotatedRole = ""; // our confusing label : undergoer, enabler etc
    String annotatedLabel = ""; // this is used for 1/0 arg label
    String multiClassLabel = "";
    double prob = 0.0;          // probability for the current assigned label
    HashMap<String, Double> roleProbPair = new HashMap<String, Double>(); // probability for all role, this is filled in during prediction
    String rolePredicted = "";
    public String pred = "";
    String propBankLabel = "";
    private String query = "";
    private String pattern = "";
    HashMap<String, String> roleFeatureVector = new HashMap<String, String>();

    private int startIdxJSON;
    private int endIdxJSON;
    private String textJSON;

    public ArgumentSpan(ArrayList<DependencyNode> argNodes, String roleType) {
        this.argNodes = argNodes;
        this.annotatedRole = roleType;
    }

    public String getMultiClassLabel() {
        return multiClassLabel;
    }

    public void setMultiClassLabel(String multiClassLabel) {
        this.multiClassLabel = multiClassLabel;
    }

    public void normalizeProbScore() {
        /*double norm = roleProbPair.values().stream().mapToDouble(Number::doubleValue).sum();
         for (String roleKey : roleProbPair.keySet())
         {
         roleProbPair.put(roleKey, roleProbPair.get(roleKey)/norm);
         }*/
        double norm = 0.0;
        for (Double probVal : roleProbPair.values()) {
            norm += Math.pow(Math.E, probVal);
        }
        for (String roleKey : roleProbPair.keySet()) {
            roleProbPair.put(roleKey, Math.pow(Math.E, roleProbPair.get(roleKey)) / norm);
        }
    }

    public double getMaxProbScore() {
        double max = Double.MIN_VALUE;
        for (String key : roleProbPair.keySet()) {
            max = Math.max(max, roleProbPair.get(key));
        }

        return max;
    }

    public int getStartIdxJSON() {
        return startIdxJSON;
    }

    public void setStartIdxJSON(int startIdxJSON) {
        this.startIdxJSON = startIdxJSON;
    }

    public int getEndIdxJSON() {
        return endIdxJSON;
    }

    public void setEndIdxJSON(int endIdxJSON) {
        this.endIdxJSON = endIdxJSON;
    }

    public String getTextJSON() {
        return textJSON;
    }

    public void setTextJSON(String textJSON) {
        this.textJSON = textJSON;
    }

    public HashMap<String, String> getRoleFeatureVector() {
        return roleFeatureVector;
    }

    public void setRoleFeatureVector(HashMap<String, String> roleFeatureVector) {
        this.roleFeatureVector = roleFeatureVector;
    }

    public String getRolePredicted() {
        return rolePredicted;
    }

    public void setRolePredicted(String rolePredicted) {
        this.rolePredicted = rolePredicted;
    }

    public String getAnnotatedRole() {
        return annotatedRole;
    }

    public void setAnnotatedRole(String annotatedRole) {
        this.annotatedRole = annotatedRole;
    }

    public String getQuery() {
        return query;
    }

    public void predictRoleType(boolean isMultiClass) {
        if (isMultiClass) {
            double maxProb = Double.MIN_VALUE;
            String rolePrediction = "";
            for (String roleLabel : roleProbPair.keySet()) {
                if (roleProbPair.get(roleLabel) > maxProb) {
                    rolePrediction = roleLabel;
                    maxProb = roleProbPair.get(roleLabel);
                }
            }
            setRolePredicted(rolePrediction);
        } else {
            String rolePrediction = "NONE";
            double maxProb = Double.MIN_VALUE;
            for (String roleLabel : roleProbPair.keySet()) {
                if (roleProbPair.get(roleLabel) > 0.3 && roleProbPair.get(roleLabel) > maxProb) {
                    rolePrediction = roleLabel;
                    maxProb = roleProbPair.get(roleLabel);
                }
            }
            setRolePredicted(rolePrediction);
        }
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPred() {
        return pred;
    }

    public void setPred(String pred) {
        this.pred = pred;
    }

    public String getPropBankLabel() {
        return propBankLabel;
    }

    public void setPropBankLabel(String propBankLabel) {
        this.propBankLabel = propBankLabel;
    }

    public ArgumentSpan() {

    }

    public HashMap<String, Double> getRoleProbPair() {
        return roleProbPair;
    }

    public void setRoleProbPair(HashMap<String, Double> roleProbPair) {
        this.roleProbPair = roleProbPair;
    }

    public int getStartIdx() {
        ArrayList<Integer> idxs = new ArrayList<Integer>();
        if (argNodes == null) {
            return startIdxJSON;
        }
        for (DependencyNode node : argNodes) {
            idxs.add(node.getId());
        }
        Collections.sort(idxs);

        return idxs.get(0);
    }

    public int getEndIdx() {
        ArrayList<Integer> idxs = new ArrayList<Integer>();
        if (argNodes == null) {
            return endIdxJSON;
        }
        for (DependencyNode node : argNodes) {
            idxs.add(node.getId());
        }
        Collections.sort(idxs);

        return idxs.get(idxs.size() - 1);
    }

    public ArrayList<Integer> getRoleIdx() {
        ArrayList<Integer> idxs = new ArrayList<Integer>();
        for (DependencyNode node : argNodes) {
            idxs.add(node.getId());
        }

        return idxs;
    }

    public ArrayList<DependencyNode> getArgNodes() {
        return argNodes;
    }

    public void setArgNodes(ArrayList<DependencyNode> argNodes) {
        this.argNodes = argNodes;
    }

    public String getRoleType() {
        return annotatedRole;
    }

    public void setRoleType(String roleType) {
        this.annotatedRole = roleType;
    }

    public String getAnnotatedLabel() {
        return annotatedLabel;
    }

    public void setAnnotatedLabel(String argLabel) {
        this.annotatedLabel = argLabel;
    }

    public double getProb() {
        return prob;
    }

    public void setProb(double prob) {
        this.prob = prob;
    }

    public String getText() {
        StringBuffer strBuff = new StringBuffer("");
        if (argNodes == null)
            return null;
        for (int i = 0; i < argNodes.size(); i++) {
            strBuff.append(argNodes.get(i).getForm()).append(" ");
        }

        return strBuff.toString().trim();
    }

    public String toString() {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("ArgumentSpan = [");
        strBuff.append("[argID=" + id);
        strBuff.append(",startIdx=" + getStartIdx());
        strBuff.append(",endIdx=" + getEndIdx());
        List<String> roles = new ArrayList<String>();
        roles.addAll(roleProbPair.keySet());
        Collections.sort(roles);
        strBuff.append(",roles=[");
        int roleCnt = roles.size();
        int cnt = 0;
        for (String role : roles) {
            cnt++;
            if (cnt == roleCnt) {
                strBuff.append(role + "=" + roleProbPair.get(role));
            } else {
                strBuff.append(role + "=" + roleProbPair.get(role)).append(",");
            }
        }
        strBuff.append("]");
        strBuff.append("]");
        return strBuff.toString();
    }

    @Override
    public int compareTo(Object o) {
        ArgumentSpan otherSpan = (ArgumentSpan) o;
        if (this.id < otherSpan.id) {
            return -1;
        }
        if (this.id > otherSpan.id) {
            return 1;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ArgumentSpan other = (ArgumentSpan) obj;

        if (getStartIdx() == other.getStartIdx() && getEndIdx() == other.getEndIdx() && this.pattern.equalsIgnoreCase(other.pattern)) {
            return true;
        } else {
            return false;
        }
    }

}
