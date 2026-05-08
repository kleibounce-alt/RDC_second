package com.klei.common.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

@WebServlet(urlPatterns = {"/static/*"})
public class StaticServlet extends HttpServlet {

    private static final String BASE_DIR = System.getProperty("user.home").replace("\\", "/") + "/goodish_uploads";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            resp.sendError(404);
            return;
        }
        File file = new File(BASE_DIR + pathInfo);
        if (!file.exists() || file.isDirectory()) {
            resp.sendError(404);
            return;
        }
        String contentType = Files.probeContentType(file.toPath());
        if (contentType != null) {
            resp.setContentType(contentType);
        }
        resp.setContentLengthLong(file.length());
        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
    }
}
