package com.deveagles.be15_deveagles_be.features.messages.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageDeliveryStatus;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageSendingType;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.Sms;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsRepository extends JpaRepository<Sms, Long> {

  List<Sms> findAllByMessageSendingTypeAndScheduledAtLessThanEqualAndMessageDeliveryStatus(
      MessageSendingType messageSendingType,
      LocalDateTime scheduledAt,
      MessageDeliveryStatus messageDeliveryStatus);

  Optional<Sms> findByMessageIdAndShopId(Long messageId, Long shopId);
}
