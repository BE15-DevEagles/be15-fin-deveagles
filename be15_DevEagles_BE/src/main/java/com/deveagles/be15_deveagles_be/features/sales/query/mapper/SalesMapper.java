package com.deveagles.be15_deveagles_be.features.sales.query.mapper;

import com.deveagles.be15_deveagles_be.features.sales.query.dto.request.SalesListFilterRequest;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.ItemSalesListDTO;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.PaymentsDTO;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SalesListResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SalesMapper {

  List<SalesListResponse> findSalesList(
      @Param("shopId") Long shopId, @Param("filter") SalesListFilterRequest filter);

  long countSalesList(@Param("shopId") Long shopId, @Param("filter") SalesListFilterRequest filter);

  List<SalesListResponse> findSalesListWithoutPayments(
      @Param("shopId") Long shopId, @Param("filter") SalesListFilterRequest filter);

  List<PaymentsDTO> findPaymentsBySalesIds(@Param("salesIds") List<Long> salesIds);

  List<ItemSalesListDTO> findItemSalesBySalesIds(@Param("salesIds") List<Long> salesIds);
}
