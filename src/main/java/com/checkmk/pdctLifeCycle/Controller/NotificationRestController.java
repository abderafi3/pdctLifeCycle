package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/notifications")
public class NotificationRestController {

    @Autowired
   public NotificationService notificationService;

    @GetMapping
    public List<HostNotification> fetchNotificationsFromCheckmk() throws Exception{
       return  notificationService.fetchNotificationsFromCheckmk();
    }



}
