package com.sungardas.snapdirector.rest;

import com.sungardas.snapdirector.aws.dynamodb.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController("/user")
public class UserController {

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createUser(@ModelAttribute("user") User newUser) {
        ResponseEntity<String> responseEntity;
        // check whether user with the same email already exists
        //TODO: try to create user here
//        if ( != null) {
//            responseEntity = new ResponseEntity<String>("User with such email already exists!", HttpStatus.INTERNAL_SERVER_ERROR);
//        }
        try {
            //TODO: try to create user here
            responseEntity = new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {

            responseEntity = new ResponseEntity<>("Failed to create user due to server error.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;
    }

}
