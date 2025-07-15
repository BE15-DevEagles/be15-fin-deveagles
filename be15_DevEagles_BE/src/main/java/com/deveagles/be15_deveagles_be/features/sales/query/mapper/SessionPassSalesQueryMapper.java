package com.deveagles.be15_deveagles_be.features.sales.query.mapper;

import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.PaymentsDTO;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SessionPassSalesDetailResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SessionPassSalesQueryMapper {
  List<PaymentsDTO> findPaymentsBySalesId(@Param("salesId") Long salesId);

  SessionPassSalesDetailResponse findSessionPassSalesDetail(
      @Param("sessionPassSalesId") Long sessionPassSalesId);
}
