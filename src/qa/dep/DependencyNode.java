/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.dep;

import com.google.common.base.Joiner;
import java.util.Arrays;

/**
 * Dependency node. Adheres to the nomenclature specified CoNLL-X shared task.
 * Feats, phead and pdeprel are ignored. See
 * <a target="_blank" href="http://ilk.uvt.nl/conll/#dataformat">CoNLL-X data
 * format</a> for details.
 *
 * @author Ritwik Banerjee
 */
public class DependencyNode implements Comparable {

    public static final int ROOT_ID = 0;
    public static final String ROOT_FORM = "ROOT";
    public static final String ROOT_DEPREL = null;

    private int id; // token counter, starting at 1 for each new sentence
    private String form; // word form or puctuation symbol
    private String lemma; // lemma of word form
    private String cpos; // coarse-grained part-of-speech tag
    private String pos; // fine-grained part-of-speech tag (can be identical to the coarse-grained tag)
    private int head_id; // id of the current token's head_id
    private String deprel; // dependency relation to the head_id

    /**
     * Constructs an artificial root node.
     */
    public DependencyNode() {
        this(ROOT_ID, ROOT_FORM, ROOT_FORM, ROOT_FORM, ROOT_FORM, ROOT_ID, ROOT_DEPREL);
    }

    /**
     * Constructs a dependency node.
     */
    private DependencyNode(int id, String form, String lemma, String cpos, String pos, int head_id, String deprel) {
        this.id = id;
        this.lemma = lemma;
        this.form = form;
        this.cpos = cpos;
        this.pos = pos;
        this.head_id = head_id;
        this.deprel = deprel;
    }

    /**
     * Creates a dependency node from a single line in the CoNLL-X format.
     * Lemma, feats, phead and pdeprel are ignored.
     */
    public static DependencyNode fromCoNLLFormatString(String conllFormatString) {
        String[] fields = conllFormatString.trim().split("(\\s|\\t)+");
        //System.out.println(Arrays.toString(fields));
        return new DependencyNode(Integer.parseInt(fields[0]), fields[1], fields[2], fields[3], fields[4], Integer.parseInt(fields[5]), fields[6]);
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public String getCpos() {
        return cpos;
    }

    public void setCpos(String cpos) {
        this.cpos = cpos;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public int getHeadID() {
        return head_id;
    }

    public void setHeadID(int head_id) {
        this.head_id = head_id;
    }

    public String getRelationLabel() {
        return deprel;
    }

    public void setRelationLabel(String deprel) {
        this.deprel = deprel;
    }

    public boolean isRoot() {
        return id == ROOT_ID;
    }

    public String toString() {
        return Joiner.on("    ").join(id,
                form,
                lemma == null ? "_" : lemma,
                cpos == null ? "_" : cpos,
                pos == null ? "_" : pos,
                head_id,
                deprel == null ? "_" : deprel);
    }

    /**
     * @return {@code true} if and only if the POS tag is a wh-determiner,
     * wh-pronoun or wh-adverb
     */
    public boolean hasWhPOSTag() {
        return pos.startsWith("W");
    }

    @Override
    public int compareTo(Object other) {
        DependencyNode otherDepNode = (DependencyNode) other;
        if (this.getId() < otherDepNode.getId()) {
            return -1;
        }
        if (this.getId() > otherDepNode.getId()) {
            return 1;
        }
        return 0;
    }

    public boolean equals(Object other) {
        DependencyNode otherDepNode = (DependencyNode) other;
        return this.getId() == otherDepNode.getId();
    }
}
