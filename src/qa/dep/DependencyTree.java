/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.dep;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.io.Serializable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import qa.WordNet;
import se.lth.cs.srl.corpus.Word;
import static se.lth.cs.srl.corpus.Word.pathToRoot;

/**
 * Dependency tree.
 *
 * @author Ritwik Banerjee
 */
public class DependencyTree extends TreeMap<Integer, DependencyNode> implements Serializable {

    /**
     * Constructs a dependency tree comprising of a single artificial root node.
     */
    public DependencyTree() {
        put(DependencyNode.ROOT_ID, new DependencyNode());
    }

    /**
     * Creates a dependency tree from its CoNLL-X representation.
     */
    public static DependencyTree fromCoNLLFormatString(String conllFormatString) {
        DependencyTree tree = new DependencyTree();
        String[] conllFormatLines = conllFormatString.split("\n");
        for (String line : conllFormatLines) {
            DependencyNode node = DependencyNode.fromCoNLLFormatString(line);
            tree.put(node.getId(), node);
        }
        return tree;
    }

    public DependencyNode getSubjectOf(DependencyNode predicateNode) {
        DependencyNode sbjNode = null;
        if (hasDependentsOfType(predicateNode, "nsubj")) {
            sbjNode = childOfNodeByRelation(predicateNode, "nsubj");
        } else if (hasDependentsOfType(predicateNode, "nsubjpass")) {
            sbjNode = childOfNodeByRelation(predicateNode, "nsubjpass");
        }

        return sbjNode;
    }

    public DependencyNode getObjectOf(DependencyNode predicateNode) {
        DependencyNode objNode = null;
        if (hasDependentsOfType(predicateNode, "dobj")) {
            objNode = childOfNodeByRelation(predicateNode, "dobj");
        } else if (hasDependentsOfType(predicateNode, "pobj")) {
            objNode = childOfNodeByRelation(predicateNode, "pobj");
        }

        return objNode;
    }

    /**
     * Returns the relation label of the node closest to the one with the given
     * id that is a nominal subject.
     */
    public String getNearestSubjectRelationLabel(int id) {
        List<DependencyNode> subjectNodes = values().stream()
                .filter(node -> !node.getForm().equals("ROOT")
                        && node.getRelationLabel().matches("(?i).*subj.*"))
                .collect(Collectors.toList());

        int diff = Integer.MAX_VALUE;
        DependencyNode nearestSubjectNode = subjectNodes.isEmpty() ? null : subjectNodes.get(0);

        for (DependencyNode node : subjectNodes) {
            if (Math.abs(node.getId() - id) < diff) {
                diff = Math.abs(node.getId() - id);
                nearestSubjectNode = node;
            }
        }

        return nearestSubjectNode == null ? "" : nearestSubjectNode.getRelationLabel();
    }

    /**
     * Returns the previous (i.e. to the left of node with given id) node with a
     * specified POS tag
     */
    public DependencyNode getPreviousNodeWithCoarsePOS(int id, String tag) {
        for (int node_id = id - 1; node_id > 0; node_id--) {
            if (get(node_id).getPos().equals(tag)) {
                return get(node_id);
            }
        }
        return null;
    }

    public DependencyNode getPreviousNodeWithCoarsePOS(DependencyNode node, String tag) {
        return getPreviousNodeWithCoarsePOS(node.getId(), tag);
    }

    public DependencyNode getNextNodeWithCoarsePOS(int id, String tag) {
        for (int node_id = id + 1; node_id < lastKey(); node_id++) {
            if (get(node_id).getPos().equals(tag)) {
                return get(node_id);
            }
        }
        return null;
    }

    public DependencyNode getHeadNode(ArrayList<Integer> tokenIdx) {
        ArrayList<DependencyNode> headCandidates = new ArrayList<DependencyNode>();

        if (tokenIdx.size() == 1) {
            headCandidates.add(get(tokenIdx.get(0)));
            return headCandidates.get(0);
        } else {
            for (int i = 0; i < tokenIdx.size(); i++) {
                DependencyNode node = get(tokenIdx.get(i));
                if ((node.getCpos().startsWith("VB") && !node.getRelationLabel().equalsIgnoreCase("cop")) || node.getCpos().startsWith("NN")) {
                    headCandidates.add(node);
                }
            }
        }

        if (headCandidates.size() == 0) {
            for (int i = 0; i < tokenIdx.size(); i++) {
                headCandidates.add(get(tokenIdx.get(i)));
            }
        }
        for (int i = 0; i < headCandidates.size(); i++) {
            boolean allAreChildren = true;
            for (int j = 0; j < headCandidates.size(); j++) {
                if (i != j) {
                    if (!isExistPathFrom(headCandidates.get(j), headCandidates.get(i))) {
                        allAreChildren = false;
                    }
                }
                if (!allAreChildren) {
                    break;
                }
            }
            if (allAreChildren) {
                return headCandidates.get(i);
            }
        }
        int maxChildren = Integer.MIN_VALUE;
        DependencyNode nodeWithMostChildren = null;
        for (int i = 0; i < headCandidates.size(); i++) {
            DependencyNode currentNode = headCandidates.get(i);
            if (getAllChildren(currentNode).size() > maxChildren) {
                nodeWithMostChildren = currentNode;
            }
        }
        return nodeWithMostChildren;
    }

    public DependencyNode getNextNodeWithCoarsePOS(DependencyNode node, String tag) {
        return getNextNodeWithCoarsePOS(node.getId(), tag);
    }

    public ArrayList<DependencyNode> getNode(ArrayList<String> words) {
        ArrayList<DependencyNode> nodes = new ArrayList<DependencyNode>();

        for (String word : words) {
            for (Map.Entry<Integer, DependencyNode> entry : this.entrySet()) {
                if (entry.getValue().getForm().equalsIgnoreCase(word)) {
                    nodes.add(entry.getValue());
                }
            }
        }
        return nodes;
    }

    public ArrayList<DependencyNode> pathToRoot(DependencyNode node) {
        ArrayList<DependencyNode> path;
        if (node.getId() == 0) {
            path = new ArrayList<DependencyNode>();
            path.add(node);
            return path;
        }
        path = pathToRoot(get(node.getHeadID()));
        path.add(node);
        return path;
    }

    /*public static List<Word> findPath(Word pred, Word arg) {
     List<Word> predPath = pathToRoot(pred);
     List<Word> argPath = pathToRoot(arg);
     List<Word> ret = new ArrayList<Word>();

     int commonIndex = 0;
     int min = (predPath.size() < argPath.size() ? predPath.size() : argPath.size());
     for (int i = 0; i < min; ++i) {
     if (predPath.get(i) == argPath.get(i)) { //Always true at root (ie first index)
     commonIndex = i;
     }
     }
     for (int j = predPath.size() - 1; j >= commonIndex; --j) {
     ret.add(predPath.get(j));
     }
     for (int j = commonIndex + 1; j < argPath.size(); ++j) {
     ret.add(argPath.get(j));
     }
     return ret;
     }*/
    public ArrayList<DependencyNode> findPath(DependencyNode srcNode, DependencyNode targetNode) {
        ArrayList<DependencyNode> srcPath = pathToRoot(srcNode);
        ArrayList<DependencyNode> targetPath = pathToRoot(targetNode);
        ArrayList<DependencyNode> ret = new ArrayList<DependencyNode>();

        int commonIndex = 0;
        int min = (srcPath.size() < targetPath.size() ? srcPath.size() : targetPath.size());
        for (int i = 0; i < min; ++i) {
            if (srcPath.get(i) == targetPath.get(i)) {
                commonIndex = i;
            }
        }
        for (int j = srcPath.size() - 1; j >= commonIndex; --j) {
            ret.add(srcPath.get(j));
        }
        for (int j = commonIndex + 1; j < targetPath.size(); ++j) {
            ret.add(targetPath.get(j));
        }

        return ret;
    }

    public String getDepRelPath(DependencyNode src, DependencyNode target) {

        boolean up = true;
        List<DependencyNode> path = findPath(src, target);

        if (path.size() == 1) {
            return "SELF";
        }
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < (path.size() - 1); ++i) {
            DependencyNode node = path.get(i);
            ret.append(node.getRelationLabel());
            if (up) {
                if (node.getHeadID() == path.get(i + 1).getId()) { //Arrow up
                    ret.append("1");
                } else {
                    ret.append("0");
                    up = false;
                }
            } else {
                ret.append("0");
            }
        }
        return ret.toString();
    }

    public Set<String> getChildWordSet(DependencyNode node) {
        Set<DependencyNode> children = dependentsOf(node);
        Set<String> childWordSet = new HashSet<String>();
        for (DependencyNode childNode : children) {
            childWordSet.add(childNode.getForm());
        }

        return childWordSet;
    }

    public Set<String> getChildPOSSet(DependencyNode node) {
        Set<DependencyNode> children = dependentsOf(node);
        Set<String> childPOSSet = new HashSet<String>();
        for (DependencyNode childNode : children) {
            childPOSSet.add(childNode.getCpos());
        }

        return childPOSSet;
    }

    public String getPOSPath(DependencyNode src, DependencyNode target) {
        boolean up = true;
        List<DependencyNode> path = findPath(src, target);
        if (path.size() == 1) {
            return "SELF";
        }
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < (path.size() - 1); ++i) {
            DependencyNode node = path.get(i);
            ret.append(node.getCpos());
            if (up) {
                if (node.getHeadID() == path.get(i + 1).getId()) { //Arrow up
                    ret.append("1");
                } else {
                    ret.append("0");
                    up = false;
                }
            } else {
                ret.append("0");
            }
        }
        return ret.toString();
    }

    public boolean isExistPathFrom(DependencyNode from, DependencyNode to) {
        DependencyNode currentNode = from;
        boolean found = false;
        while (!currentNode.isRoot() && !found) {
            if (currentNode.getId() == to.getId()) {
                found = true;
            } else {
                currentNode = this.get(currentNode.getHeadID());
            }
        }

        return found;
    }

    public DependencyNode get(int id) {
        try {
            return super.get(id);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public DependencyNode headOf(DependencyNode node) {
        return get(node.getHeadID());
    }

    public DependencyNode headOf(int node_id) {
        return get(get(node_id).getHeadID());
    }

    public Set<DependencyNode> dependentsOf(DependencyNode node) {
        return dependentsOf(node.getId());
    }

    public Set<DependencyNode> dependentsOf(int node_id) {
        Predicate<DependencyNode> isDependent = n -> n.getHeadID() == node_id;
        return values().stream().filter(isDependent).collect(Collectors.toSet());
    }

    public Set<DependencyNode> dependentsOfType(DependencyNode node, String type) {
        return dependentsOfType(node.getId(), type);
    }

    public Set<DependencyNode> dependentsOfType(int node_id, String type) {
        Predicate<DependencyNode> hasType = n -> n.getRelationLabel().equals(type);
        return dependentsOf(node_id).stream().filter(hasType).collect(Collectors.toSet());
    }

    public Set<DependencyNode> dependentsWithPOSTag(int node_id, String pos) {
        Predicate<DependencyNode> hasTag = n -> n.getPos().equals(pos);
        return dependentsOf(node_id).stream().filter(hasTag).collect(Collectors.toSet());
    }

    public boolean hasDependentsOfType(DependencyNode node, String type) {
        return dependentsOfType(node, type).size() > 0;
    }

    /**
     * Prints this dependency tree in the CoNLL-X format.
     *
     * @return The CoNLL-X format representation of this dependency tree.
     */
    public String toString() {
        OptionalInt max_tokensize_optint = values().stream().map(DependencyNode::getForm).mapToInt(String::length).max();
        int max_tokensize = max_tokensize_optint.isPresent() ? max_tokensize_optint.getAsInt() : 0;
        int formwidth = max_tokensize + 2;
        List<String> conllOutput = new ArrayList<>();
        for (int i : navigableKeySet()) {
            if (i == 0) {
                continue;
            }
            DependencyNode node = get(i);
            String form = Strings.padEnd(node.getForm(), formwidth, ' ');
            String lemma = Strings.padEnd(node.getLemma(), 6, ' ');
            String cpos = Strings.padEnd(node.getCpos(), 6, ' ');
            String pos = Strings.padEnd(node.getPos(), 6, ' ');
            String rel = Strings.padEnd(node.getRelationLabel(), 6, ' ');
            conllOutput.add(Joiner.on("    ").join(node.getId(), form, lemma, cpos, "_", node.getHeadID(), rel, "_", "_"));
        }
        return Joiner.on('\n').join(conllOutput);
    }

    /*public String toRawString() {
     StringBuilder sentenceBuilder = new StringBuilder();
     for (int i = 1; i < lastKey(); i++) {
     String curr_token = get(i).getForm();
     if (!(curr_token.equals("-") || get(i-1).getForm().endsWith("-") || PUNCTUATION_STR.contains(curr_token)))
     sentenceBuilder.append(" ");
     sentenceBuilder.append(curr_token);
     }
     return sentenceBuilder.toString();
     }*/
    public DependencyNode childOfNodeByRelation(DependencyNode node, String relation) {
        Predicate<DependencyNode> IsMatchingChild = tnode -> tnode.getId() > DependencyNode.ROOT_ID
                && tnode.getHeadID() == node.getId()
                && (relation == null || tnode.getRelationLabel().equals(relation));
        DependencyNode child = null;
        try {
            Optional<DependencyNode> optionalNode = values().stream().filter(IsMatchingChild).findFirst();
            if (optionalNode.isPresent()) {
                child = optionalNode.get();
            }
        } catch (NullPointerException ignore) { /* NullPointerException will be thrown if the 'node' arg is null */ }

        return child;
    }

    public Set<DependencyNode> childrenOfNodeByRelation(DependencyNode node, String relation) {
        Predicate<DependencyNode> IsMatchingChild = tnode -> tnode.getId() > DependencyNode.ROOT_ID
                && tnode.getHeadID() == node.getId()
                && (relation == null || tnode.getRelationLabel().equals(relation));
        Set<DependencyNode> children = new HashSet<>();
        try {
            children = values().stream().filter(IsMatchingChild).collect(Collectors.toSet());
        } catch (NullPointerException ignore) { /* NullPointerException will be thrown if the 'node' arg is null */ }

        return children;
    }

    /**
     * Finds and returns the {@code ID} of the first {@code DependencyNode} in
     * this {@code DependencyTree} with a specified POS tag, which may be fine
     * or coarse, and occurring after a specified {@code pivot} ID.
     *
     * @param pivot         <code>ID</code> of node whose successor is searched.
     * @param partOfSpeech The part-of-speech tag to be matched.
     * @param coarse If <code>true</code>, matches the coarse POS tag, else
     * matches the fine-grained tag.
     * @return The first node with the specified POS tag occurring after the
     * node with the given {@code id}. Returns -1 if no such node is present.
     */
    public int getSucceedingNodeWithPOS(int pivot, String partOfSpeech, boolean coarse) {
        for (int index = pivot + 1; index <= lastKey(); index++) {
            String indexPOS = coarse ? get(index).getCpos() : get(index).getPos();
            if (indexPOS.equals(partOfSpeech)) {
                return index;
            }
        }
        return -1; // no node with the given POS tag appears after the pivot
    }

    /**
     * Finds and returns the {@code ID} of the last {@code DependencyNode} in
     * this {@code DependencyTree} with a specified POS tag, which may be fine
     * or coarse, and occurring before a specified {@code pivot} ID.
     *
     * @param pivot         <code>ID</code> of node whose predecessor is searched.
     * @param partOfSpeech The part-of-speech tag to be matched.
     * @param coarse If <code>true</code>, matches the coarse POS tag, else
     * matches the fine-grained tag.
     * @return The last node with the specified POS tag occurring before the
     * node with the given {@code id}. Returns -1 if no such node is present.
     */
    public int getPrecedingNodeWithPOS(int pivot, String partOfSpeech, boolean coarse) {
        for (int index = pivot - 1; index >= firstKey(); index--) {
            String indexPOS = coarse ? get(index).getCpos() : get(index).getPos();
            if (indexPOS.equals(partOfSpeech)) {
                return index;
            }
        }
        return -1;
    }

    public static void removeSubtree(Map<Integer, DependencyNode> tree, DependencyNode root) {
        List<DependencyNode> dependents = new ArrayList<>();
        try {
            dependents = tree.values()
                    .stream()
                    .filter(n -> n.getId() > DependencyNode.ROOT_ID && n.getHeadID() == root.getId())
                    .collect(Collectors.toList());
        } catch (NullPointerException ignore) { /* NullPointerException will be thrown if the 'node' arg is null */ }

        for (DependencyNode dependent : dependents) {
            removeSubtree(tree, dependent);
        }
        tree.remove(root.getId(), root);
    }

    public boolean isNounModifier(DependencyNode node) {
        if (node.isRoot()) {
            return false;
        }

        if (node.getPos().contains("NN")) {
            DependencyNode parent = this.get(node.getHeadID());
            if (parent.getPos().contains("NN")) {
                return true;
            }
        }
        return false;
    }

    public int getWordIdx(String word) {
        for (int i = firstKey(); i <= lastKey(); i++) {
            if (get(i).getForm().equalsIgnoreCase(word)) {
                return i;
            }
        }

        return -1;
    }

    public DependencyNode getWordDepNode(String word) {
        for (int i = firstKey(); i <= lastKey(); i++) {
            if (get(i).getForm().equalsIgnoreCase(word)) {
                return get(i);
            }
        }
        return null;
    }

    public DependencyNode getDepNodeContains(String word) {
        for (int i = firstKey(); i <= lastKey(); i++) {
            if (get(i).getForm().contains(word)) {
                return get(i);
            }
        }
        return null;
    }

    public ArrayList<String> getWordMatchType(ArrayList<DependencyNode> trigger, String[] types, WordNet wn) {
        ArrayList<String> argMatches = new ArrayList<String>();
        for (DependencyNode node : trigger) {
            for (Map.Entry<Integer, DependencyNode> entry : this.entrySet()) {
                if (!entry.getValue().getForm().equalsIgnoreCase(node.getForm())) {
                    if (!entry.getValue().isRoot() && !isNounModifier(entry.getValue()) && isExistPathFrom(entry.getValue(), node) && wn.isMatchType(entry.getValue().getLemma().toLowerCase(), types)) {
                        if (!argMatches.contains(entry.getValue().getForm())) {
                            argMatches.add(entry.getValue().getForm());
                        }
                    }
                }

            }
        }
        return argMatches;
    }

    public boolean isExistPath(ArrayList<DependencyNode> from, DependencyNode to) {
        for (int i = 0; i < from.size(); i++) {
            if (isExistPathFrom(from.get(i), to)) {
                return true;
            }
        }

        return false;
    }

    public boolean isExistPath(DependencyNode from, ArrayList<DependencyNode> to) {
        for (int i = 0; i < to.size(); i++) {
            if (isExistPathFrom(from, to.get(i))) {
                return true;
            }
        }

        return false;
    }

    public boolean isExistPath(ArrayList<DependencyNode> from, ArrayList<DependencyNode> to) {
        for (int i = 0; i < from.size(); i++) {
            for (int j = 0; j < to.size(); j++) {
                if (isExistPathFrom(from.get(i), to.get(j)) || isExistPathFrom(to.get(j), from.get(i))) {
                    return true;
                }
            }
        }

        return false;
    }

    public ArrayList<DependencyNode> getNodes(ArrayList<Integer> indexes) {
        ArrayList<DependencyNode> nodes = new ArrayList<DependencyNode>();
        for (int i = 0; i < indexes.size(); i++) {
            nodes.add(get(indexes.get(i)));
        }
        return nodes;
    }

    public ArrayList<DependencyNode> getAllChildren(DependencyNode root) {
        ArrayList<DependencyNode> result = new ArrayList<DependencyNode>();
        Stack<DependencyNode> stack = new Stack<DependencyNode>();

        Set<DependencyNode> children = dependentsOf(root);
        stack.addAll(children);
        result.addAll(children);
        while (!stack.isEmpty()) {
            DependencyNode currentNode = stack.pop();
            children = dependentsOf(currentNode);
            result.addAll(children);
            stack.addAll(children);
        }
        return result;
    }

    public Set<String> getChildDEPSet(DependencyNode node) {
        Set<DependencyNode> children = dependentsOf(node);
        Set<String> childDepRelSet = new HashSet<String>();
        for (DependencyNode childNode : children) {
            childDepRelSet.add(childNode.getRelationLabel());
        }

        return childDepRelSet;
    }

    public String getLemmaVerb(boolean leftDirection, int windowSize, int currentTokenIdx) {
        if (leftDirection) {
            int step = 1;
            for (int i = currentTokenIdx - 1; i > 0 && step < windowSize; i--) {
                if (get(i).getCpos().startsWith("VB")) {
                    return get(i).getLemma();
                }
                step++;
            }
        } else {
            int step = 1;
            for (int i = currentTokenIdx + 1; i < lastKey() && step < windowSize; i++) {
                if (get(i).getCpos().startsWith("VB")) {
                    return get(i).getLemma();
                }
                step++;
            }
        }

        return "";
    }
}
