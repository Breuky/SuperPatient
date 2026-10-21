// pages/profile/index.js - 个人中心
const api = require('../../utils/api');
const auth = require('../../utils/auth');
const app = getApp();

Page({
  data: {
    userInfo: null,
    statistics: null,
    // 编辑资料弹窗
    showEditModal: false,
    editForm: {},
    submitting: false
  },

  onShow() {
    this.loadProfile();
    this.loadStatistics();
  },

  /**
   * 加载用户信息
   */
  loadProfile() {
    api.get('/api/auth/profile')
      .then(res => {
        const info = res.data;
        auth.setUserInfo(info);
        app.globalData.userInfo = info;
        this.setData({ userInfo: info });
      })
      .catch(() => {
        // 本地缓存兜底
        const local = auth.getUserInfo();
        if (local) this.setData({ userInfo: local });
      });
  },

  /**
   * 加载统计
   */
  loadStatistics() {
    api.get('/api/auth/statistics')
      .then(res => {
        this.setData({ statistics: res.data });
      })
      .catch(() => {});
  },

  /**
   * 编辑资料
   */
  openEditModal() {
    const u = this.data.userInfo || {};
    this.setData({
      showEditModal: true,
      editForm: {
        nickname: u.nickname || '',
        phone: u.phone || ''
      }
    });
  },

  closeEditModal() {
    this.setData({ showEditModal: false });
  },

  onEditFieldChange(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`editForm.${field}`]: e.detail.value });
  },

  /**
   * 选择头像
   */
  onChooseAvatar() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const filePath = res.tempFilePaths[0];
        api.uploadFile(filePath, 'avatar')
          .then(data => {
            // 更新后端头像
            api.put('/api/auth/profile', { avatarUrl: data.url })
              .then(() => {
                this.loadProfile();
                wx.showToast({ title: '头像已更新', icon: 'success' });
              })
              .catch(() => {});
          })
          .catch(() => {});
      }
    });
  },

  /**
   * 提交编辑
   */
  submitEdit() {
    this.setData({ submitting: true });
    const data = {};
    if (this.data.editForm.nickname) data.nickname = this.data.editForm.nickname.trim();
    if (this.data.editForm.phone) data.phone = this.data.editForm.phone.trim();

    api.put('/api/auth/profile', data)
      .then(() => {
        this.setData({ showEditModal: false, submitting: false });
        wx.showToast({ title: '修改成功', icon: 'success' });
        this.loadProfile();
      })
      .catch(() => {
        this.setData({ submitting: false });
      });
  },

  /**
   * 登出
   */
  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '退出后需要重新登录',
      success: (res) => {
        if (res.confirm) {
          api.post('/api/auth/logout')
            .catch(() => {})
            .finally(() => {
              app.clearLogin();
              this.setData({ userInfo: null, statistics: null });
              wx.showToast({ title: '已退出', icon: 'none' });
            });
        }
      }
    });
  }
});
