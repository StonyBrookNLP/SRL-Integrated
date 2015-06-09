/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.distantsupervision;

import Util.ArrUtil;
import Util.GlobalVariable;
import Util.ProcessFrameUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.StanfordDepParserSingleton;
import qa.StanfordTokenizerSingleton;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
/**
 * INPUT : a process tsv file OUTPUT : a process tsv file with A1 and A2
 * annotated by using specific dependency pattern
 *
 * @author samuellouvan
 */
public class PatternBasedAnnotator {

    // Load DS process frame
    // For each DS sentence
    // for every role e.g. enabler + result
    // if empty then perform static pattern checking
    //  for every result of pattern checking
    //      if not intersected with existing argument then add the new role values
    ProcessFrameProcessor proc;
    StanfordDepParserSingleton depParser = StanfordDepParserSingleton.getInstance();
    StanfordTokenizerSingleton tokenizer = StanfordTokenizerSingleton.getInstance();

    public PatternBasedAnnotator(String processFileName) throws IOException, FileNotFoundException, ClassNotFoundException {
        proc = new ProcessFrameProcessor(processFileName);
        proc.loadProcessData();
    }

    public void labelA2(ProcessFrame frame, ArrayList<Integer> allIdxs) throws IOException
    {
        String rawText = frame.getRawText();
        DependencyTree tree = depParser.parse(rawText);
        List<String> tokens = tokenizer.tokenize(rawText);
        String[] lexPattern = {"by", "through", "with", "because"};
        int enablerLexIdx = -1;
        boolean stop = false;
        
        if (stop) {
            ArrayList<Integer> triggerIdxs = frame.getTriggerIdx();
            //Collections.sort(triggerIdxs);
            boolean left = true;
            for (Integer idx : triggerIdxs) {
                if (idx > enablerLexIdx) {
                    left = false;
                    break;
                }
            }

            if (left) {
                // get the ID of the trigger Lex in dependency parse
                // check if the trigger word is the parent of trigger Lex
                // if yes then follow PMOD, NMOD, PMOD, NMOD
                DependencyNode enablerLexNode = tree.get(enablerLexIdx + 1);
                int idxHeadOfEnabler = enablerLexNode.getHeadID();
                if (triggerIdxs.contains(idxHeadOfEnabler)) {

                    System.out.println("This sentence may contain A1 :" + rawText);
                    ArrayList<DependencyNode> rootSubTree = new ArrayList<DependencyNode>(tree.dependentsOf(enablerLexNode));
                    if (rootSubTree.size() > 0) {
                        ArrayList<DependencyNode> childs = getLimitedChilds(tree, rootSubTree.get(0));
                        Collections.sort(childs);
                        fixGap(childs, tree);
                        if (!childs.contains(rootSubTree.get(0))) {
                            childs.add(rootSubTree.get(0));
                        }
                        fixGap(childs, tree);
                        Collections.sort(childs);
                        Collections.sort(allIdxs);
                        ArrayList<Integer> candidateIdxs = getIdxs(childs);
                        ArrayList<Integer> intersections = ArrUtil.intersection(candidateIdxs, allIdxs);
                        Collections.sort(intersections);

                        //updateChildDueToIntersection(childs, intersections); //
                        if (intersections.size() == 0) {
                            String finalEnabler = "";
                            for (int i = 0; i < childs.size(); i++) {
                                System.out.print(childs.get(i).getForm() + " ");
                                finalEnabler = finalEnabler.concat(childs.get(i).getForm() + " ");
                            }
                            finalEnabler = finalEnabler.trim();
                            frame.setEnabler(finalEnabler);
                            frame.processRoleFillers();
                            System.out.println(frame.getEnablerIdx());
                            System.out.println("");
                        }
                    }

                }
            } else {

            }
        }
    }
    
    public void labelA1(ProcessFrame frame, ArrayList<Integer> allIdxs) throws IOException {
        String rawText = frame.getRawText();
        DependencyTree tree = depParser.parse(rawText);
        List<String> tokens = tokenizer.tokenize(rawText);
        String[] lexPattern = {"by", "through", "with", "because"};
        int enablerLexIdx = -1;
        boolean stop = false;
        for (int i = 0; i < lexPattern.length && !stop; i++) {
            for (int j = 0; j < tokens.size() && !stop; j++) {
                String token = tokens.get(j);
                if (token.equalsIgnoreCase(lexPattern[i])) {
                    enablerLexIdx = j;
                    stop = true;
                }
            }
        }
        if (stop) {
            ArrayList<Integer> triggerIdxs = frame.getTriggerIdx();
            //Collections.sort(triggerIdxs);
            boolean left = true;
            for (Integer idx : triggerIdxs) {
                if (idx > enablerLexIdx) {
                    left = false;
                    break;
                }
            }

            if (left) {
                // get the ID of the trigger Lex in dependency parse
                // check if the trigger word is the parent of trigger Lex
                // if yes then follow PMOD, NMOD, PMOD, NMOD
                DependencyNode enablerLexNode = tree.get(enablerLexIdx + 1);
                int idxHeadOfEnabler = enablerLexNode.getHeadID();
                if (triggerIdxs.contains(idxHeadOfEnabler)) {

                    System.out.println("This sentence may contain A1 :" + rawText);
                    ArrayList<DependencyNode> rootSubTree = new ArrayList<DependencyNode>(tree.dependentsOf(enablerLexNode));
                    if (rootSubTree.size() > 0) {
                        ArrayList<DependencyNode> childs = getLimitedChilds(tree, rootSubTree.get(0));
                        Collections.sort(childs);
                        fixGap(childs, tree);
                        if (!childs.contains(rootSubTree.get(0))) {
                            childs.add(rootSubTree.get(0));
                        }
                        fixGap(childs, tree);
                        Collections.sort(childs);
                        Collections.sort(allIdxs);
                        ArrayList<Integer> candidateIdxs = getIdxs(childs);
                        ArrayList<Integer> intersections = ArrUtil.intersection(candidateIdxs, allIdxs);
                        Collections.sort(intersections);

                        //updateChildDueToIntersection(childs, intersections); //
                        if (intersections.size() == 0) {
                            String finalEnabler = "";
                            for (int i = 0; i < childs.size(); i++) {
                                System.out.print(childs.get(i).getForm() + " ");
                                finalEnabler = finalEnabler.concat(childs.get(i).getForm() + " ");
                            }
                            finalEnabler = finalEnabler.trim();
                            frame.setEnabler(finalEnabler);
                            frame.processRoleFillers();
                            System.out.println(frame.getEnablerIdx());
                            System.out.println("");
                        }
                    }

                }
            } else {

            }
        }

    }

    /*public ArrayList<DependencyNode> updateChildDueToIntersection(ArrayList<DependencyNode> nodes, ArrayList<Integer> intersections)
     {
       
     }*/
    public ArrayList<Integer> getIdxs(ArrayList<DependencyNode> nodes) {
        ArrayList<Integer> idxs = new ArrayList<Integer>();
        for (int i = 0; i < nodes.size(); i++) {
            idxs.add(nodes.get(i).getId());
        }
        return idxs;
    }

    public void fixGap(ArrayList<DependencyNode> nodes, DependencyTree tree) {
        if (nodes.size() < 2) {
            return;
        }
        ArrayList<DependencyNode> missed = new ArrayList<DependencyNode>();
        for (int i = 1; i < nodes.size(); i++) {
            int delta = nodes.get(i).getId() - nodes.get(i - 1).getId();
            if (delta > 1) {
                for (int j = nodes.get(i - 1).getId() + 1; j < nodes.get(i - 1).getId() + delta; j++) {
                    if (!nodes.contains(tree.get(j))) {
                        missed.add(tree.get(j));
                    }
                }
            }
        }

        if (missed.size() > 0) {
            nodes.addAll(missed);
        }
    }

    public ArrayList<DependencyNode> getLimitedChilds(DependencyTree tree, DependencyNode enablerLexNode) {
        ArrayList<DependencyNode> limitedChilds = new ArrayList<DependencyNode>();
        Set<DependencyNode> childs = tree.dependentsOf(enablerLexNode);

        for (DependencyNode node : childs) {
            Set<DependencyNode> child = tree.dependentsOf(node);
            if (child.size() > 0) {
                limitedChilds.addAll(child);
            }
        }
        limitedChilds.addAll(childs);
        return limitedChilds;
    }

    /**
     * For each process frame : Check if enabler/ result is empty If it is empty
     * then try find the role values based on the dependency pattern
     */
    public void annotate(String outFileName) throws IOException {
        ArrayList<ProcessFrame> frames = proc.getProcArr();
        for (int i = 0; i < frames.size(); i++) {
            try {
                //System.out.println(i);
                ArrayList<Integer> labeledIdx = frames.get(i).getAllLabeledIndex();
                ArrayList<Integer> enablerIdxs = frames.get(i).getEnablerIdx();
                ArrayList<Integer> resultIdxs = frames.get(i).getResultIdx();

                if (enablerIdxs.size() == 0) {
                    labelA1(frames.get(i), labeledIdx);
                }
                if (resultIdxs.size() == 0) {
                    //labelA2(frames.get(i), labeledIdx);
                }
            } catch (Exception e) {

            }
        }
        
        ProcessFrameUtil.dumpFramesToFile(frames, outFileName);
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException {

        PatternBasedAnnotator annotator = new PatternBasedAnnotator(GlobalVariable.PROJECT_DIR + "/data/ds_most_frequent_7_06_2015/ds_all_processes.tsv");
        annotator.annotate(GlobalVariable.PROJECT_DIR + "/data/ds_most_frequent_7_06_2015/ds_all_processes_w_pattern.tsv");
    }
}
