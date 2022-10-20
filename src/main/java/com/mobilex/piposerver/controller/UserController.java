package com.mobilex.piposerver.controller;

import com.mobilex.piposerver.dto.ResponseDTO;
import com.mobilex.piposerver.dto.UserDTO;
import com.mobilex.piposerver.exception.MalformedUserException;
import com.mobilex.piposerver.model.UserEntity;
import com.mobilex.piposerver.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;


@Slf4j
@RestController
@RequestMapping("user")
public class UserController {
    private static final String TAG = "UserController";

    @Autowired
    private UserService userService;

    @GetMapping("auth")
    public ResponseEntity<?> auth(@RequestHeader MultiValueMap<String, String> headers) {
        String idToken = headers.getFirst("x-auth-token");
        if (idToken == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            String userId = userService.getUserIdFromIdToken(idToken);
            String userName = userService.getUserNameFromIdToken(idToken);
            if (userId == null) {
                ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("cannot found user from auth").build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
            }
            UserEntity userEntity = UserEntity.builder()
                    .id(userId)
                    .name(userName)
                    .build();
            UserDTO userDTO = UserDTO.builder()
                    .jwt(userService.getJwtStringFromUser(userEntity))
                    .name(userName)
                    .build();
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().data(userDTO).build();
            return ResponseEntity.ok().body(responseDTO);
        } catch (MalformedUserException | GeneralSecurityException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("malformed user id token").build();
            return ResponseEntity.badRequest().body(responseDTO);
        } catch (IOException e) {
            log.error(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("internal server error").build();
            return ResponseEntity.internalServerError().body(responseDTO);
        }
    }

}
