/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Scanner;

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

    public static void dumpToFile(ArrayList<String> text, String fileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        for (String line : text) {
            writer.print(line);
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

    public static boolean mkDir(String dirName) {
        File file = new File(dirName);
        return file.mkdir();
    }

    public static String readCoNLLFormat(String fileName) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        Scanner scanner = new Scanner(new File(fileName));
        while (scanner.hasNextLine()) {
            sb.append(scanner.nextLine() + "\n");
        }

        return sb.toString().trim();
    }

    public static void main(String[] args) throws FileNotFoundException {
        //String[] lines = readLinesFromFile("./data/sp/process.tsv");
        //System.out.println(lines.length);
        System.out.println(readCoNLLFormat("temp.dep"));
    }

}
