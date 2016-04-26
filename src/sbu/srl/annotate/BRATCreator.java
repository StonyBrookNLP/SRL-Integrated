/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.annotate;

import static Util.CCGParserUtil.getPropBankLabeledSentence;
import Util.Constant;
import edu.stanford.nlp.ling.CoreLabel;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import qa.StanfordTokenizerSingleton;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;
import qa.srl.SRLWrapper;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.Sentence;
import sbu.srl.rolextract.ArgumentGenerator;
import sbu.srl.rolextract.SpockDataReader;

/**
 *
 * @author slouvan
 */
public class BRATCreator {

    @Option(name = "--arg-propbank", usage = "a text file that contains SRL output", required = true)
    String propBankFileName;

    @Option(name = "--arg-pattern", usage = "a serialized file that contains sentence with labeled arg patterns", required = true)
    String patternBasedSentenceSerFileName;

    @Option(name = "--brat-dir", usage = "BRAT directory", required = true)
    String bratDirName;

    /* Load argument spans from EasySRL */
    public ArrayList<ArgumentSpan> loadSRLArgumentSpan(Sentence propBankSentence) {
        // PROPBANK ARGUMENT EXTRACT
        // =========================
        ArrayList<ArgumentSpan> sentArgs = new ArrayList<ArgumentSpan>();
        DependencyTree depTree = propBankSentence.getDepTree();
        HashMap<String, ArrayList<ArgumentSpan>> argFromPropBankSRL = propBankSentence.getRoleArgPropBank();
        if (argFromPropBankSRL != null) {
            for (String key : argFromPropBankSRL.keySet()) {
                // Extract argument from SRL PropBank
                sentArgs.addAll(argFromPropBankSRL.get(key));
                ArrayList<ArgumentSpan> srlSpans = argFromPropBankSRL.get(key);
                // Extract the predicate from SRL propBank add it as argument also
                for (ArgumentSpan srlSpan : srlSpans) {
                    String predicate = srlSpan.getPred();
                    if (!predicate.isEmpty()) {
                        DependencyNode predNode = null;
                        for (int i = 0; i < depTree.size(); i++) {
                            if (depTree.get(i).getForm().equalsIgnoreCase(predicate)) {
                                predNode = depTree.get(i);
                            }
                        }
                        ArrayList<DependencyNode> predNodes = new ArrayList<DependencyNode>();
                        if (predNode != null) {
                            predNodes.add(predNode);
                            ArgumentSpan predSpan = new ArgumentSpan(predNodes, "pred");
                            sentArgs.add(predSpan);
                        }
                    }
                }
            }
        }
        return sentArgs;
    }
    /* Load argument span from pattern based */

    public ArrayList<ArgumentSpan> loadPatternBasedArgumentSpan(Sentence sentence) {
        HashMap<String, ArrayList<ArgumentSpan>> argFromPattern = sentence.getRoleArgAnnotation();
        ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
        if (argFromPattern != null && !argFromPattern.isEmpty()) {
            for (String key : argFromPattern.keySet()) {
                spans.addAll(argFromPattern.get(key));
            }
        }
        return spans;
    }

    /*
     This method reads result of PropBank and Pattern Based and then generate the BRAT text and annotation files
     */
    public void fromArgumentExtractorToBRAT(String SRLPropBankResultFile, String patternBasedResultFile, String BRATDir) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, FileNotFoundException, ClassNotFoundException {
        if (!FileUtil.isDirectoryExist(BRATDir)) {
            FileUtil.mkDir(BRATDir);
        }

        // Load sentences with arguments generated
        HashMap<String, Sentence> mapPropBank = getPropBankLabeledSentence(SRLPropBankResultFile);
        HashMap<String, Sentence> mapPatternBased = (HashMap<String, Sentence>) FileUtil.deserializeFromFile(patternBasedResultFile);
        HashMap<String, ArrayList<ArgumentSpan>> sentArgPair = new HashMap<String, ArrayList<ArgumentSpan>>();

        int sentCnt = 0;
        for (String rawText : mapPropBank.keySet()) {
            Sentence srlLabeledSentence = mapPropBank.get(rawText);
            Sentence patternLabeledSentence = mapPatternBased.get(rawText);
            ArrayList<ArgumentSpan> allCandidateArgs = new ArrayList<ArgumentSpan>();

            // SRL BASED ARGUMENT EXTRACT
            ArrayList<ArgumentSpan> argsFromSRL = loadSRLArgumentSpan(srlLabeledSentence);
            if (!argsFromSRL.isEmpty()) {
                allCandidateArgs.addAll(argsFromSRL);
            }
            // PATTERN BASED ARGUMENT EXTRACT
            ArrayList<ArgumentSpan> argsFromPattern = loadPatternBasedArgumentSpan(patternLabeledSentence);
            if (!argsFromPattern.isEmpty())
                allCandidateArgs.addAll(argsFromPattern);
           

            if (allCandidateArgs.size() > 0) {
                // Extract the unique candidate arguments both from SRL AND manual pattern 
                Set<ArgumentSpan> uniqueArguments = null;
                try {
                    uniqueArguments = new HashSet<ArgumentSpan>(allCandidateArgs);
                    // Flush to BRAT text file
                    List<CoreLabel> tokenLabels = StanfordTokenizerSingleton.getInstance().getTokenLabel(srlLabeledSentence.getRawText());
                    PrintWriter writer = new PrintWriter(BRATDir + "/sent_" + sentCnt + ".txt");
                    writer.println(srlLabeledSentence.getRawText());
                    writer.close();

                    // Create BRAT *.ann file
                    writer = new PrintWriter(BRATDir + "/sent_" + sentCnt + ".ann");
                    Map<String, List<ArgumentSpan>> spanMap = uniqueArguments.stream().collect(Collectors.groupingBy(s -> s.getStartIdx() + "_" + s.getEndIdx()));
                    int txtSpanCnt = 1;
                    for (String spanKey : spanMap.keySet()) {
                        ArgumentSpan span = spanMap.get(spanKey).get(0);
                        ArrayList<DependencyNode> nodes = spanMap.get(spanKey).get(0).getArgNodes();
                        int bratStartIdx = tokenLabels.get(nodes.get(0).getId() - 1).beginPosition();
                        int bratEndIdx = tokenLabels.get(nodes.get(nodes.size() - 1).getId() - 1).endPosition();
                        writer.println("T" + (txtSpanCnt++) + "\t" + "annotate_me" + " " + bratStartIdx + " " + bratEndIdx + "\t" + srlLabeledSentence.getRawText().substring(bratStartIdx, bratEndIdx));
//                    System.out.println("=============================================================================================");
//                    System.out.println("brat start :" + bratStartIdx + "\t brat end:" + bratEndIdx);
//                    System.out.println(span.getStartIdx() + "\t" + span.getEndIdx() + "\t" + span.getText() + "\t Sent :" + srlLabeledSentence.getRawText());
//                    System.out.println(span.getText());
//                    System.out.println("=============================================================================================");
                    }
                    writer.close();
                    sentCnt++;
                } catch (Exception e) {
                    int x = 0;
                    e.printStackTrace();
                    System.out.println(srlLabeledSentence.getRawText());
                }

            } else {
                System.out.println("No arguments extracted :"+srlLabeledSentence.getRawText());
            }
        }
    }

    private void doMain() {
        try {
            fromArgumentExtractorToBRAT(propBankFileName, patternBasedSentenceSerFileName, bratDirName);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(BRATCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(BRATCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(BRATCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(BRATCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BRATCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(BRATCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        try {
            BRATCreator bratCreator = new BRATCreator();
            CmdLineParser parser = new CmdLineParser(bratCreator);
            parser.parseArgument(args);
            bratCreator.doMain();
        } catch (CmdLineException ex) {
            Logger.getLogger(ArgumentGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(BRATCreator.class.getName()).log(Level.SEVERE, null, ex);
        }

        //fromArgumentExtractorToBRAT("./sentences.args.temp", "./data/argExtractFromPatter.ser", null);
            /*ArrayList<Sentence> sentences = (ArrayList<Sentence>) FileUtil.deserializeFromFile("./data/training_4_roles.ser");
         HashMap<String, DependencyTree> depTrees = (HashMap<String, DependencyTree>) FileUtil.deserializeFromFile("./data/depTree.ser");
         String dataPath = "/home/slouvan/brat-v1.3_Crunchy_Frog/data/trial/";
         FileUtil.mkDir(dataPath);
         Map<String, List<Sentence>> sentByProcess = sentences.stream().collect(Collectors.groupingBy(Sentence::getProcessName));
         for (String processName : sentByProcess.keySet()) {
         List<Sentence> processSentences = sentByProcess.get(processName);
         for (int i = 0; i < processSentences.size(); i++) {
         Sentence currentSentence = processSentences.get(i);
         ArrayList<ArgumentSpan> annotatedSpans = currentSentence.getAllAnnotatedArgumentSpan();
         List<CoreLabel> tokenLabels = StanfordTokenizerSingleton.getInstance().getTokenLabel(currentSentence.getRawText());
         PrintWriter writer = new PrintWriter(dataPath + processName +"_"+i+".txt");
         writer.println(currentSentence.getRawText());
         writer.close();
         writer = new PrintWriter(dataPath + processName +"_"+i+".ann");
         Map<String, List<ArgumentSpan>> spanMap = annotatedSpans.stream().collect(Collectors.groupingBy( s-> s.getStartIdx()+"_"+s.getEndIdx()));
         int txtSpanCnt = 1;
         for (String spanKey : spanMap.keySet()) {
         ArgumentSpan span = spanMap.get(spanKey).get(0);
         ArrayList<DependencyNode> nodes = spanMap.get(spanKey).get(0).getArgNodes();
         int bratStartIdx = tokenLabels.get(nodes.get(0).getId() - 1).beginPosition();
         int bratEndIdx = tokenLabels.get(nodes.get(nodes.size() - 1).getId() - 1).endPosition();
         writer.println("T"+(txtSpanCnt++)+"\t"+"ANNOTATE_ME"+" "+bratStartIdx+" "+bratEndIdx+"\t"+currentSentence.getRawText().substring(bratStartIdx,bratEndIdx));
         //CoreLabel label = tokenLabels.get(span.getArgNodes().get);
         System.out.println("=============================================================================================");
         System.out.println("brat start :" + bratStartIdx + "\t brat end:" + bratEndIdx);
         System.out.println(span.getStartIdx() + "\t" + span.getEndIdx() + "\t" + span.getText() + "\t Sent :" + currentSentence.getRawText());
         System.out.println(span.getText());
         System.out.println("=============================================================================================");
         }
         writer.close();
         }
         }*/
    }

}
