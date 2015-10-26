/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.extractor;

import Util.StringUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import qa.StanfordDepParser;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;

/**
 *
 * @author slouvan
 */
public class RoleExtractor {

    HashMap<String, String> nomBankTokens = new HashMap<String, String>();
    HashMap<String, String> blackListToken = new HashMap<String, String>();

    public void loadNomBank(String fileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName));
        while (scanner.hasNextLine()) {
            String token = scanner.nextLine();
            nomBankTokens.put(token, token);
        }

        scanner.close();
    }

    public void loadBlackList(String fileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName));
        while (scanner.hasNextLine()) {
            String token = scanner.nextLine();
            blackListToken.put(token, token);
        }
        scanner.close();
    }

    public ArrayList<RoleSpan> extract(int ruleID, DependencyTree tree) {
        ArrayList<RoleSpan> results = new ArrayList<RoleSpan>();

        switch (ruleID) {
            case 1: {
                System.out.println("Rule 1");
                // check token after the
                List<DependencyNode> theNode = tree.values().stream()
                        .filter(node -> node.getForm().equalsIgnoreCase("the"))
                        .collect(Collectors.toList());
                if (theNode.size() > 0) {
                    int theIdx = theNode.get(0).getId();
                    DependencyNode tokenAfterThe = tree.get(theIdx + 1);
                    // if it exists in NomBank then extract it as trigger
                    if ((nomBankTokens.containsKey(tokenAfterThe.getForm()) || tokenAfterThe.getCpos().startsWith("VB")) && !blackListToken.containsKey(tokenAfterThe.getForm())) {
                        results.add(new RoleSpan("T", tokenAfterThe));
                        System.out.println("TRIGGER : " + tokenAfterThe.getForm());
                    }
                }
                break;
            }
            case 2: {
                System.out.println("Rule 2");

                List<DependencyNode> occurNode = tree.values().stream()
                        .filter(node -> node.getForm().equalsIgnoreCase("occurs"))
                        .collect(Collectors.toList());
                if (!occurNode.isEmpty()) {
                    ArrayList<DependencyNode> nodes = tree.getAllChildren(occurNode.get(0));
                    DependencyNode advClNode = nodes.stream()
                         .filter(node -> node.getRelationLabel().equalsIgnoreCase("advcl"))
                         .findFirst().get();
                    ArrayList<DependencyNode>  roleFillersNode = tree.getAllChildren(advClNode);
                    List<DependencyNode> sortedNodes = roleFillersNode.stream()
                            .filter(node -> node.getId() > advClNode.getId())
                            .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId()))
                            .collect(Collectors.toList());
                    for (int i = 0; i < sortedNodes.size(); i++) {
                        results.add(new RoleSpan("A1", sortedNodes.get(i)));
                    }
                }
                break;
            }
            case 3:
            case 4: {
                System.out.println("Rule 3/4");
                // Take all the children of TO
                // check token after TO
                List<DependencyNode> toNode = tree.values().stream()
                        .filter(node -> node.getForm().equalsIgnoreCase("to"))
                        .collect(Collectors.toList());
                if (!toNode.isEmpty()) {
                    ArrayList<DependencyNode> nodes = tree.getAllChildren(tree.get(toNode.get(0).getHeadID() + 1));
                    List<DependencyNode> sortedNodes = nodes.stream()
                            .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId()))
                            .collect(Collectors.toList());
                    for (int i = 0; i < sortedNodes.size(); i++) {
                        results.add(new RoleSpan("A1", sortedNodes.get(i)));
                    }
                }
                break;
            }
            case 5: {
                System.out.println("Rule 5");
                // Take all the children of BY
                List<DependencyNode> byNode = tree.values().stream()
                        .filter(node -> node.getForm().equalsIgnoreCase("by"))
                        .collect(Collectors.toList());
                if (!byNode.isEmpty()) {
                    ArrayList<DependencyNode> nodes = tree.getAllChildren(byNode.get(0));
                    List<DependencyNode> sortedNodes = nodes.stream()
                            .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());
                    for (DependencyNode node : sortedNodes) {
                        results.add(new RoleSpan("A1", node));
                    }
                }
                break;
            }
            case 6: {
                System.out.println("Rule 6");
                List<DependencyNode> ofNode = tree.values().stream()
                        .filter(node -> node.getForm().equalsIgnoreCase("of"))
                        .collect(Collectors.toList());
                if (!ofNode.isEmpty()) {
                    DependencyNode afterOf = tree.get(ofNode.get(0).getId() + 1);
                    if (afterOf != null && afterOf.getRelationLabel().equalsIgnoreCase("pobj")) {
                        ArrayList<DependencyNode> pobjNodeAndChildren = tree.getAllChildren(afterOf);
                        pobjNodeAndChildren.add(afterOf);
                        List<DependencyNode> sortedNodes = pobjNodeAndChildren.stream()
                                .filter(node -> node.getId() < afterOf.getId())
                                .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());
                        if (!nomBankTokens.containsKey(pobjNodeAndChildren.get(pobjNodeAndChildren.size() - 1).getForm())) {
                            for (DependencyNode node : sortedNodes) {
                                results.add(new RoleSpan("A0", node));
                            }
                        }
                    } else {
                        DependencyNode nodeAfterOf = tree.get(ofNode.get(0).getId() + 1);
                        if (nodeAfterOf.getCpos().startsWith("VB")) {
                            results.add(new RoleSpan("T", nodeAfterOf));
                            List<DependencyNode> prtNode = tree.dependentsOf(nodeAfterOf).stream()
                                    .filter(e -> e.getRelationLabel().equalsIgnoreCase("prt"))
                                    .collect(Collectors.toList());
                            for (DependencyNode node : prtNode) {
                                results.add(new RoleSpan("T", node));
                            }

                            DependencyNode undergoer = tree.dependentsOf(afterOf)
                                    .stream()
                                    .filter(node -> node.getId() > nodeAfterOf.getId())
                                    .filter(node -> node.getRelationLabel().equalsIgnoreCase("dobj"))
                                    .findFirst().get();
                            if (undergoer != null) {
                                ArrayList<DependencyNode> undergoers = tree.getAllChildren(undergoer);
                                undergoers.add(undergoer);
                                List<DependencyNode> sortedUndergoers = undergoers.stream()
                                        .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId()))
                                        .collect(Collectors.toList());
                                for (DependencyNode node : sortedUndergoers) {
                                    results.add(new RoleSpan("A0", node));
                                }

                            }

                        }
                    }
                }
                break;
            }
            case 7: {
                System.out.println("Rule 7");
                // Take all the children of through
                List<DependencyNode> throughNode = tree.values().stream()
                        .filter(node -> node.getForm().equalsIgnoreCase("through"))
                        .collect(Collectors.toList());
                if (!throughNode.isEmpty()) {
                    ArrayList<DependencyNode> nodes = tree.getAllChildren(throughNode.get(0));
                    List<DependencyNode> sortedNodes = nodes.stream()
                            .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());
                    for (DependencyNode node : sortedNodes) {
                        results.add(new RoleSpan("A1", node));
                    }
                }
                break;
            }
            case 8: {
                System.out.println("Rule 8");
                // get next node after OF

                // FROM
                List<DependencyNode> fromNode = tree.values().stream()
                        .filter(node -> node.getForm().equalsIgnoreCase("from"))
                        .collect(Collectors.toList());
                // take all children of FROM
                ArrayList<DependencyNode> fromChildren = tree.getAllChildren(fromNode.get(0));
                List<DependencyNode> sortedNodes = fromChildren.stream()
                        .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());
                for (DependencyNode node : sortedNodes) {
                    results.add(new RoleSpan("A3", node));
                }
                // TO
                List<DependencyNode> toNode = tree.values().stream()
                        .filter(node -> node.getForm().equalsIgnoreCase("to"))
                        .collect(Collectors.toList());
                // take all children of TO
                ArrayList<DependencyNode> toChildren = tree.getAllChildren(toNode.get(0));
                sortedNodes = toChildren.stream()
                        .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());
                for (DependencyNode node : sortedNodes) {
                    results.add(new RoleSpan("A3", node));
                }

                // get the dobj of the VERB, that's the undergoer
                break;
            }
            case 9: {
                System.out.println("Rule 9");
                // Take the parents of  CAUSE
                List<DependencyNode> causeNode = tree.values().stream()
                        .filter(node -> (node.getLemma().equalsIgnoreCase("cause")))
                        .collect(Collectors.toList());
                ArrayList<DependencyNode> nodes = tree.getAllChildren(causeNode.get(0));
                List<DependencyNode> sortedNodes = nodes.stream().filter(node -> node.getId() > causeNode.get(0).getId())
                        .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());
                for (DependencyNode node : sortedNodes) {
                    results.add(new RoleSpan("A2", node));
                }
                break;
            }
            case 10: {
                System.out.println("Rule 10");
                // get the parent of BY
                List<DependencyNode> byNode = tree.values().stream()
                        .filter(node -> (node.getLemma().equalsIgnoreCase("by")))
                        .collect(Collectors.toList());
                DependencyNode byParentNode = tree.get(byNode.get(0).getHeadID());
                // if it is a VERB then it is a TRIGGER
                if (byParentNode.getCpos().startsWith("VB")) {
                    System.out.println("Trigger :" + byParentNode.getForm());
                    results.add(new RoleSpan("T", byParentNode));
                    // nsubj of trigger and its children is UNDERGOER
                    List<DependencyNode> childrenOfTrigger = tree.getAllChildren(byParentNode);
                    List<DependencyNode> sbjTrigger = childrenOfTrigger.stream()
                            .filter(e -> e.getRelationLabel().equalsIgnoreCase("nsubj"))
                            .collect(Collectors.toList());
                    if (!sbjTrigger.isEmpty()) {
                        List<DependencyNode> sbjTriggerAndChildren = tree.getAllChildren(sbjTrigger.get(0));
                        sbjTriggerAndChildren.add(sbjTrigger.get(0));
                        List<DependencyNode> sortedNode = sbjTriggerAndChildren.stream()
                                .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());
                        for (DependencyNode node : sortedNode) {
                            results.add(new RoleSpan("A0", node));
                        }
                    }

                }
                // get the INTO node
                List<DependencyNode> intoNode = tree.values().stream()
                        .filter(node -> (node.getLemma().equalsIgnoreCase("into")))
                        .collect(Collectors.toList());
                List<DependencyNode> resultNode = tree.getAllChildren(intoNode.get(0));
                List<DependencyNode> sortedNode = resultNode.stream()
                        .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());
                for (DependencyNode node : sortedNode) {
                    results.add(new RoleSpan("A2", node));
                }

                // get all children of INTO, these are results
                break;
            }
            case 11: {
                System.out.println("Rule 11");
                List<DependencyNode> necessaryNode = tree.values().stream()
                        .filter(node -> (node.getLemma().equalsIgnoreCase("necessary")))
                        .collect(Collectors.toList());
                List<DependencyNode> subjectOfNecessaryNode = tree.getAllChildren(necessaryNode.get(0))
                        .stream()
                        .filter(e -> e.getRelationLabel().equalsIgnoreCase("nsubj"))
                        .collect(Collectors.toList());

                if (!subjectOfNecessaryNode.isEmpty()) {
                    ArrayList<DependencyNode> nodes = tree.getAllChildren(subjectOfNecessaryNode.get(0));
                    nodes.add(subjectOfNecessaryNode.get(0));
                    List<DependencyNode> sortedNode = nodes.stream()
                            .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());

                    for (DependencyNode node : sortedNode) {
                        results.add(new RoleSpan("A1", node));
                    }
                }
                break;
            }
            case 12: {
                System.out.println("Rule 12");
                // Take right children of AS
                // Take the parents of  CAUSE
                List<DependencyNode> asNode = tree.values().stream()
                        .filter(node -> (node.getLemma().equalsIgnoreCase("as")))
                        .collect(Collectors.toList());
                ArrayList<DependencyNode> nodes = tree.getAllChildren(asNode.get(0));
                List<DependencyNode> sortedNode = nodes.stream().filter(node -> node.getId() > asNode.get(0).getId())
                        .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());

                for (DependencyNode node : sortedNode) {
                    results.add(new RoleSpan("A1", node));
                }
                break;
            }

            case 13:
            case 14: {
                System.out.println("Rule 13/14");
                // Get the parent of TO
                // Get all the children of that node
                List<DependencyNode> toNode = tree.values().stream()
                        .filter(node -> (node.getLemma().equalsIgnoreCase("to")))
                        .collect(Collectors.toList());
                DependencyNode parentOfToNode = tree.get(toNode.get(0).getHeadID());
                ArrayList<DependencyNode> nodes = tree.getAllChildren(parentOfToNode);
                nodes.add(parentOfToNode);
                List<DependencyNode> sortedNode = nodes.stream().filter(node -> node.getId() >= parentOfToNode.getId())
                        .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId())).collect(Collectors.toList());
                for (DependencyNode node : sortedNode) {
                    results.add(new RoleSpan("A2", node));
                }
                break;

            }
            default:
                break;

        }
        StringUtil.removePunctuationStartEndFromRoleSpan(results);
        return results;
    }

    public static void main(String[] args) throws IOException {
        RoleExtractor extractor = new RoleExtractor();
        extractor.loadBlackList("./data/roleFillersBlackList.txt");
        extractor.loadNomBank("./data/nombank.1.0.words");

        StanfordDepParser parser = new StanfordDepParser();
        DependencyTree tree = parser.parse("Diffusion is the process of molecules moving from regions of higher concentrations to regions of lower concentrations.");
        System.out.println(tree.toString());

        extractor.extract(6, tree);
    }
}
