package com.mobilex.piposerver.controller;

import com.mobilex.piposerver.dto.PipoDTO;
import com.mobilex.piposerver.dto.ResponseDTO;
import com.mobilex.piposerver.exception.*;
import com.mobilex.piposerver.model.PipoEntity;
import com.mobilex.piposerver.model.UserEntity;
import com.mobilex.piposerver.service.PipoService;
import com.mobilex.piposerver.service.UserService;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLConnection;
import java.util.List;

@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("pipo")
public class PipoController {
    private static final String TAG = "PipoController";

    @Autowired
    private PipoService pipoService;

    @Autowired
    private UserService userService;

    @PostMapping(path = "new", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> uploadOrigin(
            @RequestParam(value = "file") MultipartFile multipartFile,
            @RequestHeader MultiValueMap<String, String> headers
    ) {
        try {
            UserEntity userEntity = getUserEntityFromHeaderAuth(headers);
            PipoEntity newEntity = pipoService.createOrigin(multipartFile, userEntity);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().data(newEntity).build();
            return ResponseEntity.ok().body(responseDTO);
        }
        catch (MultipartFileException | MultipartException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("file is not available image").build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDTO);
        }
        catch (IOException e) {
            log.error(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("internal server error").build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        } catch (UserNotFoundException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("cannot found user from auth").build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDTO);
        } catch (JwtException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("invalid JWT").build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDTO);
        }
    }

    @PutMapping(path = "process")
    public ResponseEntity<?> processOrigin(
            @RequestBody PipoDTO pipoDTO,
            @RequestHeader MultiValueMap<String, String> headers
    ) {
        try {
            UserEntity userEntity = getUserEntityFromHeaderAuth(headers);
            PipoEntity pipoEntity = pipoService.processOrigin(pipoDTO.toEntity(), userEntity);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().data(pipoEntity).build();
            return ResponseEntity.ok().body(responseDTO);
        }
        catch (MalformedPipoException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("malformed Parameters").build();
            return ResponseEntity.badRequest().body(responseDTO);
        }
        catch (UnknownPipoException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("cannot found saved").build();
            return ResponseEntity.badRequest().body(responseDTO);
        }
        catch (IOException | PipoFileNotFoundException e) {
            log.error(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("internal server error").build();
            return ResponseEntity.internalServerError().body(responseDTO);
        } catch (UserNotFoundException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("cannot found user from auth").build();
            return ResponseEntity.badRequest().body(responseDTO);
        } catch (NotAuthorizedException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("not permitted").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
        } catch (JwtException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("invalid JWT").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
        }
    }

    @GetMapping(value = "{option}/{pipoId}")
    public ResponseEntity<?> pipoImage(
            @PathVariable String option, @PathVariable String pipoId,
            @RequestHeader MultiValueMap<String, String> headers
    ) {
        try {
            UserEntity userEntity = getUserEntityFromHeaderAuth(headers);
            File file;
            switch (option) {
                case "origin":
                    file = pipoService.getPipoOriginFile(PipoEntity.builder().id(pipoId).build(), userEntity);
                    break;
                case "preview":
                    file = pipoService.getPipoPreviewFile(PipoEntity.builder().id(pipoId).build(), userEntity);
                    break;
                case "processed":
                    file = pipoService.getPipoProcessedFile(PipoEntity.builder().id(pipoId).build(), userEntity);
                    break;
                default:
                    return ResponseEntity.notFound().build();
            }
            String contentType = URLConnection.guessContentTypeFromName(file.getName());
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(new FileInputStream(file)));
        } catch (MalformedPipoException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("malformed Parameters").build();
            return ResponseEntity.badRequest().body(responseDTO);
        } catch (UnknownPipoException | PipoNotProcessedException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("cannot found saved").build();
            return ResponseEntity.badRequest().body(responseDTO);
        } catch (PipoFileNotFoundException | FileNotFoundException e) {
            log.error(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("internal server error").build();
            return ResponseEntity.internalServerError().body(responseDTO);
        } catch (UserNotFoundException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("cannot found user from auth").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
        } catch (NotAuthorizedException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("not permitted").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
        } catch (JwtException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("invalid JWT").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
        }
    }

    @GetMapping(value = "list")
    public ResponseEntity<?> pipoList(
            @RequestHeader MultiValueMap<String, String> headers
    ) {
        try {
            UserEntity userEntity = getUserEntityFromHeaderAuth(headers);
            if (userEntity == null) {
                ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("not permitted").build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
            }
            List<PipoEntity> pipoList = pipoService.getUserPipoList(userEntity);
            return ResponseEntity.ok().body(ResponseDTO.builder().data(pipoList).build());
        } catch (UserNotFoundException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("cannot found user from auth").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
        } catch (JwtException e) {
            log.debug(TAG, e);
            ResponseDTO<Object> responseDTO = ResponseDTO.builder().error("invalid JWT").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
        }
    }

    @Nullable
    private UserEntity getUserEntityFromHeaderAuth(MultiValueMap<String, String> headers) throws UserNotFoundException, JwtException {
        String jwtString = headers.getFirst("authorization");
        if (jwtString == null || jwtString.length() < 8) return null;
        jwtString = jwtString.substring(7);
        return userService.getUserFromJwtString(jwtString);
    }
}
