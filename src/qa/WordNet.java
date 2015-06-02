/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import Util.ArrUtil;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

/**
 *
 * @author samuellouvan
 */
public class WordNet {

    private IDictionary dict;

    public WordNet() throws IOException
    {
        init();
    }
    public void init() throws IOException {
// construct the URL to the Wordnet dictionary directory
        String wnhome = "/usr/local/WordNet-3.0";
        String path = wnhome + File.separator + "dict";
        URL url = new URL("file", null, path);
// construct the dictionary object and open it
        dict = new Dictionary(url);
        dict.open();
// look up first sense of the word "dog"

    }

    public boolean isHypernym(String lemma) {
        return false;
    }

    public ArrayList<String> getLemma(ISynset syn) {
        List<IWord> words;
        ArrayList<String> lemmas = new ArrayList<String>();
        words = syn.getWords();
        
        for (Iterator<IWord> i = words.iterator(); i.hasNext();) {
            //System.out.print(i.next().getLemma());
            lemmas.add(i.next().getLemma());
            
        }
        
        return lemmas;
    }
        
    
    

    public boolean isMatchType(String lemma, String[] types) {
        // get the synset
        IIndexWord idxWord = dict.getIndexWord(lemma, POS.NOUN);
        if (idxWord == null)
            return false;
        IWordID wordID = idxWord.getWordIDs().get(0); // 1st meaning 
        IWord word = dict.getWord(wordID);
        ISynset synset = word.getSynset();
        // get the hypernyms
        List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);

        Queue queue = new LinkedList();
        queue.addAll(hypernyms);

        int depth = 0;
        while (queue.size() > 0) {
            ISynsetID currentSynsetID = (ISynsetID) queue.remove();
            ISynset currentSynset = dict.getSynset(currentSynsetID);
            
            ArrayList<String> lemmas = getLemma(currentSynset);
            if (ArrUtil.isExistIntersect(lemmas, types))
                return true;
            List<ISynsetID> parents = currentSynset.getRelatedSynsets(Pointer.HYPERNYM);
            queue.addAll(parents);
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        WordNet wn = new WordNet();
        String[] types = {"stone"};
        System.out.println(wn.isMatchType("water", types));

    }
}
