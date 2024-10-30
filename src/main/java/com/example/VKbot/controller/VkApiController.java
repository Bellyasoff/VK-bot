package com.example.VKbot.controller;

import com.example.VKbot.Message.MessageBuilder;
import com.example.VKbot.model.ConnectionModel;
import com.example.VKbot.model.MessageModel;
import lombok.extern.log4j.Log4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

@RestController
@Log4j
public class VkApiController {

    @Value("${ACCESS_TOKEN}")
    private String ACCESS_TOKEN;
    private final MessageModel messageModel = new MessageModel();
    private final ConnectionModel connectionModel = new ConnectionModel();
    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    @RequestMapping(method = RequestMethod.POST, consumes = "application/json")
    public @ResponseBody String getMessage(@RequestBody String jsonResponse) {

        JSONObject jsonObject = new JSONObject(jsonResponse);

        connectionModel.setGroup_id(jsonObject.get("group_id").toString());
        connectionModel.setType(jsonObject.get("type").toString());
        connectionModel.setVersion(jsonObject.get("v").toString());

        if (connectionModel.getType().equals("confirmation")) {
            String serverResponse = getConfirmationCode();
            log.debug(serverResponse);

        /*###
        Подключаемся к api вконтакте для получения ответа с кодом в формате json, вида ->

        {
           "response": {
             "code": "2363c83c"
           }
         }
         */

            StringBuilder builder = new StringBuilder();

            try {
                URL url = new URL(serverResponse);
                URLConnection connection = url.openConnection();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    builder.append(line + "\n");
                }
                bufferedReader.close();
            } catch (IOException e) {
                log.error(e);
            }

            JSONObject responseObject = new JSONObject(builder.toString());
            String code = responseObject.getJSONObject("response").getString("code");
            log.info(code);

            /*###
                Парсим код из json и возвращаем его, для установки подключения к серверу
             */

            return code;
        } else if (connectionModel.getType().equals("message_new")) {
            JSONObject message = jsonObject.getJSONObject("object").getJSONObject("message");

            /*
                Получаем в ответ json формата ->
    {
    "group_id": 227991875,
    "type": "message_new",
    "event_id": "c70e18007c57caf46ac2fe52f0ac22036b0a8f72",
    "v": "5.199",
    "object": {
        "message": {
            "date": 1730286227,
            "from_id": 101719262,
            "id": 185,
            "version": 10000389,
            "out": 0,
            "important": false,
            "is_hidden": false,
            "attachments": [],
            "conversation_message_id": 182,
            "fwd_messages": [],
            "text": "test",
            "peer_id": 101719262,
            "random_id": 0
        },
        "client_info": {
            "button_actions": [
                "text",
                "vkpay",
                "open_app",
                "location",
                "open_link",
                "callback",
                "intent_subscribe",
                "intent_unsubscribe"
            ],
            "keyboard": true,
            "inline_keyboard": true,
            "carousel": true,
            "lang_id": 3
        }
    }
}

        Забираем нужные поля и присваиваем их переменным в MessageModel
             */

            messageModel.setUser_id(message.getInt("peer_id"));
            messageModel.setText(message.getString("text"));

            MessageBuilder messageBuilder = new MessageBuilder();

            if (messageModel.getText() != null) {
                String botAnswer = botResponse(messageBuilder.answer(messageModel.getText()));
                log.info(botAnswer);

                try {
                    restTemplate.getForObject(botAnswer, String.class);
                } catch (Exception e) {
                    log.error(e);
                }
                return "ok";
            }
        }
        return "Unsupported event";
    }

    private String botResponse(String message) {
        //### URL для отправки сообщения
        return ("https://api.vk.com/method/messages.send?" +
                "user_id=" + messageModel.getUser_id() +
                "&random_id=" + random.nextInt() +
                "&message=" + message +
                "&access_token=" + ACCESS_TOKEN +
                "&v=" + connectionModel.getVersion());
    }

    private String getConfirmationCode() {
        //### URL для получения кода подтверждения
        return ("https://api.vk.com/method/groups.getCallbackConfirmationCode?" +
                "group_id=" + connectionModel.getGroup_id() + "&access_token=" + ACCESS_TOKEN +
                "&v=" + connectionModel.getVersion() +
                "&type=" + connectionModel.getType());
    }
}
