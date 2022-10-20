package com.mobilex.piposerver.persistance;

import com.mobilex.piposerver.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
}
