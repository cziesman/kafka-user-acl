package com.redhat.kafka.client.controller;

import java.util.List;

import com.redhat.kafka.client.service.DynamicConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/demo")
public class DemoController {

    private static final Logger LOG = LoggerFactory.getLogger(DemoController.class);

    private static final String SEND_ERROR_MESSAGE = "send-error-message";

    @Value("${server.port}")
    private int serverPort;

    @Autowired
    private DynamicConsumer dynamicConsumer;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping({
            "/index", "/", ""
    })
    public String index() {

        return "redirect:/demo/send";
    }

    @GetMapping("/send")
    public String send(Model model) {

        MessageForm messageForm = new MessageForm();
        model.addAttribute("messageForm", messageForm);

        return "message-send";
    }

    @PostMapping(path = "send", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String send(MessageForm messageForm, Model model) {

        LOG.info(messageForm.toString());

        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host("localhost")
                .port(serverPort)
                .path("/api/send")
                .query("topic=" + messageForm.getTopic())
                .query("username=" + messageForm.getUsername())
                .query("message=" + messageForm.getMessage())
                .build();

        ResponseEntity<Void> response = restTemplate.getForEntity(uriComponents.toUriString(), Void.class);

        Result result = new Result(response.getStatusCode().value(), messageForm.getMessage());
        model.addAttribute("result", result);

        return "success";
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public String handleException(HttpServerErrorException ex, Model model) {

        List<String> errors = ex.getResponseHeaders().get(SEND_ERROR_MESSAGE);

        Result result = new Result(ex.getStatusCode().value(), errors.get(0));
        model.addAttribute("result", result);

        return "failure";
    }

    record Result(int status, String message) {

    }
}
