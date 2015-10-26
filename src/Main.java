/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws PyException {
        PythonInterpreter pi = new PythonInterpreter();
        pi.exec("import sys");
        pi.exec("from pymodule import getFrame");
        pi.exec("from pymodule import getAllFrames");
        pi.exec("print sys.path");
        System.out.println("START");
        PyFunction pf = (PyFunction) pi.get("getFrame");
        PyObject object = pf.__call__(new PyString("convert.v"));

        object = pf.__call__(new PyString("move.v"));
        System.out.println("START2");

        pf = (PyFunction) pi.get("getAllFrames");
        object = pf.__call__();
        String[] frameNames = (String[]) object.__tojava__(String[].class);
        System.out.println(frameNames.length);
    }

}
