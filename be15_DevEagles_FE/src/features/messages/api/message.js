import api from '@/plugins/axios.js';
import { getApiLogger } from '@/plugins/LoggerManager.js';

const logger = getApiLogger('MessagesAPI');
const BASE_URL = '/message';

class MessagesAPI {
  /**
   * 문자 목록 조회 (page, size만 전달)
   */
  async fetchMessages({ page = 0, size = 20 }) {
    try {
      const params = { page, size };
      logger.request('GET', BASE_URL, params);
      const response = await api.get(BASE_URL, { params });
      logger.response('GET', BASE_URL, response.status, response.data);

      const { content, pagination } = response.data.data;
      if (!Array.isArray(content)) {
        throw new Error('문자 목록 조회 응답이 올바르지 않습니다.');
      }

      return {
        content,
        totalElements: pagination?.totalItems ?? 0,
        totalPages: pagination?.totalPages ?? 0,
      };
    } catch (error) {
      logger.error('GET', BASE_URL, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 문자 상세 조회
   */
  async fetchMessageDetail(messageId) {
    try {
      const url = `${BASE_URL}/${messageId}`;
      logger.request('GET', url);
      const response = await api.get(url);
      logger.response('GET', url, response.status, response.data);

      return response.data?.data || null;
    } catch (error) {
      logger.error('GET', `${BASE_URL}/${messageId}`, error);
      throw this.handleApiError(error);
    }
  }

  /**
   * 문자 발신 설정 조회
   * - 404인 경우는 에러가 아님 → null 반환
   */
  async getMessageSettings() {
    const url = `${BASE_URL}/settings`;
    try {
      logger.request('GET', url);
      const res = await api.get(url);
      logger.response('GET', url, res.status, res.data);

      const data = res.data.data;
      if (!data || typeof data !== 'object') {
        return null;
      }

      return {
        senderNumber: data.senderNumber,
        canAlimtalk: data.canAlimtalk,
        point: data.point,
      };
    } catch (err) {
      if (err.response?.status === 404) {
        logger.info('GET', url, '설정 없음 (404) → null 반환');
        return null;
      }

      logger.error('GET', url, err);
      throw this.handleApiError(err);
    }
  }

  /**
   * 문자 발신 설정 수정
   */
  async updateMessageSettings(payload) {
    try {
      const url = `${BASE_URL}/settings`;
      logger.request('PUT', url, payload);
      const res = await api.put(url, payload);
      logger.response('PUT', url, res.status, res.data);
      return res.data.data;
    } catch (err) {
      logger.error('PUT', `${BASE_URL}/settings`, err);
      throw this.handleApiError(err);
    }
  }

  /**
   * 공통 에러 핸들링
   */
  handleApiError(error) {
    if (error.response) {
      const { status, data } = error.response;
      switch (status) {
        case 400:
          return new Error(data.message || '잘못된 요청입니다.');
        case 404:
          return new Error('문자 메시지를 찾을 수 없습니다.');
        case 500:
          return new Error('서버 오류가 발생했습니다.');
        default:
          return new Error(data.message || '알 수 없는 오류가 발생했습니다.');
      }
    } else if (error.request) {
      return new Error('네트워크 오류가 발생했습니다.');
    } else {
      return new Error(error.message || '오류가 발생했습니다.');
    }
  }

  async sendMessage(payload) {
    const url = `${BASE_URL}/send`;
    try {
      logger.request('POST', url, payload);
      const response = await api.post(url, payload);
      logger.response('POST', url, response.status, response.data);
      return response.data.data;
    } catch (error) {
      logger.error('POST', url, error);
      throw this.handleApiError(error);
    }
  }
}

const messagesAPI = new MessagesAPI();
export default messagesAPI;
