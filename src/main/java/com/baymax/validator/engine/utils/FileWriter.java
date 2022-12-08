package com.baymax.validator.engine.utils;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class FileWriter {
    private static Pattern p = Pattern.compile("\\s+");

    private static void mkdirsIfNecessary(String path) {
        if (!new File(path).exists()) {
            try {
                new File(path).mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
        }
    }


    private static void createFileIfNecessary(String fileUri) {
        if (!new File(fileUri).exists()) {
            try {
                new File(fileUri).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
        }
    }

    private static String getFileUri(String path, String fileName, String fileType) {
        return path + File.separator + fileName + "." + fileType;
    }

    /**
     * 生成文件，会覆盖旧文件
     * @param path
     * @param fileName
     * @param fileType
     * @param content
     */
    public static void write(String path, String fileName, String fileType, String content) {
        String fileUri = getFileUri(path, fileName, fileType);
        System.out.println("XmlOutputDir is :" + fileUri);

        //if(true) return;
        mkdirsIfNecessary(path);
        createFileIfNecessary(fileUri);

        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(fileUri), 10);

            if("xml".equals(fileType)) {
                OutputFormat outputFormat = new OutputFormat();
                outputFormat.setIndent(true);
                outputFormat.setIndentSize(4);
                outputFormat.setNewlines(true);
                outputFormat.setNewLineAfterNTags(4);
                outputFormat.setPadText(true);
                outputFormat.setEncoding("utf-8");
                XMLWriter writer = new XMLWriter(out, outputFormat);

                Document document = DocumentHelper.parseText(cleanSpace(content));
                writer.write(document);
            } else {
                out.write(content.getBytes("utf-8"));
                // 将“换行符\n”写入到输出流中
                out.write('\n');
                out.flush();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                out = null;
            }
        }
    }

    public static void backupAndWrite(String path, String fileName, String fileType, String content) {
        String fileUri = getFileUri(path, fileName, fileType);
        File file = new File(fileUri);
        if(file.exists()) {
            File newfile = new File(path + File.separator + fileName + "_" + System.currentTimeMillis() + "." + fileType);
            file.renameTo(newfile);
        }
        write(path, fileName, fileType, content);
    }

    /**
     * 如果不存在，则新增
     * @param path
     * @param fileName
     * @param fileType
     * @param content
     */
    public static void writeToFileIfNotExist(String path, String fileName, String fileType, String content) {
        String fileUri = getFileUri(path, fileName, fileType);
        File f = new File(fileUri);
        if(!f.exists()){
            write(path, fileName, fileType, content);
        }
    }

    /**
     * 将多个空格变成一个空格
     * @param str
     * @return
     */
    public static String cleanSpace(String str) {
        Matcher m = p.matcher(str);
        return m.replaceAll(" ");
    }
}
