package com.meigy.jstress.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.util.*;

/**
 * @Author Meigy
 * @Description 对象还没有描述
 * @Date 2024-12-14 16:04
 **/
@Service
public class LogService {

    @Value("${logging.file.name}")
    private String logFilePath;

    public Map<String, Object> getLogs(int lines) {
        Map<String, Object> result = new HashMap<>();
        List<String> logLines = new ArrayList<>();

        File file = new File(logFilePath);
        if (!file.exists()) {
            result.put("success", false);
            result.put("error", "日志文件不存在");
            return result;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file)))) {
            // 使用循环队列保存最后N行
            LinkedList<String> lastLines = new LinkedList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lastLines.add(line);
                if (lastLines.size() > lines) {
                    lastLines.removeFirst();
                }
            }

            logLines.addAll(lastLines);
            result.put("success", true);
            result.put("data", logLines);
        } catch (IOException e) {
            result.put("success", false);
            result.put("error", "读取日志文件失败: " + e.getMessage());
        }

        return result;
    }
}
