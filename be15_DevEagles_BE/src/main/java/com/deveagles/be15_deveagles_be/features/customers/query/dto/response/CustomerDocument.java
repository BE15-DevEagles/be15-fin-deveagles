package com.deveagles.be15_deveagles_be.features.customers.query.dto.response;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "customers")
@Getter
@Builder
public class CustomerDocument {

  @Id private String id;

  @Field(type = FieldType.Long)
  private Long customerId;

  @Field(type = FieldType.Long)
  private Long shopId;

  @Field(type = FieldType.Text, analyzer = "standard")
  private String customerName;

  @Field(type = FieldType.Keyword)
  private String phoneNumber;

  @Field(type = FieldType.Long)
  private Long customerGradeId;

  @Field(type = FieldType.Keyword)
  private String gender;

  @Field(type = FieldType.Date)
  private LocalDateTime deletedAt;

  public static CustomerDocument from(Customer customer) {
    return CustomerDocument.builder()
        .id(customer.getShopId() + "_" + customer.getId()) // shopId_customerId 형태로 unique ID 생성
        .customerId(customer.getId())
        .shopId(customer.getShopId())
        .customerName(customer.getCustomerName())
        .phoneNumber(customer.getPhoneNumber())
        .customerGradeId(customer.getCustomerGradeId())
        .gender(customer.getGender() != null ? customer.getGender().name() : null)
        .deletedAt(customer.getDeletedAt())
        .build();
  }
}
