package com.deveagles.be15_deveagles_be.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorCodeType {

  // 공통 에러
  VALIDATION_ERROR("00001", "유효성 검증에 실패했습니다", HttpStatus.BAD_REQUEST),
  INVALID_INPUT_VALUE("00002", "잘못된 입력값입니다", HttpStatus.BAD_REQUEST),
  METHOD_NOT_ALLOWED("00003", "허용되지 않는 HTTP 메소드입니다", HttpStatus.METHOD_NOT_ALLOWED),
  AUTHENTICATION_FAILED("00004", "인증에 실패했습니다", HttpStatus.UNAUTHORIZED),
  ACCESS_DENIED("00005", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),
  RESOURCE_NOT_FOUND("00006", "요청한 리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  DUPLICATE_RESOURCE("00007", "중복된 리소스입니다", HttpStatus.CONFLICT),
  FILE_SAVE_ERROR("00008", "파일 저장에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
  SEND_EMAIL_FAILURE_EXCEPTION("00009", "이메일 전송을 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  INTERNAL_SERVER_ERROR("00999", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),

  // 회원 관련 에러 (10000번대)
  USER_NAME_NOT_FOUND("11001", "존재하지 않는 ID입니다. 다시 입력해주세요.", HttpStatus.NOT_FOUND),
  USER_INVALID_PASSWORD("11002", "잘못된 비밀번호입니다.", HttpStatus.BAD_REQUEST),
  USER_NOT_FOUND("11003", "존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND),
  DUPLICATE_SEND_AUTH_EXCEPTION(
      "11004", "이미 인증 메일을 발송했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.BAD_REQUEST),
  INVALID_AUTH_CODE("11005", "유효한 인증이 아닙니다.", HttpStatus.BAD_REQUEST),
  STAFF_NOT_FOUND("11006", "존재하지 않는 직원입니다.", HttpStatus.NOT_FOUND),

  // 알림 관련 에러 (12000번대)
  NOTIFICATION_NOT_FOUND("12001", "존재하지 않는 알림이거나 접근 권한이 없습니다.", HttpStatus.NOT_FOUND),
  // 매장 관련 에러 (20000번대)
  SHOP_NOT_FOUNT("20001", "매장이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
  // 고객 관련 에러 (30000번대)
  CUSTOMER_NOT_FOUND("30001", "고객을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  CUSTOMER_PHONE_DUPLICATE("30002", "이미 등록된 전화번호입니다", HttpStatus.CONFLICT),
  CUSTOMER_ALREADY_DELETED("30003", "이미 삭제된 고객입니다", HttpStatus.BAD_REQUEST),
  CUSTOMER_ACCESS_DENIED("30004", "해당 고객에 대한 접근 권한이 없습니다", HttpStatus.FORBIDDEN),
  CUSTOMER_INVALID_PHONE_NUMBER("30005", "유효하지 않은 전화번호 형식입니다", HttpStatus.BAD_REQUEST),
  CUSTOMER_INVALID_BIRTHDATE("30006", "유효하지 않은 생년월일입니다", HttpStatus.BAD_REQUEST),

  // 메시지 관련 에러 (40000번대)
  MESSAGE_SETTINGS_ALREADY_EXISTS("40001", "이미 메시지 설정이 존재합니다.", HttpStatus.BAD_REQUEST),
  MESSAGE_SETTINGS_NOT_FOUND("40002", "해당 매장의 메시지 설정이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
  MESSAGE_INVALID_SENDER_NUMBER("40003", "유효하지 않은 발신 번호입니다.", HttpStatus.BAD_REQUEST),
  MESSAGE_INSUFFICIENT_POINTS("40004", "메시지를 발송할 포인트가 부족합니다.", HttpStatus.BAD_REQUEST),
  SCHEDULE_TIME_REQUIRED_FOR_RESERVATION(
      "40005", "예약 메시지일 경우에는 예약 시간이 필요합니다.", HttpStatus.BAD_REQUEST),
  SCHEDULE_TIME_NOT_ALLOWED_FOR_IMMEDIATE(
      "40006", "즉시 메시지일 경우에는 예약 시간이 없습니다.", HttpStatus.BAD_REQUEST),
  SMS_NOT_FOUND("40007", "해당 메시지가 없습니다.", HttpStatus.BAD_REQUEST),
  TEMPLATE_NOT_FOUND("40008", "해당 템플릿이 없습니다.", HttpStatus.BAD_REQUEST),
  TEMPLATE_ACCESS_DENIED("40009", "해당 템플릿을 수정할 수 있는 권한이 없습니다.", HttpStatus.UNAUTHORIZED),
  SMS_SHOP_MISMATCH("40010", "해당 매장에서 접근할 수 없는 메시지입니다.", HttpStatus.BAD_REQUEST),
  INVALID_MESSAGET_TYPE("40011", "해당 메시지는 예약 메시지가 아닙니다.", HttpStatus.BAD_REQUEST),
  ALREADY_SENT_OR_CANCELED("40012", "해당 메시지는 취소됐거나 이미 발송된 메시지입니다.", HttpStatus.BAD_REQUEST),
  INVALID_SCHEDULED_TIME("40013", "예약 발송 시간은 현재 시간 이후여야 합니다.", HttpStatus.BAD_REQUEST),
  INVALID_MESSAGE_CANCEL_CONDITION("40014", "예약 취소가 불가능한 상태입니다", HttpStatus.BAD_REQUEST),
  AUTOMATIC_TEMPLATE_ALREADY_EXISTS(
      "40015", "이미 동일한 이벤트 타입으로 메시지가 등록 되어 있습니다.", HttpStatus.BAD_REQUEST),
  INVALID_MESSAGE_RESEND_CONDITION("40015", "취소 상태인 메시지만 재전송할 수 있습니다.", HttpStatus.BAD_REQUEST),
  // 채팅 관련 에러 (41000번대)
  CHATROOM_NOT_FOUND("41001", "해당 채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  WEBSOCKET_INVALID_TOKEN("41002", "WebSocket 연결 실패: 유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
  WEBSOCKET_AUTHENTICATION_FAILED(
      "41003", "WebSocket 인증에 실패했습니다. 사용자 정보를 확인할 수 없습니다.", HttpStatus.UNAUTHORIZED),

  // 쿠폰 관련 에러 (50000번대)
  COUPON_NOT_FOUND("50001", "쿠폰을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  COUPON_CODE_DUPLICATE("50002", "이미 존재하는 쿠폰 코드입니다", HttpStatus.CONFLICT),
  COUPON_ALREADY_DELETED("50003", "이미 삭제된 쿠폰입니다", HttpStatus.BAD_REQUEST),
  COUPON_ALREADY_EXPIRED("50004", "만료된 쿠폰입니다", HttpStatus.BAD_REQUEST),
  COUPON_ALREADY_ACTIVE("50005", "이미 활성화된 쿠폰입니다", HttpStatus.BAD_REQUEST),
  COUPON_ALREADY_INACTIVE("50006", "이미 비활성화된 쿠폰입니다", HttpStatus.BAD_REQUEST),
  COUPON_DISCOUNT_CALCULATION_ERROR("50007", "쿠폰 할인 계산 중 오류가 발생했습니다", HttpStatus.BAD_REQUEST),
  COUPON_INVALID_AMOUNT("50008", "유효하지 않은 금액입니다", HttpStatus.BAD_REQUEST),
  COUPON_INVALID_DISCOUNT_RATE("50009", "유효하지 않은 할인율입니다", HttpStatus.BAD_REQUEST),
  DELETED_COUPON_OPERATION_NOT_ALLOWED(
      "50010", "삭제된 쿠폰은 해당 작업을 수행할 수 없습니다", HttpStatus.BAD_REQUEST),

  // 캠페인 관련 에러 (51000번대)
  CAMPAIGN_NOT_FOUND("51001", "캠페인을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  CAMPAIGN_ALREADY_DELETED("51002", "이미 삭제된 캠페인입니다", HttpStatus.BAD_REQUEST),
  INVALID_CAMPAIGN_DATE_RANGE("51003", "캠페인 종료일은 시작일보다 늦어야 합니다", HttpStatus.BAD_REQUEST),

  // 워크플로우 관련 에러 (60000번대)
  WORKFLOW_NOT_FOUND("60001", "워크플로우를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  WORKFLOW_TITLE_ALREADY_EXISTS("60002", "이미 존재하는 워크플로우 제목입니다", HttpStatus.CONFLICT),
  WORKFLOW_ACCESS_DENIED("60003", "해당 워크플로우에 대한 접근 권한이 없습니다", HttpStatus.FORBIDDEN),
  WORKFLOW_ALREADY_DELETED("60004", "이미 삭제된 워크플로우입니다", HttpStatus.BAD_REQUEST),
  WORKFLOW_EXECUTION_FAILED("60005", "워크플로우 실행에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
  WORKFLOW_TRIGGER_VALIDATION_FAILED("60006", "워크플로우 트리거 조건이 유효하지 않습니다", HttpStatus.BAD_REQUEST),
  WORKFLOW_ACTION_EXECUTION_FAILED(
      "60007", "워크플로우 액션 실행에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),

  // 예약 설정 관련 에러 (70000번대)
  INVALID_RESERVATION_TIME_RANGE("70001", "예약 시작 시간은 종료 시간보다 빨라야 합니다", HttpStatus.BAD_REQUEST),
  INVALID_LUNCH_TIME_RANGE("70002", "점심시간은 예약 가능 시간 범위 내여야 합니다", HttpStatus.BAD_REQUEST),
  INVALID_LUNCH_TIME_ORDER("70003", "점심시간 시작은 종료보다 빨라야 합니다", HttpStatus.BAD_REQUEST),
  RESERVATION_SETTING_ALREADY_EXISTS("70004", "이미 예약 설정이 존재합니다", HttpStatus.BAD_REQUEST),
  SHOP_NOT_FOUND("70005", "매장을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  RESERVATION_SETTING_NOT_FOUND("70006", "예약 설정이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
  INVALID_RESERVATION_TERM("70007", "예약 단위는 1분 이상이어야 합니다", HttpStatus.BAD_REQUEST),
  // 예약 관련 에러 (71000번대)
  RESERVATION_NOT_FOUND("71001", "예약을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  MODIFY_NOT_ALLOWED_FOR_PAID_RESERVATION(
      "71002", "PAID 상태의 예약은 수정할 수 없습니다", HttpStatus.BAD_REQUEST),
  // 일정 삭제 관련 에러 (72000번대)
  PLAN_NOT_FOUND("72001", "단기 일정이 존재하지 않습니다", HttpStatus.NOT_FOUND),
  REGULAR_PLAN_NOT_FOUND("72002", "정기 일정이 존재하지 않습니다", HttpStatus.NOT_FOUND),
  // 휴무 삭제 관련 에러 (73000번대)
  LEAVE_NOT_FOUND("73001", "단기 휴무가 존재하지 않습니다", HttpStatus.NOT_FOUND),
  REGULAR_LEAVE_NOT_FOUND("73002", "정기 휴무가 존재하지 않습니다", HttpStatus.NOT_FOUND),
  // 정기 일정,휴무 관련 에러 (74000번대)
  INVALID_SCHEDULE_TYPE("74001", "유효하지 않은 스케줄 타입입니다", HttpStatus.BAD_REQUEST),
  INVALID_SCHEDULE_REPEAT_TYPE("74002", "정기 스케줄은 요일 또는 월 중 하나만 설정해야 합니다", HttpStatus.BAD_REQUEST),
  INVALID_DAY_OF_WEEK("74003", "유효하지 않은 요일입니다", HttpStatus.BAD_REQUEST),

  // 상품, 회원권 관련 에러 (상품 :81000번대, 회원권: 82000번대)
  PRIMARY_ITEM_NAME_REQUIRED("81001", "1차 상품명을 입력해주세요.", HttpStatus.BAD_REQUEST),
  PRIMARY_ITEM_CATEGORY_REQUIRED("81002", "카테고리를 선택해주세요.", HttpStatus.BAD_REQUEST),
  INVALID_PRIMARY_ITEM_INPUT("81003", "잘못된 1차 상품 입력값입니다", HttpStatus.BAD_REQUEST),
  PRIMARY_ITEM_NOT_FOUND("81004", "1차 상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  SECONDARY_ITEM_NAME_REQUIRED("81005", "2차 상품명을 입력해주세요.", HttpStatus.BAD_REQUEST),
  SECONDARY_ITEM_PRICE_REQUIRED("81006", "2차 상품의 가격을 입력해주세요.", HttpStatus.BAD_REQUEST),
  INVALID_SECONDARY_ITEM_INPUT("81007", "잘못된 2차 상품 입력값입니다.", HttpStatus.BAD_REQUEST),
  SECONDARY_ITEM_SERVICE_TIME_REQUIRED("81008", "시술의 시술시간을 입력해주세요.", HttpStatus.BAD_REQUEST),
  SECONDARY_ITEM_NOT_FOUND("81009", "2차 상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  ITEMS_SHOP_NOT_FOUND("810010", "샵을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  ITEMS_SHOP_ID_REQUIRED("81011", "샵 ID는 필수입니다.", HttpStatus.BAD_REQUEST),

  MEMBERSHIP_NAME_REQUIRED("82001", "회원권명을 입력해주세요", HttpStatus.BAD_REQUEST),
  MEMBERSHIP_PRICE_REQUIRED("82002", "회원권가격을 입력해주세요.", HttpStatus.BAD_REQUEST),
  MEMBERSHIP_EXPIRATION_PERIOD_REQUIRED("82003", "회원권 유효기간을 입력해주세요.", HttpStatus.BAD_REQUEST),
  MEMBERSHIP_SESSION_REQUIRED("82004", "횟수를 입력해주세요", HttpStatus.BAD_REQUEST),
  PREPAIDPASS_NOT_FOUND("82005", "선불권을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  SESSIONPASS_NOT_FOUND("82006", "횟수권을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  MEMBERSHIP_EXPIRATION_PERIOD_TYPE_REQUIRED(
      "82007", "회원권 유효기간 단위를 입력해주세요.", HttpStatus.BAD_REQUEST),
  CUSTOMERPREPAIDPASS_NOT_FOUND("82008", "고객선불권을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  CUSTOMERSESSIONPASS_NOT_FOUND("82009", "고객횟수권을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  // 매출 관련 에러 (90000번대)

  SALES_RETAILPRICE_REQUIRED("90001", "매출정가를 입력해주세요.", HttpStatus.BAD_REQUEST),
  SALES_TOTALAMOUNT_REQUIRED("90002", "결제금액을 입력해주세요.", HttpStatus.BAD_REQUEST),
  SALES_SALESDATE_REQUIRED("90003", "매출일시를 입력해수제요.", HttpStatus.BAD_REQUEST),
  SALES_PAYMENTMETHOD_REQUIRED("90004", "결제수단을 입력해주세요.", HttpStatus.BAD_REQUEST),
  SALES_PAYMENTSAMOUNT_REQUIRED("90005", "결제금액을 입력해주세요.", HttpStatus.BAD_REQUEST),
  SALES_NOT_FOUND("90006", "매출을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  SALES_ALREADY_REFUNDED("90007", "이미 환불된 매출입니다.", HttpStatus.BAD_REQUEST),
  SALES_ALREADY_DELETED("90008", "이미 삭제된 매출입니다.", HttpStatus.BAD_REQUEST),
  ITEMSALES_NOT_FOUND("90010", "상품매출을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  INVALID_MEMBERSHIP_HISTORY("90011", "횟수권 사용횟수를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),

  // 분석 관련 에러 (95000번대)
  ANALYTICS_SERVER_CONNECTION_FAILED("95001", "분석 서버 연결에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
  ANALYTICS_CUSTOMER_RISK_ANALYSIS_FAILED(
      "95002", "고객 위험도 분석에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
  ANALYTICS_HIGH_RISK_CUSTOMER_QUERY_FAILED(
      "95003", "고위험 고객 목록 조회에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
  ANALYTICS_BATCH_TAGGING_FAILED("95004", "배치 위험 태깅에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
  ANALYTICS_RISK_SEGMENT_UPDATE_FAILED(
      "95005", "위험 세그먼트 업데이트에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
  ANALYTICS_INVALID_PARAMETER("95006", "분석 요청 파라미터가 유효하지 않습니다", HttpStatus.BAD_REQUEST),
  ANALYTICS_RESULT_PARSING_FAILED("95007", "분석 결과 파싱에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
  ;

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
