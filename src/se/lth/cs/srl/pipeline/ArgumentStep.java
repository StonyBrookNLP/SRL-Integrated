package se.lth.cs.srl.pipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import se.lth.cs.srl.Learn;
import se.lth.cs.srl.Parse;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.features.Feature;
import se.lth.cs.srl.features.FeatureSet;
import se.lth.cs.srl.ml.LearningProblem;
import se.lth.cs.srl.ml.Model;
import se.lth.cs.srl.ml.liblinear.Label;

public abstract class ArgumentStep extends AbstractStep {

    public ArgumentStep(FeatureSet fs) {
        super(fs);
    }

    protected abstract Integer getLabel(Predicate pred, Word arg);

    protected String getPOSPrefix(String pos) {
        for (String prefix : featureSet.POSPrefixes) {
            if (pos.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    //TODO this way we lookup the POSPrefix several times for the same predicate. Maybe the second parameter should be Iterable<Word> ?
    protected void addInstance(Predicate pred, Word arg) {
        String POSPrefix = getPOSPrefix(pred.getPOS());
        if (POSPrefix == null) {
            if (Learn.learnOptions.skipNonMatchingPredicates) {
                return;
            } else {
                POSPrefix = featureSet.POSPrefixes[0];
            }
        }
        LearningProblem lp = learningProblems.get(POSPrefix);
        Collection<Integer> indices = collectIndices(pred, arg, POSPrefix, new TreeSet<Integer>());
        if (indices != null) {
            lp.addInstance(getLabel(pred, arg), indices);
        }

    }

    protected Collection<Integer> collectIndices(Predicate pred, Word arg, String POSPrefix, Collection<Integer> indices) {
        if (POSPrefix == null) {
            return null;
        }
        Integer offset = 0;
        for (Feature f : featureSet.get(POSPrefix)) {
            f.addFeatures(indices, pred, arg, offset, false);
            offset += f.size(false);
        }

        List<Feature> features = featureSet.get(POSPrefix);
        int featureSize = 0;
        for (Feature f : features) {
            featureSize += f.size(false);
        }
        if (Learn.learnOptions != null && !Learn.learnOptions.domainAdaptation) {
            return indices;
        } else if (Learn.learnOptions != null && Learn.learnOptions.domainAdaptation) {
            if (Pipeline.isSRC) {
                ArrayList<Integer> modIdx = new ArrayList<Integer>();
                for (Integer i : indices) {
                    modIdx.add(i + featureSize);
                }
                indices.addAll(modIdx);
            } else { // Target 
                ArrayList<Integer> modIdx = new ArrayList<Integer>();
                for (Integer i : indices) {
                    modIdx.add(2 * featureSize + i);
                }
                indices.addAll(modIdx);
            }

        } else if (Parse.parseOptions.domainAdaptation) {
            ArrayList<Integer> modIdx = new ArrayList<Integer>();
            for (Integer i : indices) {
                modIdx.add(2 * featureSize + i);
            }
            indices.addAll(modIdx);
        }

        return indices;
    }

    //TODO same thing as above.
    public Integer classifyInstance(Predicate pred, Word arg) {
        String POSPrefix = getPOSPrefix(pred.getPOS());
        if (POSPrefix == null) {
            POSPrefix = featureSet.POSPrefixes[0];
            System.out.println("Unknown POS-tag for predicate '" + pred.getForm() + "', falling back to " + POSPrefix);
        }
        Model m = models.get(POSPrefix);

        Collection<Integer> indices = collectIndices(pred, arg, POSPrefix, new TreeSet<Integer>());
        return m.classify(indices);
    }

    // s.louvan
    public List<Label> classifyAllProbs(Predicate pred, Word arg) {
        String POSPrefix = getPOSPrefix(pred.getPOS());
        if (POSPrefix == null) {
            POSPrefix = featureSet.POSPrefixes[0];
            System.out.println("Unknown POS-tag for predicate '" + pred.getForm() + "', falling back to " + POSPrefix);
        }
        Model m = models.get(POSPrefix);

        Collection<Integer> indices = collectIndices(pred, arg, POSPrefix, new TreeSet<Integer>());
        return  m.classifyProb(indices);
    }
}
