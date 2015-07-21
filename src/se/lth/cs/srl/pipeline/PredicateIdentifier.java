package se.lth.cs.srl.pipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import static java.util.stream.Collectors.toList;

import se.lth.cs.srl.Learn;
import se.lth.cs.srl.Parse;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.features.Feature;
import se.lth.cs.srl.features.FeatureSet;
import se.lth.cs.srl.ml.LearningProblem;
import se.lth.cs.srl.ml.Model;
import se.lth.cs.srl.ml.liblinear.Label;

public class PredicateIdentifier extends AbstractStep {

    private static final String FILEPREFIX = "pi_";

    public PredicateIdentifier(FeatureSet fs) {
        super(fs);
    }

    public void extractInstances(Sentence s) {
        /*
         * We add an instance if it
         * 1) Is a predicate. Then either to its specific classifier, or the fallback one. (if fallback behavior is specified, i.e. skipNonMatchingPredicates=false
         * 2) Is not a predicate, but matches the POS-tag
         */
        for (int i = 1, size = s.size(); i < size; ++i) {
            Word potentialPredicate = s.get(i);
            String POS = potentialPredicate.getPOS();
            String POSPrefix = null;
            for (String prefix : featureSet.POSPrefixes) {
                if (POS.startsWith(prefix)) {
                    POSPrefix = prefix;
                    break;
                }
            }
            if (POSPrefix == null) { //It matches a prefix, we will use it for sure.
                if (!Learn.learnOptions.skipNonMatchingPredicates && potentialPredicate instanceof Predicate) {
                    POSPrefix = featureSet.POSPrefixes[0];
                } else {
                    continue; //Its just some word we dont care about
                }
            }
            Integer label = potentialPredicate instanceof Predicate ? POSITIVE : NEGATIVE;
            addInstance(s, i, POSPrefix, label);
        }
    }

    private void addInstance(Sentence s, int i, String POSPrefix, Integer label) {
        LearningProblem lp = learningProblems.get(POSPrefix);
        Collection<Integer> indices = new TreeSet<Integer>();
        Integer offset = 0;
        int featureSize = 0;
        for (Feature f : featureSet.get(POSPrefix)) {
            featureSize += f.size(true);
        }
        for (Feature f : featureSet.get(POSPrefix)) {
            f.addFeatures(s, indices, i, -1, offset, true);
            offset += f.size(true);
        }
        lp.addInstance(label, indices);

        /*if (!Learn.learnOptions.domainAdaptation) {
            lp.addInstance(label, indices);
        } else {
            if (Pipeline.isSRC) {
                lp.addInstance(label, indices, featureSize, true);
            } else {
                lp.addInstance(label, indices, featureSize, false);
            }
        }*/
    }

    // s.louvan
    public void parse(Sentence s) {
        boolean predicateSet = false;
        HashMap<Integer,List<Label>> predicateCandidate = new HashMap<Integer,List<Label>>();
        for (int i = 1, size = s.size(); i < size; ++i) {
            Integer label = classifyInstance(s, i);
            List<Label> probs = classifyAllProbs(s, i);
            if (probs != null)
                predicateCandidate.put(i,probs);
            if (label.equals(POSITIVE)) {
                s.makePredicate(i);
                predicateSet = true;
            }
        }
        if (!predicateSet && predicateCandidate.size() > 0)
        {
            
            double maxConf = Double.MIN_VALUE;
            int maxIndex = -1;
            for (int i : predicateCandidate.keySet())
            {
                List<Label> posLabel = predicateCandidate.get(i).stream().filter(pred -> pred.getLabel().equals( POSITIVE)).collect(toList());
                System.out.println("SIZE : "+posLabel.size()+" "+posLabel);
                if (posLabel.get(0).getProb() > maxConf)
                {
                    maxIndex = i;
                    maxConf = posLabel.get(0).getProb();
                }
            }
            s.makePredicate(maxIndex);
            
            predicateSet = true;
        }
        
        if (!predicateSet)
            System.out.println("PREDICATE IS STILL NOT SET");
    }

    private Integer classifyInstance(Sentence s, int i) {
        String POSPrefix = null;
        String POS = s.get(i).getPOS();
        for (String prefix : featureSet.POSPrefixes) {
            if (POS.startsWith(prefix)) {
                POSPrefix = prefix;
                break;
            }
        }
        if (POSPrefix == null) {
            return NEGATIVE;
        }
        Model m = models.get(POSPrefix);
        Collection<Integer> indices = new TreeSet<Integer>();
        Integer offset = 0;
        for (Feature f : featureSet.get(POSPrefix)) {
            f.addFeatures(s, indices, i, -1, offset, true);
            offset += f.size(true);
        }

        if (Parse.parseOptions.domainAdaptation) {

        }
        
        return m.classify(indices);
    }
    
    //s.louvan
    private List<Label> classifyAllProbs(Sentence s, int i)
    {
        String POSPrefix = null;
        String POS = s.get(i).getPOS();
        for (String prefix : featureSet.POSPrefixes) {
            if (POS.startsWith(prefix)) {
                POSPrefix = prefix;
                break;
            }
        }
        if (POSPrefix == null) {
            return null;
        }
        Model m = models.get(POSPrefix);
        Collection<Integer> indices = new TreeSet<Integer>();
        Integer offset = 0;
        for (Feature f : featureSet.get(POSPrefix)) {
            f.addFeatures(s, indices, i, -1, offset, true);
            offset += f.size(true);
        }

        return m.classifyProb(indices);
    }

    @Override
    public void prepareLearning() {
        super.prepareLearning(FILEPREFIX);
    }

    @Override
    protected String getModelFileName() {
        return FILEPREFIX + ".models";
    }

}
