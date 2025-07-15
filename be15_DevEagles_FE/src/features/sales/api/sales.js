import api from '@/plugins/axios'; // 토큰 및 리프레시 포함된 axios 인스턴스 사용

// 매출 전체 조회
export const getSalesList = async (filters = {}) => {
  const response = await api.get('/sales', {
    params: filters,
  });
  return response.data.data; // ApiResponse.success(result) 기준
};

// 선불권 매출 등록
export const registerPrepaidPassSale = async data => {
  return await api.post('/prepaid-pass-sales', data);
};

// 횟수권 매출 등록
export const registerSessionPassSale = async data => {
  return await api.post('/session-pass-sales', data);
};

// 상품 매출 등록
export const registerItemSale = async data => {
  return await api.post('/item-sales', data);
};

// 회원권 매출 환불
export const refundSales = async salesId => {
  return await api.post(`/sales/refund/${salesId}`);
};

// 매출 삭제
export const deleteSales = async salesId => {
  return await api.delete(`/sales/delete/${salesId}`);
};

// 선불권 매출 상세 조회
export const getPrepaidPassSalesDetail = async prepaidPassSalesId => {
  return await api.get(`/membership-sales/prepaid/${prepaidPassSalesId}`);
};

// 횟수권 매출 상세 조회
export const getSessionPassSalesDetail = async sessionPassSalesId => {
  return await api.get(`/membership-sales/session/${sessionPassSalesId}`);
};
// 상품 매출 상세 조회
export const getItemSalesDetail = async itemSalesId => {
  return await api.get(`/item-sales/${itemSalesId}`);
};

// 상품 매출 수정
export const updateItemSales = async (salesId, payload) => {
  return await api.put(`/item-sales/${salesId}`, payload);
};

// 상품 매출 환불
export const refundItemSales = async salesId => {
  return await api.put(`/item-sales/refund/${salesId}`);
};
