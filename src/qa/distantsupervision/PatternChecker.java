/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.distantsupervision;

import java.util.ArrayList;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;

/**
 *
 * @author samuellouvan
 */
public class PatternChecker {
    
    public static boolean isValidArgument(ArrayList<String> arguments, ArrayList<String> triggers, DependencyTree tree)
    {
        ArrayList<DependencyNode> argumentNodes =  tree.getNode(arguments);
        ArrayList<DependencyNode> triggerNodes =  tree.getNode(triggers);
        if (arguments.size() <= 0)
            return false;
        // getNode of argument
        // getNode  of triggers
        // for each node  of argument
        for (DependencyNode argNode:argumentNodes)
        {
            // for each node  of trigger
            for (DependencyNode triggerNode : triggerNodes)
            {
                if (tree.isExistPathFrom(argNode, triggerNode))
                    return true;
            }
            // is there a path from argument to trigger?
        }
        return false;
    }
}
