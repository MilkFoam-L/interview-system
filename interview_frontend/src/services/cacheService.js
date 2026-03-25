/**
 * 简单的内存缓存服务
 * 支持设置TTL过期时间，自动清理过期项
 */
class CacheService {
  constructor(options = {}) {
    this.cache = new Map();
    this.ttl = options.ttl || 5 * 60 * 1000; // 默认5分钟
    this.maxItems = options.maxItems || 100; // 默认最大缓存项数
    
    // 定期清理过期项
    this.interval = setInterval(() => {
      this.clearExpired();
    }, 60 * 1000); // 每分钟清理一次
  }
  
  /**
   * 设置缓存项
   * @param {string} key - 缓存键
   * @param {any} value - 缓存值
   * @param {Object} options - 选项
   * @param {number} options.ttl - 过期时间(毫秒)
   */
  set(key, value, options = {}) {
    const ttl = options.ttl || this.ttl;
    const expiry = Date.now() + ttl;
    
    this.cache.set(key, {
      value,
      expiry
    });
    
    // 如果缓存项过多，删除最旧的
    if (this.cache.size > this.maxItems) {
      const oldestKey = this.cache.keys().next().value;
      this.cache.delete(oldestKey);
    }
    
    return value;
  }
  
  /**
   * 获取缓存项
   * @param {string} key - 缓存键
   * @returns {any} 缓存值，如果不存在或已过期则返回null
   */
  get(key) {
    const item = this.cache.get(key);
    
    if (!item) return null;
    
    // 检查是否过期
    if (Date.now() > item.expiry) {
      this.cache.delete(key);
      return null;
    }
    
    return item.value;
  }
  
  /**
   * 删除缓存项
   * @param {string} key - 缓存键
   */
  delete(key) {
    return this.cache.delete(key);
  }
  
  /**
   * 清除所有缓存
   */
  clear() {
    this.cache.clear();
  }
  
  /**
   * 清除过期项
   */
  clearExpired() {
    const now = Date.now();
    
    for (const [key, item] of this.cache.entries()) {
      if (now > item.expiry) {
        this.cache.delete(key);
      }
    }
  }
  
  /**
   * 获取或设置缓存
   * 如果缓存不存在或已过期，则调用fetcher函数获取值并缓存
   * @param {string} key - 缓存键
   * @param {Function} fetcher - 获取数据的函数，应返回Promise
   * @param {Object} options - 选项
   * @returns {Promise<any>} 缓存值或fetcher函数的返回值
   */
  async getOrSet(key, fetcher, options = {}) {
    const cachedValue = this.get(key);
    if (cachedValue !== null) {
      return cachedValue;
    }
    
    const value = await fetcher();
    this.set(key, value, options);
    return value;
  }
  
  /**
   * 获取缓存状态
   * @returns {Object} 缓存状态信息
   */
  getStatus() {
    const now = Date.now();
    let expired = 0;
    
    for (const item of this.cache.values()) {
      if (now > item.expiry) {
        expired++;
      }
    }
    
    return {
      size: this.cache.size,
      expired,
      active: this.cache.size - expired
    };
  }
  
  /**
   * 销毁缓存服务
   */
  destroy() {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
    this.clear();
  }
}

// 创建全局缓存实例
const globalCache = new CacheService();

export default globalCache; 