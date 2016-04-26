/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import clear.util.FileUtil;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import sbu.srl.datastructure.Sentence;
import sbu.srl.rolextract.SpockDataReader;

/**
 *
 * @author samuellouvan
 */
public class StanfordTokenizer {

    protected StanfordCoreNLP pipeline;

    public StanfordTokenizer() {
        // Create StanfordCoreNLP object properties, with POS tagging
        // (required for lemmatization), and lemmatization
        Properties props;
        props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");

        /*
         * This is a pipeline that takes in a string and returns various analyzed linguistic forms. 
         * The String is tokenized via a tokenizer (such as PTBTokenizerAnnotator), 
         * and then other sequence model style annotation can be used to add things like lemmas, 
         * POS tags, and named entities. These are returned as a list of CoreLabels. 
         * Other analysis components build and store parse trees, dependency graphs, etc. 
         * 
         * This class is designed to apply multiple Annotators to an Annotation. 
         * The idea is that you first build up the pipeline by adding Annotators, 
         * and then you take the objects you wish to annotate and pass them in and 
         * get in return a fully annotated object.
         * 
         *  StanfordCoreNLP loads a lot of models, so you probably
         *  only want to do this once per execution
         */
        this.pipeline = new StanfordCoreNLP(props);
    }

    public synchronized List<String> tokenize(String documentText) {
        List<String> tokens = new LinkedList<String>();
        // Create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);
        // run all Annotators on this text
        this.pipeline.annotate(document);

        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                tokens.add(token.originalText());
                
            }
        }
        return tokens;
    }

    public List<CoreLabel> getTokenLabel(String documentText) {
        List<CoreLabel> tokens = new LinkedList<CoreLabel>();
        // Create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);
        // run all Annotators on this text
        this.pipeline.annotate(document);

        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                //System.out.println(token.beginPosition() + " "+token.endPosition());
                tokens.add(token);

            }
        }
        return tokens;
    }

    public static String replaceBrace(String sentence) {
        sentence = sentence.replace("(", "-LRB-");
        sentence = sentence.replace(")", "-RRB-");
        sentence = sentence.replace("[", "-LSB-");
        sentence = sentence.replace("]", "-RSB-");
        sentence = sentence.replace("{", "-LCB-");
        sentence = sentence.replace("}", "-RCB-");
        return sentence;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        String inputFile = args[0];
        String outputFile = args[1];
        System.out.println("Input file : "+inputFile);
        System.out.println("Output file : "+outputFile);
        String sentences[] = FileUtil.readLinesFromFile(inputFile);
        StanfordTokenizer tokenizer = StanfordTokenizerSingleton.getInstance();
        PrintWriter writer = new PrintWriter(outputFile);
        for (int i = 0; i < sentences.length; i++) {
            System.out.println("Tokenizing");
            List<String> tokens = tokenizer.tokenize(sentences[i].trim());
            String tokenizedStr = String.join(" ", tokens);
            tokenizedStr = replaceBrace(tokenizedStr);
            writer.println(tokenizedStr);
        }
        System.out.println("Finish tokenizing");
        writer.close();
        /*SpockDataReader reader = new SpockDataReader("./data/training_4_roles.tsv", "./configFrameFile/config.txt", false);
         reader.readData();
         ArrayList<Sentence> sentences = reader.getSentences();

         StanfordTokenizer tokenizer = StanfordTokenizerSingleton.getInstance();
         PrintWriter writer = new PrintWriter("./data/tokenizedStanford.txt");
         PrintWriter rawTextWriter = new PrintWriter("./data/sentences.txt");
         for (Sentence sent : sentences) {
         List<String> tokens = tokenizer.tokenize(sent.getRawText().trim());
         String tokenizedStr = String.join(" ", tokens);
         tokenizedStr = replaceBrace(tokenizedStr);
         writer.println(tokenizedStr);
         rawTextWriter.println(sent.getRawText().trim());
         }
         writer.close();
         rawTextWriter.close();*/

        /*String[] stanfordL = FileUtil.readLinesFromFile("./data/tokenizedStanford.txt");
         String[] ptbL = FileUtil.readLinesFromFile("./data/tokenizedPTB.txt");
         for (int i = 0; i < stanfordL.length; i++) {
         String[] tokenStanford = stanfordL[i].split(" ");
         String[] tokenPTB = ptbL[i].split(" ");
         if (tokenStanford.length != tokenPTB.length) {
         System.out.println("NOT EQUAL");
         System.out.println(stanfordL[i]);
         for (int j = 0; j < tokenStanford.length; j++) {
         if (!tokenStanford[j].equalsIgnoreCase(tokenPTB[j])) {
         System.out.println(tokenStanford[j]);
         }
         }
         }
         for (int j = 0 ; j < tokenStanford.length; j++)
         {
         if (!tokenStanford[j].equalsIgnoreCase(tokenPTB[j]))
         System.out.println(tokenStanford[j]);
         }
         }*/
    }
}
