package com.sungardas.snapdirector.rest;

import com.amazonaws.regions.Regions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/regions")
public class RegionsController {


    @RequestMapping(method = RequestMethod.GET)
    public String getRegions() {
        JSONObject record = null;
        JSONArray regionsJson = new JSONArray();
        for (Regions nextRegion : Regions.values()) {
            record = new JSONObject();
            record.put("id", nextRegion.getName());
            record.put("name", nextRegion.getName());
            regionsJson.put(record);
        }
        return regionsJson.toString();
    }

}
