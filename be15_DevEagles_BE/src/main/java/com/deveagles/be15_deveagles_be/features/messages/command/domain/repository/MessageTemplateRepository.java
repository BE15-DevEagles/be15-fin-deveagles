package com.deveagles.be15_deveagles_be.features.messages.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {}
