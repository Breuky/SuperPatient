// api.js - HTTP 请求封装 (自动带 Token, 统一错误处理)
const auth = require('./auth');

// 后端 API 地址
const BASE_URL = 'http://localhost:8080';

/**
 * 发起 HTTP 请求
 * @param {string} method - GET/POST/PUT/DELETE
 * @param {string} path - 接口路径 (如 /api/patients)
 * @param {object} data - 请求体 (GET 请求会转为 query string)
 * @param {object} options - 额外选项 { showLoading, loadingText }
 * @returns {Promise<any>}
 */
function request(method, path, data = {}, options = {}) {
  const { showLoading = false, loadingText = '加载中...' } = options;

  if (showLoading) {
    wx.showLoading({ title: loadingText, mask: true });
  }

  const token = auth.getToken();

  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + path,
      method: method,
      data: data,
      header: {
        'Content-Type': 'application/json',
        'token': token || ''
      },
      success(res) {
        if (showLoading) wx.hideLoading();

        const { statusCode, data: body } = res;

        // HTTP 401 → Token 过期或未登录
        if (statusCode === 401) {
          auth.removeToken();
          const app = getApp();
          app.clearLogin();
          wx.showToast({ title: '登录已过期，请重新进入', icon: 'none' });
          reject(new Error('未授权'));
          return;
        }

        // HTTP 500
        if (statusCode >= 500) {
          wx.showToast({ title: '服务器错误', icon: 'none' });
          reject(new Error('服务器错误'));
          return;
        }

        // 业务层错误 (code=0)
        if (body && body.code === 0) {
          wx.showToast({ title: body.msg || '请求失败', icon: 'none' });
          reject(new Error(body.msg));
          return;
        }

        resolve(body);
      },
      fail(err) {
        if (showLoading) wx.hideLoading();
        wx.showToast({ title: '网络异常，请检查连接', icon: 'none' });
        reject(err);
      }
    });
  });
}

/**
 * GET 请求
 */
function get(path, params = {}, options = {}) {
  // 将 params 转为 query string
  const keys = Object.keys(params).filter(k => params[k] !== undefined && params[k] !== null && params[k] !== '');
  if (keys.length > 0) {
    const query = keys.map(k => `${k}=${encodeURIComponent(params[k])}`).join('&');
    path = path + (path.includes('?') ? '&' : '?') + query;
  }
  return request('GET', path, {}, options);
}

/**
 * POST 请求
 */
function post(path, data = {}, options = {}) {
  return request('POST', path, data, options);
}

/**
 * PUT 请求
 */
function put(path, data = {}, options = {}) {
  return request('PUT', path, data, options);
}

/**
 * DELETE 请求
 */
function del(path, options = {}) {
  return request('DELETE', path, {}, options);
}

/**
 * 文件上传
 * @param {string} filePath - 本地文件路径
 * @param {string} type - 文件类型 report/avatar
 * @returns {Promise<any>}
 */
function uploadFile(filePath, type = 'report') {
  const token = auth.getToken();

  return new Promise((resolve, reject) => {
    wx.showLoading({ title: '上传中...', mask: true });
    wx.uploadFile({
      url: BASE_URL + '/api/files/upload',
      filePath: filePath,
      name: 'file',
      formData: { type },
      header: { 'token': token || '' },
      success(res) {
        wx.hideLoading();
        try {
          const body = JSON.parse(res.data);
          if (body.code === 1) {
            resolve(body.data);
          } else {
            wx.showToast({ title: body.msg || '上传失败', icon: 'none' });
            reject(new Error(body.msg));
          }
        } catch (e) {
          reject(new Error('解析响应失败'));
        }
      },
      fail(err) {
        wx.hideLoading();
        wx.showToast({ title: '上传失败', icon: 'none' });
        reject(err);
      }
    });
  });
}

/**
 * 导出文件下载
 * @param {string} url - 完整的导出 URL
 */
function downloadFile(url) {
  const token = auth.getToken();
  return new Promise((resolve, reject) => {
    wx.showLoading({ title: '下载中...', mask: true });
    wx.downloadFile({
      url: BASE_URL + url,
      header: { 'token': token || '' },
      success(res) {
        wx.hideLoading();
        if (res.statusCode === 200) {
          resolve(res.tempFilePath);
        } else {
          reject(new Error('下载失败'));
        }
      },
      fail(err) {
        wx.hideLoading();
        reject(err);
      }
    });
  });
}

/**
 * 把后端返回的图片地址转成可直接加载的完整 URL。
 * 图片接口需要鉴权，而 <image> 无法自定义请求头，因此把 token 拼在 query 上。
 */
function fileUrl(url) {
  if (!url) return '';
  if (url.indexOf('http://') === 0 || url.indexOf('https://') === 0) return url;
  if (url.indexOf('data:') === 0) return url;
  if (url.indexOf('/api/files/') !== 0) return BASE_URL + url;
  return BASE_URL + url + '?token=' + encodeURIComponent(auth.getToken() || '');
}

module.exports = {
  get,
  post,
  put,
  del,
  uploadFile,
  downloadFile,
  fileUrl,
  BASE_URL
};
