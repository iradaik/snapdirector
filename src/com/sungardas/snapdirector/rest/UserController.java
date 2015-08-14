package com.sungardas.snapdirector.rest;

import com.sungardas.snapdirector.aws.dynamodb.model.User;
import com.sungardas.snapdirector.aws.dynamodb.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createUser(@ModelAttribute("user") User newUser) {
        ResponseEntity<String> responseEntity;

        // check whether user with the same email already exists
        if (userRepository.exists(newUser.getEmail())) {
            responseEntity = new ResponseEntity<>("User with such email already exists.", HttpStatus.INTERNAL_SERVER_ERROR);
            return responseEntity;
        }
        try {
            userRepository.save(newUser);
            responseEntity = new ResponseEntity<>("User was successfully created.", HttpStatus.OK);
        } catch (Exception e) {
            responseEntity = new ResponseEntity<>("Failed to create user due to server error.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return responseEntity;
    }

}
