/**
 * 格式化日期为后端期望的格式 (yyyy-MM-ddTHH:mm:ss)
 * @param {Date} date - 日期对象
 * @returns {string} 格式化后的日期字符串
 */
export function formatDateForBackend(date) {
  const pad = (num) => String(num).padStart(2, '0');
  
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hours = pad(date.getHours());
  const minutes = pad(date.getMinutes());
  const seconds = pad(date.getSeconds());
  
  // 使用ISO 8601格式，这是Java LocalDateTime默认支持的格式
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
}

/**
 * 格式化时间为 MM:SS 格式
 * @param {number} seconds - 秒数
 * @returns {string} 格式化后的时间字符串
 */
export function formatTime(seconds) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

/**
 * 获取两个日期之间的时间差（秒）
 * @param {Date|string} startDate - 开始日期
 * @param {Date|string} endDate - 结束日期
 * @returns {number} 时间差（秒）
 */
export function getTimeDifference(startDate, endDate) {
  const start = startDate instanceof Date ? startDate : new Date(startDate);
  const end = endDate instanceof Date ? endDate : new Date(endDate);
  
  return Math.floor((end - start) / 1000);
}

/**
 * 将秒数转换为人类可读的时间格式
 * @param {number} seconds - 秒数
 * @returns {string} 格式化后的时间字符串（例如：2小时30分钟15秒）
 */
export function formatDuration(seconds) {
  if (seconds < 60) {
    return `${seconds}秒`;
  }
  
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  
  if (minutes < 60) {
    return `${minutes}分钟${remainingSeconds > 0 ? ` ${remainingSeconds}秒` : ''}`;
  }
  
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  
  return `${hours}小时${remainingMinutes > 0 ? ` ${remainingMinutes}分钟` : ''}${remainingSeconds > 0 ? ` ${remainingSeconds}秒` : ''}`;
}

export function formatDateTimeDisplay(input) {
  if (!input) return '';
  const date = input instanceof Date ? input : new Date(input);
  if (Number.isNaN(date.getTime())) return String(input);
  const pad = (n) => String(n).padStart(2, '0');
  const y = date.getFullYear();
  const m = pad(date.getMonth() + 1);
  const d = pad(date.getDate());
  const hh = pad(date.getHours());
  const mm = pad(date.getMinutes());
  const ss = pad(date.getSeconds());
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
} 