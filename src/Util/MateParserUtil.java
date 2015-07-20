/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

/**
 *
 * @author samuellouvan
 */
public class MateParserUtil {

    public static String[] TRAIN_ARGS = {"eng",
        "", // TRAINING FILE INPUT
        ""// MODEL NAME

    };

    public static String[] PREDICT_ARGS_NOPI = {"eng",
        "", // TEST FILE INPUT
        "", // MODEL NAME
        "-nopi",
        ""  // OUTPUT FILE
     //"-nopi", // NOPI
    };
    
    public static String[] PREDICT_ARGS_PI = {"eng",
        "", // TEST FILE INPUT
        "", // MODEL NAME
        ""
    };

}
