package interview.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AIScreeningService {
	/**
	 * 分页查询AI初选候选人列表
	 */
	Map<String, Object> queryCandidates(String keyword, String position, Integer status, int page, int size);

	/** 单个通过并记录通知人/时间 */
	void notifyPass(Long applicationId, Long operatorUserId, LocalDateTime notifyTime);

	/** 单个拒绝并记录理由 */
	void reject(Long applicationId, String reason, Long operatorUserId);

	/** 批量通过 */
	void batchNotifyPass(List<Long> applicationIds, Long operatorUserId, LocalDateTime notifyTime);

	/** 批量拒绝 */
	void batchReject(List<Long> applicationIds, String reason, Long operatorUserId);

	/**
	 * 根据申请ID获取最新综合报告
	 */
	Map<String, Object> getLatestReportByApplicationId(Long applicationId);
} 