package com.deveagles.be15_deveagles_be.features.messages.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.AutomaticEventType;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {
  boolean existsByShopIdAndAutomaticEventType(Long shopId, AutomaticEventType eventType);

  List<MessageTemplate> findByShopIdAndAutomaticEventTypeIsNotNull(Long shopId);
}
