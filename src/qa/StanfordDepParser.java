/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoNLLOutputter;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import qa.dep.DependencyTree;
import qa.util.FileUtil;

/**
 *
 * @author samuellouvan
 */
public class StanfordDepParser {
   protected StanfordCoreNLP pipeline;
   

    public StanfordDepParser() {
        // Create StanfordCoreNLP object properties, with POS tagging
        // (required for lemmatization), and lemmatization
        Properties props;
        props = new Properties();
        props.put("annotators", "tokenize,ssplit,pos,lemma,depparse");
        //props.put("threads",  Runtime.getRuntime().availableProcessors());
        
        
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

    public synchronized DependencyTree parse(String documentText) throws IOException
    {
       
        // Create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);
        // run all Annotators on this text
        this.pipeline.annotate(document);
        // Iterate over all of the sentences found
        SemanticGraph ccProcessed  = document.get(CoreAnnotations.SentencesAnnotation.class).get(0)
                                        .get(
                                            SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
        
        Collection<TypedDependency> dependencies = ccProcessed.typedDependencies();
        CoNLLOutputter.conllPrint(document, new FileOutputStream(new File("temp.dep")));
        String conllString = FileUtil.readCoNLLFormat("temp.dep");
        //System.out.println(documentText);
        DependencyTree tree = DependencyTree.fromCoNLLFormatString(conllString);
        
        return tree;
    } 
    
    public String parseCoNLL(String documentText)throws IOException
    {
        // Create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);
        // run all Annotators on this text
        this.pipeline.annotate(document);
        // Iterate over all of the sentences found
        SemanticGraph ccProcessed  = document.get(CoreAnnotations.SentencesAnnotation.class).get(0)
                                        .get(
                                            SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
        
        Collection<TypedDependency> dependencies = ccProcessed.typedDependencies();
        CoNLLOutputter.conllPrint(document, new FileOutputStream(new File("temp.dep")));
        String conllString = FileUtil.readCoNLLFormat("temp.dep");
        
        return conllString;
    }
    public static void main(String[] args) throws IOException
    {
        StanfordDepParser parser = new StanfordDepParser();
        DependencyTree tree = parser.parse("This is just another way of saying that evolution is the process of Self-realization.");
        System.out.println(tree.toString());
    }
}
