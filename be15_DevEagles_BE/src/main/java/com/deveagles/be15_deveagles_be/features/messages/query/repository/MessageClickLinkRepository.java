package com.deveagles.be15_deveagles_be.features.messages.query.repository;

import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageClickLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageClickLinkRepository extends JpaRepository<MessageClickLink, Long> {
  Optional<MessageClickLink> findByShortToken(String token);
}
