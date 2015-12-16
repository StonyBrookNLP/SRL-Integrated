/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.util;

import Util.Constant;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.io.AllCoNLL09Reader;

/**
 *
 * @author samuellouvan
 */
public class FileUtil {

    public static String getFileNameWoExt(String fileNameWithExt) {

        int dotIdx = fileNameWithExt.indexOf(".");
        if (dotIdx == -1) {
            return fileNameWithExt;
        }
        return fileNameWithExt.substring(0, dotIdx);
    }

    public static File[] getFilesFromDir(String dirName) {
        File folder = new File(dirName);
        File[] files = folder.listFiles();
        return files;
    }

    /**
     * This method makes a "deep clone" of any Java object it is given.
     */
    public static Object deepClone(Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getFileHeader(String fileName) throws FileNotFoundException {
        String[] lines = readLinesFromFile(fileName);
        return lines[0];
    }

    public static void serializeToFile(Object o, String fileName) throws IOException {
        FileOutputStream outFStream = new FileOutputStream(fileName);
        ObjectOutputStream oos = new ObjectOutputStream(outFStream);
        oos.writeObject(o);
        oos.close();
    }

    public static Object deserializeFromFile(String fileName) throws FileNotFoundException, IOException, ClassNotFoundException {
        FileInputStream inFStream = new FileInputStream(fileName);
        ObjectInputStream ois = new ObjectInputStream(inFStream);
        Object obj = ois.readObject();

        return obj;
    }

    public static List<String[]> readDataObject(String fileName, String delimiter) throws FileNotFoundException {
        final List<String[]> dataObjectList = new ArrayList<>();
        Scanner scanner = new Scanner(new File(fileName));
        String[] headerObjects = scanner.nextLine().split(delimiter);
        dataObjectList.add(headerObjects);
        while (scanner.hasNextLine()) {
            String[] dataObjects = scanner.nextLine().split(delimiter);
            if (dataObjects.length < headerObjects.length) {
                String[] modDataObjects = new String[headerObjects.length];
                for (int i = 0; i < modDataObjects.length; i++) {
                    if (i < dataObjects.length) {
                        modDataObjects[i] = dataObjects[i];
                    } else {
                        modDataObjects[i] = "";
                    }
                }
                dataObjectList.add(modDataObjects);
            } else if (dataObjects.length > headerObjects.length) {
                System.out.println("DATA ITEM IS WRONG! LENGTH MISMATCH");
                System.exit(0);
            } else {
                dataObjectList.add(dataObjects);
            }
        }
        scanner.close();

        return dataObjectList;
    }

    public static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.flush();
        oos.close();
        bos.close();
        byte[] byteData = bos.toByteArray();
        return byteData;
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        Object object = (Object) new ObjectInputStream(bais).readObject();

        return object;
    }

    public static File[] getFilesFromDir(String dirName, String filter) {
        // create new filename filter
        FilenameFilter fileNameFilter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.contains(filter)) {
                    return true;
                }
                return false;
            }
        };

        File folder = new File(dirName);

        return folder.listFiles(fileNameFilter);
    }

    public static String[] readLinesFromFile(String fileName) throws FileNotFoundException {
        ArrayList<String> lines = new ArrayList<String>();
        Scanner scanner = new Scanner(new File(fileName));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            lines.add(line);
        }

        return lines.toArray(new String[lines.size()]);
    }

    public static String[] readLinesFromFile(String fileName, boolean skipHeader, String headerSignature) throws FileNotFoundException {
        ArrayList<String> lines = new ArrayList<String>();
        Scanner scanner = new Scanner(new File(fileName));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().toLowerCase();
            if (skipHeader) {
                if (!line.startsWith(headerSignature.toLowerCase())) {
                    lines.add(line);
                }
            } else {
                lines.add(line);
            }

        }

        return lines.toArray(new String[lines.size()]);
    }

    public static void dumpToFile(ArrayList<String> text, String fileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        for (String line : text) {
            writer.print(line);
        }
        writer.close();
    }

    public static void dumpToFileWHeader(ArrayList<String> text, String fileName, String header) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        writer.println(header);
        for (String line : text) {
            writer.print(line + "\n");
        }
        writer.close();
    }

    public static void dumpToFile(ArrayList<String> text, String fileName, String sep) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        for (String line : text) {
            writer.print(line + "\n");
        }
        writer.close();
    }

    public static void dumpToFile(String text, String fileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        writer.println(text);
        writer.close();
    }

    public static boolean mkDir(String dirName) throws IOException {
        File file = new File(dirName);
        if (file.exists()) {
            FileUtils.cleanDirectory(new File(dirName));
            return true;
        } else {
            return file.mkdir();
        }

    }

    public static String readCoNLLFormat(String fileName) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        Scanner scanner = new Scanner(new File(fileName));
        while (scanner.hasNextLine()) {
            sb.append(scanner.nextLine() + "\n");
        }
        scanner.close();
        return sb.toString().trim();
    }

    public static int getTriggerType(String conllFile) {
        List<Sentence> sentences = new AllCoNLL09Reader(new File(conllFile)).readAll();
        int nbV = 0;
        int nbN = 0;
        for (Sentence sentence : sentences) {
            List<Predicate> predicates = sentence.getPredicates();
            for (Predicate predicate : predicates) {
                String posType = predicate.getPOS();
                if (predicate.getArgMap().size() > 0) {
                    if (posType.startsWith("VB")) {
                        nbV++;
                    }
                    if (posType.startsWith("NN")) {
                        nbN++;
                    }
                }
            }
        }

        if (nbV > 0 && nbN == 0) {
            return Constant.TRIGGER_VB_ONLY;
        }
        if (nbN > 0 && nbV == 0) {
            return Constant.TRIGGER_NN_ONLY;
        }
        return Constant.TRIGGER_OTHER;
    }

    public static void fromConll2009ToClearParserFormat(String conllFile, String clearParserFile) throws FileNotFoundException {
        List<Sentence> sentences = new AllCoNLL09Reader(new File(conllFile)).readAll();
        PrintWriter clearWriter = new PrintWriter(clearParserFile);
        for (Sentence sentence : sentences) {
            List<Predicate> predicates = sentence.getPredicates();
            int nbPredicate = predicates.size();
            ArrayList<Integer> predicateIdx = new ArrayList<Integer>();
            for (int i = 0; i < predicates.size(); i++) {
                predicateIdx.add(predicates.get(i).getIdx());
            }
            Collections.sort(predicateIdx);
            StringBuilder clearParserStr = new StringBuilder();
            for (int i = 1; i < sentence.size(); i++) {
                Word word = sentence.get(i);
                clearParserStr.append(word.getIdx()).append("\t");
                clearParserStr.append(word.getForm()).append("\t");
                clearParserStr.append(word.getLemma()).append("\t");
                clearParserStr.append(word.getPOS()).append("\t");
                clearParserStr.append("_").append("\t");
                clearParserStr.append(word.getHeadId()).append("\t");
                clearParserStr.append(word.getDeprel()).append("\t");

                if (word instanceof Predicate) {
                    clearParserStr.append(word.getLemma()).append(".01").append("\t");
                } else {
                    clearParserStr.append("_").append("\t");
                }

                String tag;
                //clearParserStr.append("_").append("\t"); // sejumlah banyaknya predicates
                if (nbPredicate == 0) {
                    clearParserStr.append("_");
                } else {
                    boolean hasArg = false;
                    for (int j = 0; j < predicates.size(); ++j) {
                        Predicate pred = predicates.get(j);
                        if (pred.getArgumentTag(word) != null) {
                            if (!hasArg) {
                                clearParserStr.append(pred.getIdx() + ":" + pred.getArgumentTag(word));
                            } else {
                                clearParserStr.append(";").append(pred.getIdx() + ":" + pred.getArgumentTag(word));
                            }
                            hasArg = true;
                        }
                        //clearParserStr.append((tag = pred.getArgumentTag(word)) != null ? (pred.getIdx() + ":" + tag) : "_"); // tambahin ; 
                    }
                    if (!hasArg) {
                        clearParserStr.append("_");
                    }
                }
                clearParserStr.append("\n");
            }
            clearWriter.println(clearParserStr.toString());
        }
        clearWriter.close();
    }

    public static boolean isFileExist(String fileName) {
        File f = new File(fileName);
        return f.exists();
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Hello");
        List<String[]> dataObjects = readDataObject("/home/slouvan/NetBeansProjects/SRL-Integrated/data/training_w_pattern.tsv", "\t");

    }

}
