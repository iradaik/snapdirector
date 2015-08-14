package com.sungardas.snapdirector.dto.converter;

import com.sungardas.snapdirector.aws.dynamodb.model.User;
import com.sungardas.snapdirector.dto.UserDto;

import java.util.ArrayList;
import java.util.List;

public final class UserDtoConverter {
    private UserDtoConverter() {
    }

    public static UserDto convert(User user){
        UserDto userDto = new UserDto();

        userDto.setAdmin("admin".equals(user.getRole()));
        userDto.setEmail(user.getEmail());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());

        return userDto;
    }

    public static List<UserDto> convert(Iterable<User> users){
        List<UserDto> dtos = new ArrayList<>();
        for(User user: users){
            dtos.add(convert(user));
        }
        return dtos;
    }
}
