package com.meigy.jstress.controller;

import com.meigy.jstress.service.LogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    
    @Resource
    private LogService logService;

    @GetMapping
    public Map<String, Object> getLogs(@RequestParam(defaultValue = "100") int lines) {
        return logService.getLogs(lines);
    }
} 