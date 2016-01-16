/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import java.io.Serializable;
import java.util.Arrays;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

/**
 *
 * @author slouvan
 */
public class FrameNetFeatureExtractor{
static final long serialVersionUID = 2106L;
    PythonInterpreter pi;
    PyFunction pf;

    public FrameNetFeatureExtractor()
    {
        init();
    }
    public void init() {
        pi = new PythonInterpreter();
        pi.exec("import sys");

        pi.exec("from pymodule import getNbFrame");
        pi.exec("from pymodule import getFrame");
        pi.exec("from pymodule import getAllFrames");

       /* System.out.println("START");
        pf = (PyFunction) pi.get("getFrame");
        PyObject object = pf.__call__(new PyString("convert.v"));

        object = pf.__call__(new PyString("move.v"));
        System.out.println("START2");

        pf = (PyFunction) pi.get("printAllFrame");
        pf.__call__();*/
    }

    public void loadAllFrame() {

    }

    public String[] getAllFrames()
    {
         pf = (PyFunction) pi.get("getAllFrames");
         PyObject object = pf.__call__();
         return (String[]) object.__tojava__(String[].class);
    }
    //public 
    public int getNbFrame() {
        pf = (PyFunction) pi.get("getNbFrame");
        return pf.__call__().asInt();
    }

    public String[] getFrame(String lex)
    {
        pf = (PyFunction) pi.get("getFrame");
        PyObject object = pf.__call__(new PyString(lex));
        return (String[]) object.__tojava__(String[].class);
    }
    
    public static void main(String[] args) {
        FrameNetFeatureExtractor fe = new FrameNetFeatureExtractor();
        fe.init();
        fe.loadAllFrame();
        System.out.println(fe.getNbFrame());
        //System.out.println(Arrays.toString(fe.getAllFrames()));
    }
}
