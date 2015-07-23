/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.distantsupervision;

import Util.ArrUtil;
import Util.GlobalV;
import Util.ProcessFrameUtil;
import com.google.common.collect.Lists;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    public void labelA2(ProcessFrame frame, ArrayList<Integer> allIdxs) throws IOException {
        String rawText = frame.getRawText();
        DependencyTree tree = depParser.parse(rawText);
        List<String> tokens = tokenizer.tokenize(rawText);
        String[] lexPattern = {"causes", "into", "produces"};
        int resultLexIdx = -1;
        boolean stop = false;
        for (int i = 0; i < lexPattern.length && !stop; i++) {
            for (int j = 0; j < tokens.size() && !stop; j++) {
                String token = tokens.get(j);
                if (token.equalsIgnoreCase(lexPattern[i])) {
                    resultLexIdx = j;
                    stop = true;
                }
            }
        }
        if (stop) {
            ArrayList<Integer> triggerIdxs = frame.getTriggerIdx();
            //Collections.sort(triggerIdxs);
            boolean left = true;
            for (Integer idx : triggerIdxs) {
                if (idx > resultLexIdx) {
                    left = false;
                    break;
                }
            }

            if (left) {
                DependencyNode resultLexNode = tree.get(resultLexIdx + 1);
                ArrayList<DependencyNode> triggerNodes = tree.getNodes(triggerIdxs);

                if (tree.isExistPath(triggerNodes, resultLexNode) || tree.isExistPath(resultLexNode, triggerNodes)) { //|| triggerIdxs.contains(idxHeadOfEnabler)) {
                    ArrayList<DependencyNode> rootSubTree = new ArrayList<DependencyNode>(tree.dependentsOf(resultLexNode));
                    if (rootSubTree.size() > 0) {
                        ArrayList<DependencyNode> childs = new ArrayList<DependencyNode>();
                        ArrayList<DependencyNode> rootBaseResults = new ArrayList<DependencyNode>();
                        for (int i = 0; i < rootSubTree.size(); i++) {
                            if (rootSubTree.get(i).getId() > resultLexNode.getId() && !rootSubTree.get(i).getRelationLabel().equalsIgnoreCase("punct")) {
                                ArrayList<DependencyNode> nodes = getLimitedChilds(tree, rootSubTree.get(i));
                                if (nodes.size() > 0) {
                                    childs.addAll(nodes);
                                }
                                rootBaseResults.add(rootSubTree.get(i));
                                break;
                            }
                        }

                        Collections.sort(childs);
                        fixGap(childs, tree);
                        for (int i = 0; i < rootBaseResults.size(); i++) {
                            if (!childs.contains(rootBaseResults.get(i))) {
                                childs.add(rootBaseResults.get(i));
                            }
                        }

                        fixGap(childs, tree);
                        Collections.sort(childs);
                        Collections.sort(allIdxs);
                        ArrayList<Integer> candidateIdxs = getIdxs(childs);
                        ArrayList<Integer> intersections = ArrUtil.intersection(candidateIdxs, allIdxs);
                        Collections.sort(intersections);

                        //updateChildDueToIntersection(childs, intersections); //
                        if (intersections.size() == 0) {
                            String finalResult = "";
                            for (int i = 0; i < childs.size(); i++) {
                                System.out.print(childs.get(i).getForm() + " ");
                                finalResult = finalResult.concat(childs.get(i).getForm() + " ");
                            }
                            finalResult = finalResult.trim();
                            frame.setResult(finalResult);
                            frame.processRoleFillers();
                            if (frame.getResultIdx().size() != 0) {
                                System.out.println("This sentence may contain A2 :" + rawText);
                                System.out.println(frame.getResultIdx());
                            }
                            else
                            {
                                frame.setResult("");
                            }
                            System.out.println("");
                        }
                    }

                }
            } else {

            }
        }
    }

    public void labelA0Pattern1(ProcessFrame frame, ArrayList<Integer> allIdxs) throws IOException {
        String rawText = frame.getRawText();
        DependencyTree tree = depParser.parse(rawText);
        List<String> tokens = tokenizer.tokenize(rawText);
        String[] lexPattern = {"is", "are", "was", "were"};
        int undergoerLexIdx = -1;
        boolean stop = false;
        for (int i = 0; i < lexPattern.length && !stop; i++) {
            for (int j = 0; j < tokens.size() && !stop; j++) {
                String token = tokens.get(j);
                if (token.equalsIgnoreCase(lexPattern[i])) {
                    undergoerLexIdx = j;
                    stop = true;
                }
            }
        }
        if (stop) {
            ArrayList<Integer> triggerIdxs = frame.getTriggerIdx();
            //Collections.sort(triggerIdxs);
            boolean right = true;
            for (Integer idx : triggerIdxs) {
                if (idx < undergoerLexIdx) {
                    right = false;
                    break;
                }
            }
            if (right) {
                // get the ID of the trigger Lex in dependency parse
                // check if the trigger word is the parent of trigger Lex
                // if yes then follow PMOD, NMOD, PMOD, NMOD
                DependencyNode undergoerLexNode = tree.get(undergoerLexIdx + 1);
                ArrayList<DependencyNode> triggerNodes = tree.getNodes(triggerIdxs);
                if (tree.isExistPath(triggerNodes, undergoerLexNode)) {

                    System.out.println("This sentence may contain A0 :" + rawText + " Label " + undergoerLexNode.getRelationLabel());

                    //ArrayList<DependencyNode> rootSubTree = new ArrayList<DependencyNode>(tree.dependentsOf(undergoerLexNode));
                    /*if (rootSubTree.size() > 0) {
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
                     }*/
                }
            }

        }
    }

    public void labelA0Pattern2(ProcessFrame frame, ArrayList<Integer> allIdxs) throws IOException {
        String rawText = frame.getRawText();
        DependencyTree tree = depParser.parse(rawText);
        List<String> tokens = tokenizer.tokenize(rawText);
        String[] lexPattern = {"of"};
        int undergoerLexIdx = -1;
        boolean stop = false;
        for (int i = 0; i < lexPattern.length && !stop; i++) {
            for (int j = 0; j < tokens.size() && !stop; j++) {
                String token = tokens.get(j);
                if (token.equalsIgnoreCase(lexPattern[i])) {
                    undergoerLexIdx = j + 1;
                    stop = true;
                }
            }
        }
        if (stop) {
            ArrayList<Integer> triggerIdxs = frame.getTriggerIdx();
            //Collections.sort(triggerIdxs);
            boolean valid = false;
            for (Integer idx : triggerIdxs) {
                if (idx == undergoerLexIdx - 1) {
                    valid = true;
                    break;
                }
            }
            if (valid) {
                DependencyNode undergoerLexNode = tree.get(undergoerLexIdx);

                Set<DependencyNode> undergoerNodeCandidates = tree.dependentsOf(undergoerLexNode);
                if (undergoerNodeCandidates.size() > 0) {
                    ArrayList<DependencyNode> nodeList = Lists.newArrayList(undergoerNodeCandidates);
                    boolean NNFound = false;
                    DependencyNode rootSubTree = null;
                    for (int i = 0; i < nodeList.size(); i++) {
                        if (nodeList.get(i).getCpos().startsWith("NN")) {
                            NNFound = true;
                            rootSubTree = nodeList.get(i);
                        }
                    }
                    if (NNFound) {

                        ArrayList<DependencyNode> childs = getLimitedChilds(tree, rootSubTree);
                        Collections.sort(childs);
                        fixGap(childs, tree);
                        if (!childs.contains(rootSubTree)) {
                            childs.add(rootSubTree);
                        }

                        fixGap(childs, tree);
                        Collections.sort(childs);
                        Collections.sort(allIdxs);
                        ArrayList<Integer> candidateIdxs = getIdxs(childs);
                        ArrayList<Integer> intersections = ArrUtil.intersection(candidateIdxs, allIdxs);
                        Collections.sort(intersections);
                        //updateChildDueToIntersection(childs, intersections); //
                        if (intersections.size() == 0) {
                            String finalUndergoer = "";

                            for (int i = 0; i < childs.size(); i++) {
                                System.out.print(childs.get(i).getForm() + " ");
                                finalUndergoer = finalUndergoer.concat(childs.get(i).getForm() + " ");
                            }
                            finalUndergoer = finalUndergoer.trim();
                            frame.setUnderGoer(finalUndergoer);
                            frame.processRoleFillers();
                            if (frame.getUndergoerIdx().size() != 0) {
                                System.out.println("This sentence may contain A0 :" + rawText + "trigger : " + frame.getTrigger() + " Process : " + frame.getProcessName());
                                System.out.println(frame.getUndergoerIdx());
                            } else {
                                frame.setUnderGoer("");
                            }

                            System.out.println("");
                        }
                    }
                }
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
                                //System.out.print(childs.get(i).getForm() + " ");
                                finalEnabler = finalEnabler.concat(childs.get(i).getForm() + " ");
                            }
                            finalEnabler = finalEnabler.trim();
                            frame.setEnabler(finalEnabler);
                            frame.processRoleFillers();
                            //System.out.println(frame.getEnablerIdx());
                            if (frame.getEnablerIdx().size() > 0) {
                                System.out.println("This sentence may contain A1 :" + rawText);
                                frame.setEnabler(finalEnabler);
                                System.out.println(frame.getEnablerIdx());
                            } else {
                                frame.setEnabler("");
                            }
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
                ArrayList<Integer> undergoerIdxs = frames.get(i).getUndergoerIdx();
                if (enablerIdxs.size() == 0 ) {
                    labelA1(frames.get(i), labeledIdx);

                }
                if (resultIdxs.size() == 0  ) {
                    labelA2(frames.get(i), labeledIdx);

                }
                if (undergoerIdxs.size() == 0 ) {
                    //labelA0Pattern1(frames.get(i), labeledIdx);
                    //labeledIdx = frames.get(i).getAllLabeledIndex();
                    labelA0Pattern2(frames.get(i), labeledIdx);

                }
            } catch (Exception e) {

            }
        }

        ProcessFrameUtil.dumpFramesToFile(frames, outFileName);
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, ClassNotFoundException {

        PatternBasedAnnotator annotator = new PatternBasedAnnotator(GlobalV.PROJECT_DIR + "/data/ds_most_frequent_7_06_2015/ds_all_processes.tsv");
        annotator.annotate(GlobalV.PROJECT_DIR + "/data/ds_most_frequent_7_06_2015/ds_all_processes_w_pattern.tsv");

        //PatternBasedAnnotator annotator = new PatternBasedAnnotator(GlobalVariable.PROJECT_DIR + "/data/smallA0.tsv");
        //annotator.annotate(GlobalVariable.PROJECT_DIR + "/data/smallA0Annotated.tsv");
    }
}
