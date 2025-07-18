package com.deveagles.be15_deveagles_be.features.sales.query.mapper;

import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.ItemSalesDetailDTO;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.ItemSalesDetailResponse;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.PaymentsDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ItemSalesQueryMapper {

  ItemSalesDetailResponse findItemSalesDetail(@Param("itemSalesId") Long itemSalesId);

  List<PaymentsDTO> findPaymentsBySalesId(@Param("salesId") Long salesId);

  ItemSalesDetailResponse findItemSalesDetailBySalesId(Long salesId);

  List<ItemSalesDetailDTO> findItemSalesListBySalesId(Long salesId);
}
