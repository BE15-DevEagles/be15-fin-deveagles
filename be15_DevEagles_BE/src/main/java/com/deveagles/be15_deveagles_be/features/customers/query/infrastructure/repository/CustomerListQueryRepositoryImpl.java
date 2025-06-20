package com.deveagles.be15_deveagles_be.features.customers.query.infrastructure.repository;

import static com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.QCustomer.customer;
import static com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.QCustomerGrade.customerGrade;
import static com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.QTag.tag;
import static com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.QTagByCustomer.tagByCustomer;

import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerListResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.repository.CustomerListQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CustomerListQueryRepositoryImpl implements CustomerListQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<CustomerListResponse> findCustomerListByShopId(Long shopId) {
    List<CustomerListResponse> customerList = createBaseQuery(shopId).fetch();
    return attachTagsToCustomers(customerList);
  }

  @Override
  public Page<CustomerListResponse> findCustomerListByShopId(Long shopId, Pageable pageable) {
    JPAQuery<CustomerListResponse> baseQuery = createBaseQuery(shopId);

    Long total =
        queryFactory
            .select(customer.count())
            .from(customer)
            .innerJoin(customerGrade)
            .on(customer.customerGradeId.eq(customerGrade.id))
            .where(customer.shopId.eq(shopId).and(customer.deletedAt.isNull()))
            .fetchOne();

    if (total == null || total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    List<CustomerListResponse> customerList =
        baseQuery.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();

    List<CustomerListResponse> customersWithTags = attachTagsToCustomers(customerList);
    return new PageImpl<>(customersWithTags, pageable, total);
  }

  private JPAQuery<CustomerListResponse> createBaseQuery(Long shopId) {
    return queryFactory
        .select(
            Projections.constructor(
                CustomerListResponse.class,
                customer.id,
                customer.customerName,
                customer.phoneNumber,
                customer.memo,
                customer.visitCount,
                customer.totalRevenue,
                customer.recentVisitDate,
                customer.birthdate,
                customer.gender.stringValue(),
                customerGrade.customerGradeName,
                customerGrade.discountRate,
                customer.staffId,
                com.querydsl.core.types.dsl.Expressions.constant(
                    0) // remainingPrepaidAmount - 임시로 0
                ))
        .from(customer)
        .innerJoin(customerGrade)
        .on(customer.customerGradeId.eq(customerGrade.id))
        .where(customer.shopId.eq(shopId).and(customer.deletedAt.isNull()))
        .orderBy(customer.recentVisitDate.desc(), customer.customerName.asc());
  }

  private List<CustomerListResponse> attachTagsToCustomers(
      List<CustomerListResponse> customerList) {
    if (customerList.isEmpty()) {
      return customerList;
    }

    List<Long> customerIds =
        customerList.stream().map(CustomerListResponse::getCustomerId).toList();

    Map<Long, List<CustomerListResponse.TagInfo>> tagsByCustomer =
        getTagsByCustomerIds(customerIds);

    customerList.forEach(
        customer ->
            customer
                .getTags()
                .addAll(tagsByCustomer.getOrDefault(customer.getCustomerId(), List.of())));

    return customerList;
  }

  private Map<Long, List<CustomerListResponse.TagInfo>> getTagsByCustomerIds(
      List<Long> customerIds) {
    return queryFactory
        .select(tagByCustomer.customerId, tag.id, tag.tagName, tag.colorCode)
        .from(tagByCustomer)
        .innerJoin(tag)
        .on(tagByCustomer.tagId.eq(tag.id))
        .where(tagByCustomer.customerId.in(customerIds))
        .fetch()
        .stream()
        .collect(
            Collectors.groupingBy(
                tuple -> tuple.get(tagByCustomer.customerId),
                Collectors.mapping(
                    tuple ->
                        new CustomerListResponse.TagInfo(
                            tuple.get(tag.id), tuple.get(tag.tagName), tuple.get(tag.colorCode)),
                    Collectors.toList())));
  }
}
