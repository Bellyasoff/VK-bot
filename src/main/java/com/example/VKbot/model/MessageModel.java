package com.example.VKbot.model;

import lombok.Data;

@Data
public class MessageModel {
    private int user_id;
    private String text;
    private int random_id;
    private long response;
}
