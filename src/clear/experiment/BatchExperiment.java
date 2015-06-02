package clear.experiment;

import java.io.File;
import java.util.Arrays;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import clear.engine.SRLPredict;
import clear.engine.SRLTrain;
import clear.util.FileUtil;

public class BatchExperiment {
	public static void main(String[] args) throws InterruptedException
	{
		// For each sample.clearparser file, build a model
		String dirName = "/Users/samuellouvan/NetBeansProjects/QA/data/sublimation/";
		String ds_ClearParser = "sublimation_ds.clearparser";
		String ds_model = "sublimation_ds.model";
		String testingFile  = "sublimation.clearparser";
		String predictionFile = "sublimation_ds.predict";
		File folder  = new File(dirName);
		File[] files = folder.listFiles();
		
		String [] trainingArgs = {"-c", "/Users/samuellouvan/Downloads/clearparser-read-only/config/config_srl_en.xml", 
				                  "-i", dirName+ ds_ClearParser, 
								  "-t", "/Users/samuellouvan/Downloads/clearparser-read-only/config/feature_srl_en_conll09.xml", 
								  "-m", dirName+ds_model};
		


		for (File file: files)
		{
			if (file.getName().contains("ds") && file.getName().contains(".clearparser"))
			{
				SRLTrain train = new SRLTrain();
				CmdLineParser cmd = new CmdLineParser(train);
				
				try
				{
					trainingArgs[3] = dirName+file.getName();
					trainingArgs[7] = dirName+FileUtil.getFileNameWoExt(file.getName())+".model";
					System.out.println(Arrays.toString(trainingArgs));
					cmd.parseArgument(trainingArgs);
					train.init();
					train.train();
				}
				catch (CmdLineException e)
				{
					System.err.println(e.getMessage());
					cmd.printUsage(System.err);
				}
				catch (Exception e) {e.printStackTrace();}
			}
		}
		
		String [] predictArgs = {"-c", "/Users/samuellouvan/Downloads/clearparser-read-only/config/config_srl_en.xml", 
                				 "-i", dirName+testingFile, 
                				 "-o", dirName+predictionFile, 
                				 "-m", dirName+ds_model};
		
		Thread.sleep(2000);
		for (File file: files)
		{
			if (file.getName().contains(".model") && file.getName().contains("ds") && !file.getName().contains(".boot"))
			{
				System.out.println("TESTING!!");
				predictArgs[5] = dirName+FileUtil.getFileNameWoExt(file.getName())+".predict";
				predictArgs[7] = dirName+file.getName();
				new SRLPredict(predictArgs);
			}
		}
		// For each model test on evaporation
		// DONE
	}
}
