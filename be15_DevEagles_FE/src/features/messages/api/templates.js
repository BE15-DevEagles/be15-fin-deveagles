// ✅ templates.js - 전체 API 함수 정의 (GET/POST/PUT/DELETE + 로깅 포함)
import api from '@/plugins/axios';
import { getApiLogger } from '@/plugins/LoggerManager.js';

const logger = getApiLogger('📩 TemplatesAPI');
const BASE_URL = '/message/templates';

const TemplatesAPI = {
  async getTemplates(searchParams = {}) {
    try {
      const res = await api.get(BASE_URL, { params: searchParams });
      console.log(res.data);
      return res.data;
    } catch (error) {
      logger.error('템플릿 목록 조회 실패', error);
      throw error;
    }
  },

  async getTemplateById(templateId) {
    try {
      const res = await api.get(`${BASE_URL}/${templateId}`);
      logger.info('단일 템플릿 조회 성공', res.data);
      return res.data.data;
    } catch (error) {
      logger.error('단일 템플릿 조회 실패', error);
      throw error;
    }
  },

  async createTemplate(payload) {
    try {
      const res = await api.post(BASE_URL, payload);
      logger.info('템플릿 생성 성공', res.data);
      return res.data.data;
    } catch (error) {
      logger.error('템플릿 생성 실패', error);
      throw error;
    }
  },

  async updateTemplate(templateId, payload) {
    try {
      const res = await api.put(`${BASE_URL}/${templateId}`, payload);
      logger.info('템플릿 수정 성공', res.data);
      return res.data.data;
    } catch (error) {
      logger.error('템플릿 수정 실패', error);
      throw error;
    }
  },

  async deleteTemplate(templateId) {
    try {
      const res = await api.delete(`${BASE_URL}/${templateId}`);
      logger.info('템플릿 삭제 성공', res.data);
      return res.data.data;
    } catch (error) {
      logger.error('템플릿 삭제 실패', error);
      throw error;
    }
  },
};

export default TemplatesAPI;
