package com.klei.common.utils;

import com.klei.common.exception.BusinessException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileUploadUtil {

    private static final String UPLOAD_BASE = "upload";
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final String[] ALLOW_TYPES = {".jpg", ".jpeg", ".png", ".gif", ".webp"};

    public static String uploadSingleImage(HttpServletRequest request, String subDir) throws Exception {
        List<String> paths = uploadImages(request, subDir, 1);
        return paths.isEmpty() ? null : paths.get(0);
    }

    public static List<String> uploadImages(HttpServletRequest request, String subDir, int maxCount) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request)) {
            throw new BusinessException("请求不是multipart类型，无法上传文件");
        }

        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setSizeMax(MAX_SIZE);

        List<FileItem> items = upload.parseRequest(request);
        List<String> result = new ArrayList<>();

        for (FileItem item : items) {
            if (item.isFormField() || item.getSize() == 0) {
                continue;
            }

            String fileName = item.getName();
            String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

            if (!isAllowed(ext)) {
                LogUtil.warn("跳过不支持的文件类型: " + ext);
                continue;
            }

            String newName = UUID.randomUUID().toString().replace("-", "") + ext;
            String relativePath = UPLOAD_BASE + "/" + subDir + "/" + newName;

            String realPath = request.getServletContext().getRealPath("/") + relativePath;
            File dest = new File(realPath);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }

            item.write(dest);
            result.add(relativePath);

            if (maxCount > 0 && result.size() >= maxCount) {
                break;
            }
        }

        return result;
    }

    private static boolean isAllowed(String ext) {
        for (String type : ALLOW_TYPES) {
            if (type.equals(ext)) {
                return true;
            }
        }
        return false;
    }
}