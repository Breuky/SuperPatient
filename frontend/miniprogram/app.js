// app.js - SuperPatient 小程序入口
const auth = require('./utils/auth');
const api = require('./utils/api');

App({
  globalData: {
    // 后端 API 地址 (开发环境)
    // 微信开发者工具 → 详情 → 本地设置 → 勾选"不校验合法域名"
    baseUrl: 'http://localhost:8080',
    token: null,
    userInfo: null,
    isLoggedIn: false
  },

  onLaunch() {
    // 检查本地 Token 是否有效
    const token = auth.getToken();
    if (token) {
      this.globalData.token = token;
      this.globalData.isLoggedIn = true;
      // 尝试获取用户信息验证 Token 有效性
      this.checkLoginStatus();
    }
  },

  /**
   * 验证登录状态
   */
  checkLoginStatus() {
    api.get('/api/auth/profile')
      .then(res => {
        if (res.code === 1) {
          this.globalData.userInfo = res.data;
          this.globalData.isLoggedIn = true;
        } else {
          this.clearLogin();
        }
      })
      .catch(() => {
        this.clearLogin();
      });
  },

  /**
   * 执行微信登录
   */
  doLogin() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (loginRes) => {
          if (loginRes.code) {
            api.post('/api/auth/login', {
              code: loginRes.code
            }).then(res => {
              if (res.code === 1 && res.data.token) {
                auth.setToken(res.data.token);
                this.globalData.token = res.data.token;
                this.globalData.userInfo = res.data.userInfo;
                this.globalData.isLoggedIn = true;
                resolve(res.data);
              } else {
                reject(new Error(res.msg || '登录失败'));
              }
            }).catch(reject);
          } else {
            reject(new Error('wx.login 失败'));
          }
        },
        fail: reject
      });
    });
  },

  /**
   * 清除登录状态
   */
  clearLogin() {
    auth.removeToken();
    this.globalData.token = null;
    this.globalData.userInfo = null;
    this.globalData.isLoggedIn = false;
  },

  /**
   * 确保已登录，未登录则自动登录
   */
  ensureLogin() {
    if (this.globalData.isLoggedIn && this.globalData.token) {
      return Promise.resolve();
    }
    return this.doLogin();
  }
});
