package com.example.VKbot.Message;

public class MessageBuilder {

    /*
        Строим шаблон сообщения, отправляемого ботом, вида ->
        "Вы сказали: <Ваше сообщение>"
     */

    public String answer(String message) {
        return "Вы сказали: " + message;
    }
}
