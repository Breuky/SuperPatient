// auth.js - JWT Token 管理

const TOKEN_KEY = 'super_patient_token';
const USER_KEY = 'super_patient_user';

/**
 * 保存 Token 到本地存储
 */
function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token);
}

/**
 * 获取 Token
 */
function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || null;
}

/**
 * 删除 Token
 */
function removeToken() {
  wx.removeStorageSync(TOKEN_KEY);
  wx.removeStorageSync(USER_KEY);
}

/**
 * 保存用户信息
 */
function setUserInfo(userInfo) {
  wx.setStorageSync(USER_KEY, JSON.stringify(userInfo));
}

/**
 * 获取用户信息
 */
function getUserInfo() {
  const raw = wx.getStorageSync(USER_KEY);
  if (raw) {
    try {
      return JSON.parse(raw);
    } catch (e) {
      return null;
    }
  }
  return null;
}

module.exports = {
  setToken,
  getToken,
  removeToken,
  setUserInfo,
  getUserInfo
};
