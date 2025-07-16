package com.deveagles.be15_deveagles_be.features.messages.query.repository;

import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageClickLinkLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageClickLinkLogRepository extends JpaRepository<MessageClickLinkLog, Long> {
  boolean existsByMessageLinkId(Long messageLinkId);
}
