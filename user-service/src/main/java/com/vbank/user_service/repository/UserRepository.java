// enables interaction with the database for User entities
//by extending the jpaRepository interface.
//also provides methods to find users by username and email.

package com.vbank.user_service.repository;
import com.vbank.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // allow using optional to handle null values gracefully
import java.util.UUID;

@Repository // indicates that this interface is a repository component in the Spring context
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username); // method to find a user by their username (Query method)
    Optional<User> findByEmail(String email); // method to find a user by their email (Query method)
}
