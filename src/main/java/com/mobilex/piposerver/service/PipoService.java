package com.mobilex.piposerver.service;

import com.mobilex.piposerver.exception.*;
import com.mobilex.piposerver.model.PipoEntity;
import com.mobilex.piposerver.model.UserEntity;
import com.mobilex.piposerver.persistance.PipoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PipoService {
    private static final String ORIGIN_PATH = "/files/origin";
    private static final String PROCESSED_PATH = "/files/processed";
    private static final String PREVIEW_PATH = "/files/preview";

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private PipoRepository pipoRepository;

    public PipoEntity createOrigin(@NonNull MultipartFile multipartFile, @Nullable UserEntity userEntity) throws MultipartFileException, IOException {
        String contentType = multipartFile.getContentType();
        if (contentType == null) throw new MultipartFileException();
        String imageType = validateImageType(contentType);
        if (imageType == null) throw new MultipartFileException();

        PipoEntity newEntity = PipoEntity.builder()
                .userId(userEntity != null ? userEntity.getId() : null)
                .createdAt(LocalDateTime.now())
                .temp(userEntity == null)
                .type(imageType)
                .build();
        newEntity = pipoRepository.save(newEntity);

        String originDirectory = servletContext.getRealPath(ORIGIN_PATH);
        Files.createDirectories(Paths.get(originDirectory));
        File saveFile = new File(originDirectory, newEntity.getId() + "." + imageType);
        multipartFile.transferTo(saveFile);
        log.info("createOrigin : origin file : {}", saveFile.getAbsolutePath());

        return newEntity;
    }

    public PipoEntity processOrigin(@NonNull PipoEntity pipoEntity, @Nullable UserEntity userEntity) throws MalformedPipoException, UnknownPipoException, PipoFileNotFoundException, IOException, NotAuthorizedException {
        int difficulty = pipoEntity.getDifficulty();
        if (difficulty < 1 || difficulty > 3 || pipoEntity.getId() == null) throw new MalformedPipoException();
        pipoEntity = getValidPipoEntity(pipoEntity);
        pipoEntity.setDifficulty(difficulty);

        if (pipoEntity.getUserId() != null) {
            if (userEntity == null || userEntity.getId() == null) throw new NotAuthorizedException();
            if (!pipoEntity.getUserId().equals(userEntity.getId())) throw new NotAuthorizedException();
        }

        String processedDirectory = servletContext.getRealPath(PROCESSED_PATH);
        String originDirectory = servletContext.getRealPath(ORIGIN_PATH);
        String previewDirectory = servletContext.getRealPath(PREVIEW_PATH);

        File originFile = new File(originDirectory, pipoEntity.getId() + "." + pipoEntity.getType());
        File processedFile = new File(processedDirectory, pipoEntity.getId() + "." + pipoEntity.getType());
        File previewFile = new File(previewDirectory, pipoEntity.getId() + "." + pipoEntity.getType());
        if (!originFile.exists()) throw new PipoFileNotFoundException();

        Files.createDirectories(Paths.get(processedDirectory));
        Files.createDirectories(Paths.get(previewDirectory));
        processPipo(originFile.getAbsolutePath(), processedFile.getAbsolutePath(), previewFile.getAbsolutePath(), difficulty);
        pipoEntity.setProcessed(true);
        log.info("processOrigin : processed file : {}", processedFile.getAbsolutePath());
        return pipoRepository.save(pipoEntity);
    }

    public List<PipoEntity> deleteTempEntities(@NonNull LocalDateTime lessThanDateTime) {
        List<PipoEntity> entities = pipoRepository.findAllByTempTrueAndCreatedAtLessThan(lessThanDateTime);
        pipoRepository.deleteAll(entities);
        return entities;
    }

    public void deleteFileFromEntity(@NonNull PipoEntity pipoEntity) throws IOException, MalformedPipoException {
        if (pipoEntity.getId() == null) throw new MalformedPipoException();
        String processedDirectory = servletContext.getRealPath(PROCESSED_PATH);
        String originDirectory = servletContext.getRealPath(ORIGIN_PATH);
        String previewDirectory = servletContext.getRealPath(PREVIEW_PATH);
        File originFile = new File(originDirectory, pipoEntity.getId() + "." + pipoEntity.getType());
        File processedFile = new File(processedDirectory, pipoEntity.getId() + "." + pipoEntity.getType());
        File previewFile = new File(previewDirectory, pipoEntity.getId() + "." + pipoEntity.getType());
        Files.deleteIfExists(Paths.get(originFile.toURI()));
        Files.deleteIfExists(Paths.get(processedFile.toURI()));
        Files.deleteIfExists(Paths.get(previewFile.toURI()));
    }

    @NonNull
    public File getPipoPreviewFile(@NonNull PipoEntity pipoEntity, @Nullable UserEntity userEntity) throws UnknownPipoException, MalformedPipoException, NotAuthorizedException, PipoNotProcessedException, PipoFileNotFoundException {
        pipoEntity = getValidPipoEntity(pipoEntity);
        if (!pipoEntity.isTemp()) {
            if (userEntity == null || userEntity.getId() == null) throw new NotAuthorizedException();
            if (!pipoEntity.getUserId().equals(userEntity.getId())) throw new NotAuthorizedException();
        }
        if (!pipoEntity.isProcessed()) throw new PipoNotProcessedException();
        String previewDirectory = servletContext.getRealPath(PREVIEW_PATH);
        File previewFile = new File(previewDirectory, pipoEntity.getId() + "." + pipoEntity.getType());
        if (!previewFile.exists()) throw new PipoFileNotFoundException();
        return previewFile;
    }

    @NonNull
    public File getPipoProcessedFile(@NonNull PipoEntity pipoEntity, @Nullable UserEntity userEntity) throws MalformedPipoException, UnknownPipoException, PipoNotProcessedException, PipoFileNotFoundException, NotAuthorizedException {
        pipoEntity = getValidPipoEntity(pipoEntity);
        if (!pipoEntity.isTemp()) {
            if (userEntity == null || userEntity.getId() == null) throw new NotAuthorizedException();
            if (!pipoEntity.getUserId().equals(userEntity.getId())) throw new NotAuthorizedException();
        }
        if (!pipoEntity.isProcessed()) throw new PipoNotProcessedException();
        String processedDirectory = servletContext.getRealPath(PROCESSED_PATH);
        File previewFile = new File(processedDirectory, pipoEntity.getId() + "." + pipoEntity.getType());
        if (!previewFile.exists()) throw new PipoFileNotFoundException();
        return previewFile;
    }

    @NonNull
    public File getPipoOriginFile(@NonNull PipoEntity pipoEntity, @Nullable UserEntity userEntity) throws UnknownPipoException, PipoFileNotFoundException, MalformedPipoException, NotAuthorizedException {
        pipoEntity = getValidPipoEntity(pipoEntity);
        if (!pipoEntity.isTemp()) {
            if (userEntity == null || userEntity.getId() == null) throw new NotAuthorizedException();
            if (!pipoEntity.getUserId().equals(userEntity.getId())) throw new NotAuthorizedException();
        }
        String originDirectory = servletContext.getRealPath(ORIGIN_PATH);
        File previewFile = new File(originDirectory, pipoEntity.getId() + "." + pipoEntity.getType());
        if (!previewFile.exists()) throw new PipoFileNotFoundException();
        return previewFile;
    }

    @NonNull
    private PipoEntity getValidPipoEntity(@NonNull PipoEntity entity) throws UnknownPipoException, MalformedPipoException {
        if (entity.getId() == null || entity.getId().isEmpty()) throw new MalformedPipoException();
        Optional<PipoEntity> foundEntity = pipoRepository.findById(entity.getId());
        if (!foundEntity.isPresent()) throw new UnknownPipoException();
        return foundEntity.get();
    }

    public List<PipoEntity> getUserPipoList(UserEntity userEntity) {
        return pipoRepository.findAllByUserId(userEntity.getId());
    }

    public void setPipoUser(@NonNull PipoEntity pipoEntity, @NonNull UserEntity userEntity) throws MalformedPipoException, UnknownPipoException, UserNotFoundException, NotAuthorizedException {
        pipoEntity = getValidPipoEntity(pipoEntity);
        if (!pipoEntity.isTemp()) throw new NotAuthorizedException();
        pipoEntity.setUserId(userEntity.getId());
        pipoEntity.setTemp(false);
        pipoRepository.save(pipoEntity);
    }

    @Nullable
    private String validateImageType(@NonNull String contentType) {
        if (!contentType.startsWith("image")) return null;
        String[] types = contentType.split("/");
        if (types.length < 2) return null;
        if (types[1].equals("png") || types[1].equals("gif") ||
                types[1].equals("jpeg") || types[1].equals("bmp") || types[1].equals("webp"))
            return types[1];
        return null;
    }

    private native void processPipo(String originFile, String processedFile, String previewFile, int difficulty)
            throws IOException;
}
