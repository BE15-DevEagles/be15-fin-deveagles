package com.deveagles.be15_deveagles_be.features.customers.query.infrastructure.repository;

import static com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.QAcquisitionChannel.acquisitionChannel;
import static com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.QCustomer.customer;
import static com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.QCustomerGrade.customerGrade;
import static com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.QTag.tag;
import static com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.QTagByCustomer.tagByCustomer;

import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.repository.CustomerDetailQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CustomerDetailQueryRepositoryImpl implements CustomerDetailQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public Optional<CustomerDetailResponse> findCustomerDetailById(Long customerId, Long shopId) {
    CustomerDetailResponse customerDetail =
        queryFactory
            .select(
                Projections.constructor(
                    CustomerDetailResponse.class,
                    customer.id,
                    customer.customerName,
                    customer.phoneNumber,
                    customer.memo,
                    customer.visitCount,
                    customer.totalRevenue,
                    customer.recentVisitDate,
                    customer.birthdate,
                    customer.registeredAt,
                    customer.noshowCount,
                    customer.gender,
                    customer.marketingConsent,
                    customer.marketingConsentedAt,
                    customer.notificationConsent,
                    customer.lastMessageSentAt,
                    customer.createdAt,
                    customer.modifiedAt,
                    customer.shopId,
                    customer.staffId,
                    customerGrade.customerGradeName,
                    customerGrade.discountRate,
                    acquisitionChannel.channelName))
            .from(customer)
            .innerJoin(customerGrade)
            .on(customer.customerGradeId.eq(customerGrade.id))
            .innerJoin(acquisitionChannel)
            .on(customer.channelId.eq(acquisitionChannel.id))
            .where(
                customer
                    .id
                    .eq(customerId)
                    .and(customer.shopId.eq(shopId))
                    .and(customer.deletedAt.isNull()))
            .fetchOne();

    if (customerDetail == null) {
      return Optional.empty();
    }

    List<CustomerDetailResponse.TagInfo> tags =
        queryFactory
            .select(
                Projections.constructor(
                    CustomerDetailResponse.TagInfo.class, tag.id, tag.tagName, tag.colorCode))
            .from(tagByCustomer)
            .innerJoin(tag)
            .on(tagByCustomer.tagId.eq(tag.id))
            .where(tagByCustomer.customerId.eq(customerId))
            .fetch();

    return Optional.of(
        CustomerDetailResponse.builder()
            .customerId(customerDetail.getCustomerId())
            .customerName(customerDetail.getCustomerName())
            .phoneNumber(customerDetail.getPhoneNumber())
            .memo(customerDetail.getMemo())
            .visitCount(customerDetail.getVisitCount())
            .totalRevenue(customerDetail.getTotalRevenue())
            .recentVisitDate(customerDetail.getRecentVisitDate())
            .birthdate(customerDetail.getBirthdate())
            .registeredAt(customerDetail.getRegisteredAt())
            .noshowCount(customerDetail.getNoshowCount())
            .gender(customerDetail.getGender())
            .marketingConsent(customerDetail.getMarketingConsent())
            .marketingConsentedAt(customerDetail.getMarketingConsentedAt())
            .notificationConsent(customerDetail.getNotificationConsent())
            .lastMessageSentAt(customerDetail.getLastMessageSentAt())
            .createdAt(customerDetail.getCreatedAt())
            .modifiedAt(customerDetail.getModifiedAt())
            .shopId(customerDetail.getShopId())
            .staffId(customerDetail.getStaffId())
            .customerGradeName(customerDetail.getCustomerGradeName())
            .discountRate(customerDetail.getDiscountRate())
            .channelName(customerDetail.getChannelName())
            .tags(tags)
            .build());
  }
}
