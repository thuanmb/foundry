package com.thuanbui.foundry.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController extends BaseApiController {

    @GetMapping("/")
    public Map<String, String> index() {
        return Map.of("status", "UP");
    }
}
