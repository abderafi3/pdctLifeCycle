package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.checkmk.pdctLifeCycle.service.HostLiveInfoService;
import com.checkmk.pdctLifeCycle.service.HostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/hosts")
public class HostRestController {

    @Autowired
    public HostService hostService;

    @Autowired
    public HostLiveInfoService hostLiveInfoService;

    @GetMapping
    public List<Host> getAllDatabaseHosts(){
        return hostService.getAllHosts();
    }

    @GetMapping("info")
    public List<HostLiveInfo> getAllHostInfo() throws Exception{
        return hostLiveInfoService.convertToHostLiveInfo();
    }

    @PostMapping
    public Host addHost(@RequestBody Host host) throws HostServiceException {
        return hostService.addHost(host);
    }

    @PutMapping("{id}")
    public Host updateHost(@PathVariable String id, @RequestBody Host host) throws HostServiceException{
        return hostService.updateHost(host);
    }

    @DeleteMapping("{id}")
    public void deleteHost(@PathVariable String id) throws HostServiceException{
        hostService.deleteHost(id);
    }
}
