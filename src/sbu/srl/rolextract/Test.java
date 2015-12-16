/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgProcessAnnotationData;
import sbu.srl.datastructure.Sentence;

/**
 *
 * @author slouvan
 */
public class Test {

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        ArrayList<Orang> orangs = new ArrayList<Orang>();
        Orang orang = new Orang("Samuel");
        orangs.add(orang);
        
        ArrayList<Orang> orangs2 = new ArrayList<Orang>();
        orangs2.add(orangs.get(0));
        orangs2.get(0).setNama("Clara");
        
        
       /* ArrayList<ArgProcessAnnotationData> arr = (ArrayList<ArgProcessAnnotationData>) FileUtil.deserializeFromFile("/home/slouvan/NetBeansProjects/SRL-Integrated/data/cross-val/fold-1/test/test.predict.ser");
        Sentence s = arr.get(0).getSentence();
        //System.out.println(s);
        Gson gson = new Gson();

        System.out.println(gson.toJson(s));*/
    }
}

class Orang {

    private String nama;

    public Orang(String nama) {
        this.nama = nama;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    @Override
    public String toString() {
        return "Orang{" + "nama=" + nama + '}';
    }

}
