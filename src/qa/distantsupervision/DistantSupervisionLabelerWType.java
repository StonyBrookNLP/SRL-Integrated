package qa.distantsupervision;

import Util.ProcessFrameUtil;

import Util.StringUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import qa.ProcessFrame;
import qa.ProcessFrameProcessor;
import qa.StanfordDepParser;
import qa.StanfordDocumentProcessor;
import qa.StanfordLemmatizer;
import qa.StanfordTokenizer;
import qa.WordNet;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;

/**
 *
 * @author samuellouvan
 */
public class DistantSupervisionLabelerWType {

    private ProcessFrameProcessor proc; // To load all the data from process frame data
    private String processFrameFilename;  // Process frame fileName
    private String corpusFile;    // Target corpus as the source for distant supervision
    private String newAnnotatedFrameFileName;
    private int SENT_LENGTH = 250;
    private ArrayList<String> triggers;
    private ArrayList<String> enablers;
    private ArrayList<String> undergoers;
    private ArrayList<String> results;
    private ArrayList<String> relevantSentences;
    private StanfordDocumentProcessor docProcessor;
    private StanfordTokenizer tokenizer;
    private String targetProcessName = "";
    private StanfordDepParser depParser;
    private WordNet wn;
    // Constructor

    public DistantSupervisionLabelerWType(String processFrameFilename, String corpusFile, String newAnnotatedFileName) {
        this.processFrameFilename = processFrameFilename;
        this.corpusFile = corpusFile;
        this.newAnnotatedFrameFileName = newAnnotatedFileName;

    }

    // Initialize
    // Load the data from the process frame file
    public void init() throws FileNotFoundException, IOException, ClassNotFoundException {
        proc = new ProcessFrameProcessor(this.processFrameFilename);
        proc.loadProcessData();
        relevantSentences = new ArrayList<String>();
        docProcessor = new StanfordDocumentProcessor();
        tokenizer = new StanfordTokenizer();
        depParser = new StanfordDepParser();
        undergoers = new ArrayList<String>();
        enablers = new ArrayList<String>();
        triggers = new ArrayList<String>();
        results = new ArrayList<String>();
        wn = new WordNet();
    }

    // Load all role fillers
    public void loadRoleFillers(String processName) {
        System.out.println("START LOADING ROLE FILLERS");
        ArrayList<ProcessFrame> processFrames = proc.getProcessFrameByName(processName);
        targetProcessName = processFrames.get(0).getProcessName();
        for (ProcessFrame p : processFrames) {
            undergoers.addAll(StringUtil.getTokenAsList(p.getUnderGoer(), ProcessFrameProcessor.SEPARATOR));
            triggers.addAll(StringUtil.getTokenAsList(p.getTrigger(), ProcessFrameProcessor.SEPARATOR));
            enablers.addAll(StringUtil.getTokenAsList(p.getEnabler(), ProcessFrameProcessor.SEPARATOR));
            results.addAll(StringUtil.getTokenAsList(p.getResult(), ProcessFrameProcessor.SEPARATOR));
        }
        System.out.println("END LOADING ROLE FILLERS");

    }

    // Get the sentences from the target corpus 
    public ArrayList<String> getSentencesFromCorpus() throws FileNotFoundException {
        StringBuilder docStr = new StringBuilder();
        Scanner scanner = new Scanner(new File(this.corpusFile));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.isEmpty())
                docStr.append(line);
        }
        return docProcessor.getSentences(docStr.toString());
    }

    public ArrayList<String> filterSentence(ArrayList<String> sentences) {
        System.out.println("Filtering"); 
        ArrayList<String> filteredSentence = new ArrayList<String>();
        for (String sentence : sentences) {
            List<String> tokens = tokenizer.tokenize(sentence);
            if (StringUtil.isValidSentence(sentence, tokens)) {
                String cleanedSent = sentence.replaceAll("-LRB-", "(");
                cleanedSent = cleanedSent.replaceAll("-LSB", "(");
                cleanedSent = cleanedSent.replaceAll("-RSB", ")");
                cleanedSent = cleanedSent.replaceAll("-RRB-", ")");
                if (!filteredSentence.contains(cleanedSent))
                    filteredSentence.add(cleanedSent);
            }
        }
        System.out.println("End Filtering"); 
        return filteredSentence;
    }

    public void annotateSentence() throws FileNotFoundException, IOException {
        ArrayList<String> sentences = getSentencesFromCorpus();
        sentences = filterSentence(sentences);
        ArrayList<ProcessFrame> newAnnotatedFrames = new ArrayList<ProcessFrame>();
        int cnt = 0;
        for (String sentence : sentences) {
            // Dependency Parse
            List<String> tokens = tokenizer.tokenize(sentence);
            //System.out.println(sentence);
            DependencyTree depTree = depParser.parse(sentence);
            // If the sentence is related to the target process then check for occurrence of the role fillers
            ArrayList<String> matchesTrigger = StringUtil.getMatch(tokenizer.tokenize(sentence), triggers);
            // get the node of the trigger
            ArrayList<DependencyNode> triggerNodes = depTree.getNode(matchesTrigger);
            // get the type of undergoer, get the type of enabler, get the type of results
            // 
            String[] undergoerTypes = {"fluid"};
            String[] resultTypes    = {"gas", "vapor"};
            String[] enablerTypes   = {"energy","physical_property"};
            
            if (!matchesTrigger.isEmpty() && (matchesTrigger.contains("evaporates") || matchesTrigger.contains("evaporation") || matchesTrigger.contains("evaporate")) && sentence.length() <= SENT_LENGTH) {
                ArrayList<String> matchesUndergoer = depTree.getWordMatchType(triggerNodes, undergoerTypes, wn);
                ArrayList<String> matchesEnabler = depTree.getWordMatchType(triggerNodes, enablerTypes, wn);
                ArrayList<String> matchesResult = depTree.getWordMatchType(triggerNodes, resultTypes, wn);
                boolean validUndergoer = PatternChecker.isValidArgument(matchesUndergoer, matchesTrigger, depTree);
                boolean validEnabler = PatternChecker.isValidArgument(matchesEnabler, matchesTrigger, depTree);
                boolean validResult = PatternChecker.isValidArgument(matchesResult, matchesTrigger, depTree);

                if (validUndergoer || validEnabler || validResult) {
                    System.out.println("SENTENCE :" + sentence);
                    System.out.println("TRIGGER : " + matchesTrigger);
                    if (validUndergoer) {
                        // check whether it meets the dependency relation
                        System.out.println("UNDERGOER :" + matchesUndergoer);
                    }
                    if (validResult) {
                        // check whether it meets the dependency relation
                        System.out.println("RESULTS :" + matchesResult);
                    }
                    if (validEnabler) {
                        // check whether it meets the dependency relation
                        System.out.println("ENABLERS :" + matchesEnabler);
                    }

                    ProcessFrame frame = ProcessFrameUtil.createProcessFrame(targetProcessName, matchesUndergoer, matchesEnabler, matchesTrigger, matchesResult, sentence);
                    newAnnotatedFrames.add(frame);

                    cnt++;
                }
            }
        }
        System.out.println(cnt);
        ProcessFrameUtil.dumpFramesToFile(newAnnotatedFrames, this.newAnnotatedFrameFileName);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        DistantSupervisionLabelerWType labeler = new DistantSupervisionLabelerWType("./data/all_process_frame.tsv", "./data/evaporation_trigger_larger.txt", "./data/evaporation_trigger_larger.tsv");
        labeler.init();
        labeler.loadRoleFillers("evaporation");
        labeler.annotateSentence();
    }
}
