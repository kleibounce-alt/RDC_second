package com.klei.admin.service.impl;

import com.klei.admin.service.SensitiveWordService;
import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.common.utils.RedisUtil;
import com.klei.message.entity.SensitiveWord;
import com.klei.message.mapper.SensitiveWordMapper;

@Component
public class SensitiveWordServiceImpl implements SensitiveWordService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Override
    @Transactional
    public void addWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            throw new BusinessException("敏感词不能为空");
        }
        String w = word.trim();
        SensitiveWord exist = sensitiveWordMapper.findByWord(w);
        if (exist != null && exist.getIsDeleted() == 0) {
            throw new BusinessException("敏感词已存在");
        }
        if (exist != null) {
            sensitiveWordMapper.restoreById(exist.getId());
        } else {
            sensitiveWordMapper.insert(w);
        }
        RedisUtil.sadd("sensitive:words", w);
    }

    @Override
    @Transactional
    public void deleteWord(Long id) {
        SensitiveWord word = sensitiveWordMapper.findById(id);
        if (word == null || word.getIsDeleted() == 1) {
            throw new BusinessException("敏感词不存在");
        }
        sensitiveWordMapper.deleteById(id);
        RedisUtil.srem("sensitive:words", word.getWord());
    }
}