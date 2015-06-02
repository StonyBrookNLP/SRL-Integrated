package clear.util;


public class FunctionUtil {
	
	public static double getConfidenceScoreSoftMax(double[] scores)
	{
		double exps[] = new double[scores.length];
		double sum = 0;
		
		// Transform the score
		for (int i = 0; i < scores.length; i++)
		{
			exps[i] = Math.pow(Math.E, scores[i]);
			sum += exps[i];
		}
		
		double max = Double.MIN_VALUE;
		
		for (int i = 0; i < scores.length; i++)
		{
			exps[i] = exps[i] / sum;
			max = Math.max(max, exps[i]);
		}
		
		return max;
	}
	
	public static void main(String[] args)
	{
		double[] scores = {-2.85, 0.86, 0.28};
		System.out.println(getConfidenceScoreSoftMax(scores));
	}
}
