package com.klei.xianyuMarket.utils;

import com.klei.xianyuMarket.exception.BusinessException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传工具
 * 1. 图片存到 Tomcat 部署目录的 upload/ 下
 * 2. 数据库只存相对路径（如 upload/avatar/xxx.jpg）
 * 3. 限制类型：jpg/jpeg/png/gif/webp
 * 4. 限制大小：单文件 5MB
 * 5. 文件名用 UUID，防止覆盖
 */
public class FileUploadUtil {

    private static final String UPLOAD_BASE = "upload";
    // 5MB
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final String[] ALLOW_TYPES = {".jpg", ".jpeg", ".png", ".gif", ".webp"};

    /**
     * 上传单张图片
     * @param request HttpServletRequest
     * @param subDir 子目录，如 "avatar" 或 "product"
     * @return 相对路径，如 "upload/avatar/xxx.jpg"
     */
    public static String uploadSingleImage(HttpServletRequest request, String subDir) throws Exception {
        List<String> paths = uploadImages(request, subDir, 1);
        return paths.isEmpty() ? null : paths.get(0);
    }

    /**
     * 上传多张图片
     * @param request HttpServletRequest
     * @param subDir 子目录
     * @param maxCount 最多几张，0 表示不限制
     * @return 相对路径列表
     */
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

            // 获取 Tomcat 真实部署路径
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