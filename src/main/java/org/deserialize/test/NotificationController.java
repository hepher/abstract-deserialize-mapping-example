package org.deserialize.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
public class NotificationController {
  
  @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping(value = "message")
    public String getMessage() {
        return "Hello word - Notification";
    }

    @PostMapping("send/message")
    public String sendPushMessage(@RequestParam("message") String message, @RequestParam(value = "name", required = false) String name) {

        NotificationResponse response = new NotificationResponse();
        response.setMessage("Notifica in arrivo: " + message);
        response.setOriginal(message);
        response.setSender(name);

        messagingTemplate.convertAndSend("/topic/pushmessages", response);

        return response.getMessage();
    }

    @PostMapping("send/subscribed/message")
    public String sendSubscribedPushMessage(@RequestParam("message") String message, @RequestParam(value = "name", required = false) String name) {
        NotificationResponse response = new NotificationResponse();
        response.setMessage("Notifica solo ai registrati: " + message);
        response.setOriginal(message);
        response.setSender(name);

        messagingTemplate.convertAndSend("/topic/name/*", response);

        return response.getMessage();
    }

    @PostMapping("send/message/pluto")
    public String sendMessagePluto(@RequestParam("message") String message, @RequestParam(value = "name", required = false) String name) {
        NotificationResponse response = new NotificationResponse();
        response.setMessage("Notifica solo a pluto: " + message);
        response.setOriginal(message);
        response.setSender(name);

//        "/topic/warehouse/*", "topic/warehouse/*/product/*", "/topic/product/*";
        messagingTemplate.convertAndSend("/topic/name/pluto", response);

        return response.getMessage();
    }

    @PostMapping("prova/entity")
    public ResponseEntity<HashMap<String, String>> prova() {
        HashMap<String, String> map = new HashMap<>();
        map.put("prova", "prova");
        map.put("prova2", "prova2");

        return new ResponseEntity<>(map, HttpStatus.OK);
    }
}
