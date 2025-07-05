package com.deveagles.be15_deveagles_be.features.messages.query.repository;

import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageTemplateQueryRepository {
  Page<MessageTemplate> findAllByShopId(Long shopId, Pageable pageable);

  Optional<MessageTemplate> findByIdAndNotDeleted(Long id);
}
