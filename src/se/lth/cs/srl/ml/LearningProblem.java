package se.lth.cs.srl.ml;

import java.util.Collection;

public interface LearningProblem {

	public void addInstance(int label,Collection<Integer> indices);
        public void addInstance(int label, Collection<Integer> indices, int featureSize, boolean isSRC);
	public void done();
	public Model train();
}