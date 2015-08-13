package com.sungardas.snapdirector.rest;

import com.sungardas.snapdirector.rest.utils.JsonFromFile;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;


@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    @Autowired
    private ServletContext context;


    @RequestMapping(method = RequestMethod.GET)
    public String getSchedule() {
        String path = context.getInitParameter("rest:mock-directory");
        String result = null;
        try {
            JSONArray schedule = JsonFromFile.newJSONArray(path + "schedules.json");
            result = schedule.toString();
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
        return result;
    }
}
