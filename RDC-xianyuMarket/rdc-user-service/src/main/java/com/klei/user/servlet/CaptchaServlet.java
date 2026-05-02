package com.klei.user.servlet;

import com.klei.common.utils.RedisUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import javax.imageio.ImageIO;

@WebServlet("/captcha")
public class CaptchaServlet extends HttpServlet {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final int EXPIRE_SECONDS = 120;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String code = generateCode();

        // Redis 存 2 分钟
        RedisUtil.setex("captcha:" + uuid, EXPIRE_SECONDS, code);

        // 返回图片，UUID 放响应头
        resp.setHeader("X-Captcha-UUID", uuid);
        resp.setContentType("image/png");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setDateHeader("Expires", 0);

        BufferedImage image = drawImage(code);
        ImageIO.write(image, "PNG", resp.getOutputStream());
    }

    private String generateCode() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }

    private BufferedImage drawImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 白底
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        Random r = new Random();
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 20; i++) {
            g.drawLine(r.nextInt(WIDTH), r.nextInt(HEIGHT), r.nextInt(WIDTH), r.nextInt(HEIGHT));
        }

        // 画字
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(Color.BLACK);
        int x = 15;
        for (char c : code.toCharArray()) {
            g.drawString(String.valueOf(c), x, 30);
            x += 25;
        }

        g.dispose();
        return image;
    }
}