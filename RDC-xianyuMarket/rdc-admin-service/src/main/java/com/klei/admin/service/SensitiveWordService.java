package com.klei.admin.service;

import com.klei.message.entity.SensitiveWord;
import java.util.List;

public interface SensitiveWordService {

    void addWord(String word);

    void deleteWord(Long id);

    List<SensitiveWord> listAll();
}