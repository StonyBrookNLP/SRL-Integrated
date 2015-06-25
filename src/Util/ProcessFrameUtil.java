/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.StanfordDepParserSingleton;
import qa.StanfordTokenizerSingleton;
import qa.dep.DependencyTree;

/**
 *
 * @author samuellouvan
 */
public class ProcessFrameUtil {

    public static ProcessFrame createProcessFrame(String processName, ArrayList<String> undergoer, ArrayList<String> enabler, ArrayList<String> trigger,
            ArrayList<String> result, String sentence) {
        ProcessFrame frame = new ProcessFrame();
        frame.setProcessName(processName);
        if (undergoer != null&&!undergoer.isEmpty()) {
            frame.setUnderGoer(StringUtil.getTokensWithSeparator(undergoer, ProcessFrameProcessor.SEPARATOR));
        } else {
            frame.setUnderGoer("");
        }
        if (enabler != null&& !enabler.isEmpty()) {
            frame.setEnabler(StringUtil.getTokensWithSeparator(enabler, ProcessFrameProcessor.SEPARATOR));
        } else {
            frame.setEnabler("");
        }
        if (trigger != null && !trigger.isEmpty()) {
            frame.setTrigger(StringUtil.getTokensWithSeparator(trigger, ProcessFrameProcessor.SEPARATOR));
        } else {
            frame.setTrigger("");
        }
        if (result != null&& !result.isEmpty()) {
            frame.setResult(StringUtil.getTokensWithSeparator(result, ProcessFrameProcessor.SEPARATOR));
        } else {
            frame.setResult("");
        }
        frame.setUnderSpecified("");
        frame.setRawText(sentence);
        return frame;
    }

    public static String normalizeProcessName(String unnormalizedProcessName) {
        String normName = unnormalizedProcessName.replaceAll("\\|", "_");
        normName = normName.replaceAll("\\s+", "");
        return normName;
    }

    public static void toClearParserFormat(ProcessFrame p, String outFileName) throws FileNotFoundException {

        PrintWriter writer = new PrintWriter(outFileName);

        String rawText = p.getRawText();

        rawText = rawText.replace(".", " ");
        rawText = rawText.replaceAll("\"", "");
        rawText = rawText.trim();
        rawText += ".";

        // update tokenized text here
        List<String> tokenized = StanfordTokenizerSingleton.getInstance().tokenize(rawText);
        p.setTokenizedText(tokenized.toArray(new String[tokenized.size()]));
        try {
            DependencyTree tree = StanfordDepParserSingleton.getInstance().parse(rawText);

            String conLLStr = ClearParserUtil.toClearParserFormat(tree, p);
            writer.println(conLLStr);
            writer.println();
        } catch (Exception e) {

        }

        writer.close();
    }

    public static void toConll2009Format(ArrayList<ProcessFrame> processFrames, String mateParserFileName) throws FileNotFoundException, IOException {
         PrintWriter writer = new PrintWriter(mateParserFileName);
        System.out.println("Converting to clear parser format, data size : " + processFrames.size() + " frames");
        int cnt = 0;
        for (ProcessFrame p : processFrames) {
            String rawText = p.getRawText();

            rawText = rawText.replace(".", " ");
            rawText = rawText.replaceAll("\"", "");
            rawText = rawText.trim();
            for (int j = rawText.length()-1 ; ; j--)
            {
                if (Character.isAlphabetic(rawText.charAt(j)))
                {
                    rawText = rawText.substring(0,j+1);
                    rawText += ".";
                    break;
                }
            }
            
            // update tokenized text here
            List<String> tokenized = StanfordTokenizerSingleton.getInstance().tokenize(rawText);
            //System.out.println(tokenized.size());
            //System.out.println(rawText);
            p.setTokenizedText(tokenized.toArray(new String[tokenized.size()]));
            try {
                //System.out.println("DEP TREE");
                DependencyTree tree = StanfordDepParserSingleton.getInstance().parse(rawText);
                //System.out.println("END OF DEP TREE");
                String conLLStr = ClearParserUtil.toCONLL2009Format(tree, p);
                writer.println(conLLStr);
                writer.println();
                ++cnt;
                //System.out.print(++cnt + " ");

            } catch (Exception e) {
                System.out.println(rawText);
                e.printStackTrace();
            }
            if (cnt % 100 == 0) {
                //System.out.println("");
            }
        }
        if (cnt == processFrames.size())
            System.out.println("PERFECT !");
        else
        {
            System.out.println("Not everything is converted to clear parser format : "+cnt+"/"+processFrames.size());
        }
        System.out.println("");
        writer.close();
    }
    
    public static void toParserFormat(ArrayList<ProcessFrame> processFrames, String parserFileName, int parserType) throws IOException
    {
        if (parserType == Constant.SRL_CLEARPARSER)
        {
            toClearParserFormat(processFrames, parserFileName);
        }
        else if (parserType == Constant.SRL_MATE)
        {
            toConll2009Format(processFrames, parserFileName);
        }
    }
    public static void toClearParserFormat(ArrayList<ProcessFrame> processFrames, String clearParserFileName) throws FileNotFoundException, IOException {
        PrintWriter writer = new PrintWriter(clearParserFileName);
        System.out.println("Converting to clear parser format, data size : " + processFrames.size() + " frames");
        int cnt = 0;
        for (ProcessFrame p : processFrames) {
            
            String rawText = p.getRawText();

            rawText = rawText.replace(".", " ");
            rawText = rawText.replaceAll("\"", "");
            rawText = rawText.trim();
            for (int j = rawText.length()-1 ; ; j--)
            {
                if (Character.isAlphabetic(rawText.charAt(j)))
                {
                    rawText = rawText.substring(0,j+1);
                    rawText += ".";
                    break;
                }
            }
            

            // update tokenized text here
            List<String> tokenized = StanfordTokenizerSingleton.getInstance().tokenize(rawText);
            //System.out.println(tokenized.size());
            //System.out.println(rawText);
            p.setTokenizedText(tokenized.toArray(new String[tokenized.size()]));
            try {
                //System.out.println("DEP TREE");
                DependencyTree tree = StanfordDepParserSingleton.getInstance().parse(rawText);
                //System.out.println("END OF DEP TREE");
                String conLLStr = ClearParserUtil.toClearParserFormat(tree, p);
                writer.println(conLLStr);
                writer.println();
                ++cnt;
                //System.out.print(++cnt + " ");

            } catch (Exception e) {
                System.out.println(rawText);
                e.printStackTrace();
            }
            if (cnt % 100 == 0) {
                //System.out.println("");
            }
        }
        if (cnt == processFrames.size())
            System.out.println("PERFECT !");
        else
        {
            System.out.println("Not everything is converted to clear parser format : "+cnt+"/"+processFrames.size());
        }
        System.out.println("");
        writer.close();
    }

    public static void dumpFramesToFile(ArrayList<ProcessFrame> arr, String fileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        for (ProcessFrame p : arr) {
            writer.println(p.getProcessName() + "\t" + p.getUnderGoer() + "\t" + p.getEnabler() + "\t" + p.getTrigger() + "\t" + p.getResult() + "\t" + p.getUnderSpecified() + "\t" + p.getRawText());
        }
        writer.close();

    }
}
