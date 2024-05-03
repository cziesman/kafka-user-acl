package com.redhat.kafka.client.controller;

import java.util.List;

import com.redhat.kafka.client.service.DynamicConsumer;
import com.redhat.kafka.client.service.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/msg")
public class ConsumerController {

    @Autowired
    private DynamicConsumer dynamicConsumer;

    @GetMapping({
            "/index", "/", ""
    })
    public String index() {

        return "redirect:/msg/list";
    }

    @GetMapping(value = "/list")
    public String list(Model model) {

        model.addAttribute("messages", dynamicConsumer.getMessages());

        return "message-list";
    }

    @GetMapping(value = "/list-json")
    @ResponseBody
    public Wrapper listAsJson() {

        return new Wrapper(dynamicConsumer.getMessages());
    }

    /*
     * This is needed because Datatables expects the response to be a JSON object
     * named "data," which in turn contains the list of messages.
     */
    record Wrapper(List<Message> data) {

    }

}
