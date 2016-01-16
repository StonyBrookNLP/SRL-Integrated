/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import sbu.srl.googlesent.RoleSpan;
import qa.StanfordLemmatizerSingleton;

/**
 *
 * @author samuellouvan
 */
public class StringUtil {

    int THRESHOLD = 3;

    public static String[] getTokenAsArr(String str, String separator) {
        return str.split(separator);
    }

    public static ArrayList<String> getTokenAsList(String str, String separator) {
        String strs[] = str.split(separator);
        ArrayList<String> results = new ArrayList<String>();

        for (String s : strs) {
            results.add(s.trim());
        }

        return results;
    }
    public static void removePunctuationStartEndFromRoleSpan(List<RoleSpan> tokens)
    {
         //ArrayList<String> filtered = new ArrayList<String>();
        if (tokens.size() > 0) {
            String str = tokens.get(0).getNodeSpan().getForm();
            Pattern p = Pattern.compile("\\p{Punct}+");
            Matcher m = p.matcher(str);
            if (m.matches()) {
                tokens.remove(0);
            }
            str = tokens.get(tokens.size() - 1).getNodeSpan().getForm();
            p = Pattern.compile("\\p{Punct}+");
  
            m = p.matcher(str);
            if (m.matches())
                tokens.remove(tokens.size() - 1);
            System.out.println("Filtered");
        }
    }
    public static void removePunctuationStartEnd(List<String> tokens) {
        //ArrayList<String> filtered = new ArrayList<String>();
        if (tokens.size() > 0) {
            String str = tokens.get(0);
            Pattern p = Pattern.compile("\\p{Punct}+");
            Matcher m = p.matcher(str);
            if (m.matches()) {
                tokens.remove(0);
            }
            str = tokens.get(tokens.size() - 1);
            p = Pattern.compile("\\p{Punct}+");
  
            m = p.matcher(str);
            if (m.matches())
                tokens.remove(tokens.size() - 1);
            System.out.println("Filtered");
        }
        //return filtered;
    }

    public static boolean isHeader(String line) {
        String fields[] = line.split("\t");
        if (fields[0].equalsIgnoreCase("process") && fields[1].equalsIgnoreCase("undergoer")) {
            return true;
        }
        return false;
    }

    public static boolean contains(String str, String[] strArr) {
        for (String s : strArr) {
            if (s.trim().equalsIgnoreCase(str) || s.replaceAll("\\s+", "").equalsIgnoreCase(str)) {
                return true;
            }
        }

        return false;
    }

    public static boolean containsNormalized(String str[], String[] strArr) {
        for (String s : strArr) {
            for (String ss : str) {
                if (s.trim().equalsIgnoreCase(ss) || s.replaceAll("\\s+", "").equalsIgnoreCase(ss)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean contains(String str, String regex) {
        return true;
    }

    public static ArrayList<String> generateSubsetFromArr(ArrayList<String> arr) {
        class LengthFirstComparator implements Comparator<String> {

            @Override
            public int compare(String o1, String o2) {
                if (o1.length() != o2.length()) {
                    return o1.length() - o2.length(); //overflow impossible since lengths are non-negative
                }
                return o1.compareTo(o2);
            }
        }

        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < arr.size(); i++) {
            ArrayList<String> temp = generateSubset(arr.get(i));
            for (int j = 0; j < temp.size(); j++) {
                result.add(temp.get(j));
            }
        }
        Set<String> hs = new HashSet<>();
        hs.addAll(result);
        result.clear();
        result.addAll(hs);
        Collections.sort(result, new LengthFirstComparator().reversed());
        return result;
    }

    public static ArrayList<String> generateSubset(String roleFiller) {
        String[] fillers = roleFiller.split("\\s+");
        int powSetSize = (int) Math.pow(2, fillers.length);
        ArrayList<String> combination = new ArrayList<String>();
        /*Run from counter 000..0 to 111..1*/
        for (int counter = 0; counter < powSetSize; counter++) {
            String result = "";
            for (int j = 0; j < powSetSize; j++) {
                /* Check if jth bit in the counter is set
                 If set then pront jth element from set */
                if ((counter & (1 << j)) != 0) {
                    result += fillers[j] + " ";
                    //System.out.print(fillers[j]);
                }
            }
            if (result.length() > 0) {
                combination.add(0, result);
            }
            //System.out.println(result);

        }
        return combination;
    }

    public static String toString(String strArr[]) {
        StringBuffer res = new StringBuffer();
        for (String s : strArr) {
            if (s.length() == 0) {
                res.append("\n");
            } else {
                res.append(s + "\n");
            }
        }
        return res.toString();
    }

    public static ArrayList<String> getMatch(List<String> tokens, ArrayList<String> targets) {
        ArrayList<String> results = new ArrayList<String>();
        Set<String> targetSet = new HashSet<String>(targets);
        for (String s : targetSet) {
            for (int j = 0; j < tokens.size(); j++) {
                if (StringUtils.getLevenshteinDistance(s, tokens.get(j)) < 0.3 * s.length()) {
                    if (!results.contains(tokens.get(j))) {
                        results.add(tokens.get(j));
                    }
                }
            }
        }

        return results;
    }

    public static ArrayList<String> getMatchStem(List<String> tokens, ArrayList<String> targets) {
        StanfordLemmatizerSingleton stem = StanfordLemmatizerSingleton.getInstance();
        ArrayList<String> results = new ArrayList<String>();
        Set<String> targetSet = new HashSet<String>(targets);
        for (String s : targetSet) {
            for (int j = 0; j < tokens.size(); j++) {
                String stemmedS = stem.lemmatize(s.toLowerCase()).get(0);
                String stemmedToken = stem.lemmatize(tokens.get(j).toLowerCase()).get(0);
                if (stemmedS.equalsIgnoreCase(stemmedToken)) {
                    if (!results.contains(tokens.get(j))) {
                        results.add(tokens.get(j));
                    }
                }
            }
        }

        return results;
    }

    public static boolean isCapitalWord(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (!Character.isUpperCase(word.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidSentence(String sent, List<String> tokens) {
        int cnt = 0;
        for (int i = 0; i < sent.length(); i++) {
            if (sent.charAt(i) == '.') {
                cnt++;
            }
        }
        if (cnt > 1 || cnt == 0) {
            return false;
        }

        if (sent.contains("?") || sent.contains("_") || sent.contains("<") || sent.toLowerCase().contains("condensation")
                || sent.toLowerCase().contains("melting") || sent.toLowerCase().contains("freezing")) {
            return false;
        }

        if (sent.length() > 135) {
            return false;
        }
        if (sent.length() < 20) {
            return false;
        }
        int cntNom = 0;
        for (String token : tokens) {
            if (isCapitalWord(token)) {
                return false;
            }
            if (token.endsWith("tion")) {
                cntNom++;
            }
            if (cntNom > 1) {
                return false;
            }
        }
        return true;
    }

    public static String getTokensWithSeparator(ArrayList<String> arr, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            sb.append(s.trim());
            sb.append("|");
        }
        String str = sb.toString();
        return str.substring(0, str.length() - 1);
    }

    public static String removeFunctionWordsFromRoleFillers(String str) {
        String[] roleFillers = str.split("\\|");
        Set<String> functionWords = FunctionWords.FUNCTION_WORDS;
        for (int i = 0; i < roleFillers.length; i++) {
            String fillers[] = roleFillers[i].split("\\s+");
            String cleanedFillers = "";
            for (int j = 0; j < fillers.length; j++) {
                if (!functionWords.contains(fillers[j])) {
                    cleanedFillers = cleanedFillers + fillers[j] + " ";
                }
            }
            roleFillers[i] = cleanedFillers;
        }

        String resultStr = "";
        for (int i = 0; i < roleFillers.length; i++) {
            resultStr += roleFillers[i] + "|";
        }

        if (resultStr.charAt(resultStr.length() - 1) == '|') {
            resultStr = resultStr.substring(0, resultStr.length() - 1);
        }

        return resultStr;
    }

    public static void main(String[] args) {
        //System.out.println(Arrays.toString(getTokenAsArr("absorption | absorp", "\\|")));
        ArrayList<String> str = new ArrayList<String>();
        str.add("energy");
        str.add("...");
        removePunctuationStartEnd(str);
        System.out.println(str);

    }

}
