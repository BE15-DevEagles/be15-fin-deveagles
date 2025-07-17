package com.deveagles.be15_deveagles_be.features.messages.query.repository.impl;

import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.AutomaticEventType;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.QMessageTemplate;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.MessageTemplateQueryRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MessageTemplateQueryRepositoryImpl implements MessageTemplateQueryRepository {

  private final JPAQueryFactory queryFactory;
  private final QMessageTemplate template = QMessageTemplate.messageTemplate;

  @Override
  public Page<MessageTemplate> findAllByShopId(Long shopId, Pageable pageable) {
    List<MessageTemplate> content =
        queryFactory
            .selectFrom(template)
            .where(template.shopId.eq(shopId), template.deletedAt.isNull())
            .orderBy(template.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total =
        queryFactory
            .select(template.count())
            .from(template)
            .where(template.shopId.eq(shopId), template.deletedAt.isNull())
            .fetchOne();

    return new PageImpl<>(content, pageable, total);
  }

  @Override
  public Optional<MessageTemplate> findByIdAndNotDeleted(Long id) {
    MessageTemplate result =
        queryFactory
            .selectFrom(template)
            .where(template.templateId.eq(id), template.deletedAt.isNull())
            .fetchOne();

    return Optional.ofNullable(result);
  }

  @Override
  public Optional<MessageTemplate> findActiveTemplate(Long shopId, AutomaticEventType triggerType) {
    QMessageTemplate template = QMessageTemplate.messageTemplate;

    MessageTemplate result =
        queryFactory
            .selectFrom(template)
            .where(
                template.shopId.eq(shopId),
                template.deletedAt.isNull(),
                template.automaticEventType.eq(triggerType) //  핵심 조건
                )
            .orderBy(template.createdAt.desc()) // 최신 템플릿 우선
            .fetchFirst();

    return Optional.ofNullable(result);
  }
}
