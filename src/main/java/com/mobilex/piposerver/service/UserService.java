package com.mobilex.piposerver.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mobilex.piposerver.exception.*;
import com.mobilex.piposerver.model.UserEntity;
import com.mobilex.piposerver.persistance.UserRepository;
import com.mobilex.piposerver.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${oauth_client_id}")
    private String clientId;

    private GoogleIdTokenVerifier googleVerifier;

    private GoogleIdToken getVerifiedIdToken(@NonNull String idTokenString) throws GeneralSecurityException, IOException {
        if (googleVerifier == null) {
            googleVerifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();
        }
        return googleVerifier.verify(idTokenString);
    }

    @Nullable
    public String getUserIdFromIdToken(@NonNull String idTokenString) throws GeneralSecurityException, IOException {
        GoogleIdToken idToken = getVerifiedIdToken(idTokenString);
        if (idToken == null) return null;
        return idToken.getPayload().getSubject();
    }

    @Nullable
    public String getUserNameFromIdToken(@NonNull String idTokenString) throws GeneralSecurityException, IOException {
        GoogleIdToken idToken = getVerifiedIdToken(idTokenString);
        if (idToken == null) return null;
        return (String) idToken.getPayload().get("name");
    }

    public String getJwtStringFromUser(@NonNull UserEntity entity) throws MalformedUserException {
        if (entity.getId() == null) throw new MalformedUserException();
        Optional<UserEntity> userEntity = userRepository.findById(entity.getId());
        if (!userEntity.isPresent())
            userRepository.save(entity);
        return jwtTokenProvider.createToken(entity.getId());
    }

    @NonNull
    public UserEntity getUserFromJwtString(@NonNull String jwtString) throws UserNotFoundException, JwtException {
        if (!jwtTokenProvider.isValidJwtString(jwtString)) throw new JwtException(null);
        String userId = jwtTokenProvider.getUserIdFromJwt(jwtString);
        if (userId == null) throw new MalformedJwtException(null);
        Optional<UserEntity> userEntity = userRepository.findById(userId);
        if (!userEntity.isPresent()) throw new UserNotFoundException();
        return userEntity.get();
    }
}
