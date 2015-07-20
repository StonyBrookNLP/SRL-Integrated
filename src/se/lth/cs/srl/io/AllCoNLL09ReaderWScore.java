/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.lth.cs.srl.io;

import java.io.File;
import java.io.IOException;
import se.lth.cs.srl.corpus.Sentence;
import static se.lth.cs.srl.io.AbstractCoNLL09Reader.NEWLINE_PATTERN;

/**
 *
 * @author samuellouvan
 */
public class AllCoNLL09ReaderWScore extends AbstractCoNLL09Reader {

	public AllCoNLL09ReaderWScore(File file) {
		super(file);
	}

	protected void readNextSentence() throws IOException{
		String str;
		Sentence sen=null;
		StringBuilder senBuffer=new StringBuilder();
		while ((str = in.readLine()) != null) {
			
			if(!str.trim().equals("")) {
				senBuffer.append(str).append("\n");
			} else {
				//System.out.println(str.length());
				sen=Sentence.newSentenceWithScore((NEWLINE_PATTERN.split(senBuffer.toString())));
				break;
			}
		}
		if(sen==null){
			nextSen=null;
			in.close();
		} else {
			nextSen=sen;
		}
	}

    
}
