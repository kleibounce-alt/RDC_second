package com.klei.message.init;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.utils.LogUtil;
import com.klei.common.utils.RedisUtil;
import com.klei.message.entity.SensitiveWord;
import com.klei.message.mapper.SensitiveWordMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class SensitiveWordInit {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    public void load() {
        // 1. 先清空，再从数据库加载
        List<SensitiveWord> list = sensitiveWordMapper.findAll();
        RedisUtil.del("sensitive:words");
        if (list != null && !list.isEmpty()) {
            String[] words = list.stream()
                    .map(SensitiveWord::getWord)
                    .filter(w -> w != null && !w.isEmpty())
                    .toArray(String[]::new);
            if (words.length > 0) {
                RedisUtil.sadd("sensitive:words", words);
            }
        }

        // 2. 从本地文件热加载（classpath: sensitive_words.txt），合并到 Redis
        int fileCount = loadFromFile();

        LogUtil.info("敏感词加载完成，数据库: " + (list == null ? 0 : list.size())
                + " 条，本地文件补充: " + fileCount + " 条");
    }

    private int loadFromFile() {
        int count = 0;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sensitive_words.txt")) {
            if (is == null) {
                LogUtil.warn("未找到 classpath:sensitive_words.txt，跳过本地文件加载");
                return 0;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim();
                    if (word.isEmpty() || word.startsWith("#")) {
                        continue; // 跳过空行和注释行
                    }
                    RedisUtil.sadd("sensitive:words", word);
                    count++;
                }
            }
        } catch (IOException e) {
            LogUtil.error("读取本地敏感词文件失败", e);
        }
        return count;
    }
}