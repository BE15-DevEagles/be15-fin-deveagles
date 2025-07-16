import api from '@/plugins/axios';
import { getApiLogger } from '@/plugins/LoggerManager.js';

const logger = getApiLogger('📩 AutoSendAPI');

const AutoSendAPI = {
  async getTemplates() {
    try {
      const res = await api.get('/message/auto-message');
      logger.info('자동발신 템플릿 조회 성공', res.data);
      return res.data;
    } catch (error) {
      logger.error('자동발신 템플릿 조회 실패', error);
      throw error;
    }
  },

  async createTemplate(payload) {
    try {
      const res = await api.post('/message/auto-message', payload);
      logger.info('GET /messages/automatic', res.data);
      logger.info('자동발신 템플릿 등록 성공', res.data);
      return res.data;
    } catch (error) {
      logger.error('자동발신 템플릿 등록 실패', error);
      throw error;
    }
  },

  async updateAutoSendTemplate(shopId, templateId, payload) {
    try {
      const res = await api.put(`/messages/automatic/${shopId}/${templateId}`, payload);
      logger.info('자동발신 템플릿 수정 성공', res.data);
      return res.data;
    } catch (error) {
      logger.error('자동발신 템플릿 수정 실패', error);
      throw error;
    }
  },

  async deleteAutoSendTemplate(shopId, templateId) {
    try {
      const res = await api.delete(`/messages/automatic/${shopId}/${templateId}`);
      logger.info('자동발신 템플릿 삭제 성공', res.data);
      return res.data;
    } catch (error) {
      logger.error('자동발신 템플릿 삭제 실패', error);
      throw error;
    }
  },
};

export default AutoSendAPI;
