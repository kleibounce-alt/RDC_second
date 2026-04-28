package com.klei.common.utils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogUtil {

    private static final String LOG_DIR = System.getProperty("user.dir") + File.separator + "logs";
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static BufferedWriter writer;
    private static String currentDate;
    private static final Object LOCK = new Object();

    static {
        initWriter();
        Runtime.getRuntime().addShutdownHook(new Thread(LogUtil::close));
    }

    private static void initWriter() {
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            currentDate = LocalDateTime.now().format(FILE_FORMAT);
            String fileName = LOG_DIR + File.separator + "app-" + currentDate + ".log";
            writer = new BufferedWriter(new FileWriter(fileName, true));
        } catch (IOException e) {
            System.err.println("日志文件初始化失败: " + e.getMessage());
        }
    }

    private static void checkDate() {
        String now = LocalDateTime.now().format(FILE_FORMAT);
        if (!now.equals(currentDate)) {
            close();
            initWriter();
        }
    }

    private static void write(String level, String msg) {
        synchronized (LOCK) {
            checkDate();
            String time = LocalDateTime.now().format(TIME_FORMAT);
            String line = time + " [" + level + "] " + msg;

            if ("ERROR".equals(level)) {
                System.err.println(line);
            } else {
                System.out.println(line);
            }

            if (writer != null) {
                try {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    System.err.println("写入日志文件失败: " + e.getMessage());
                }
            }
        }
    }

    public static void info(String msg) {
        write("INFO", msg);
    }

    public static void warn(String msg) {
        write("WARN", msg);
    }

    public static void error(String msg) {
        write("ERROR", msg);
    }

    public static void error(String msg, Throwable t) {
        StringBuilder sb = new StringBuilder(msg);
        if (t != null) {
            sb.append(" | ").append(t.toString());
            for (StackTraceElement e : t.getStackTrace()) {
                sb.append("\n    at ").append(e);
            }
        }
        write("ERROR", sb.toString());
    }

    public static void close() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                System.err.println("关闭日志失败: " + e.getMessage());
            } finally {
                writer = null;
            }
        }
    }
}