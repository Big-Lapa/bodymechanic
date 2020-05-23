package com.lapin.bodymechanic.controller;

import com.lapin.bodymechanic.repository.DiaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class DiaryController {

    @Autowired
    DiaryRepository diaryRepository;
}
