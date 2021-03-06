package clear.util;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author samuellouvan
 */
public class FileUtil {

    public static String getFileNameWoExt(String fileNameWithExt) {
        int slashidx = fileNameWithExt.lastIndexOf("/");
        fileNameWithExt = fileNameWithExt.substring(slashidx + 1);
        int dotIdx = fileNameWithExt.lastIndexOf(".");
        return fileNameWithExt.substring(0, dotIdx);
    }

    public static File[] getFilesFromDir(String dirName) {
        File folder = new File(dirName);
        File[] files = folder.listFiles();
        return files;
    }

    public void fromConll2009ToClearParserFormat(String conllFile, String clearParserFile) {

    }

    public static File[] getFilesFromDir(String dirName, final String filter) {
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

    public static void dumpToFile(String[] text, PrintWriter writer) throws FileNotFoundException {
        for (String line : text) {
            writer.println(line);
        }
        writer.close();
    }

    public static void dumpToFile(String text, PrintWriter writer) throws FileNotFoundException {

        writer.println(text);
        writer.close();
    }

    public static void dumpToFile(ArrayList<String> text, String fileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName);
        for (String line : text) {
            writer.println(line);
        }
        writer.close();
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
