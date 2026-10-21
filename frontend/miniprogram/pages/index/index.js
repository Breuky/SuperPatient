// pages/index/index.js - 首页：患者列表
const api = require('../../utils/api');
const util = require('../../utils/util');
const app = getApp();

Page({
  data: {
    patients: [],
    keyword: '',
    page: 1,
    pageSize: 10,
    total: 0,
    loading: false,
    noMore: false,
    // 新增患者弹窗
    showAddModal: false,
    formData: {
      name: '',
      gender: 1,
      birthDate: '',
      diagnosisDate: '',
      phone: '',
      notes: ''
    },
    submitting: false
  },

  onLoad() {
    this.ensureLoginAndLoad();
  },

  onShow() {
    // 从其他页面返回时刷新
    if (this.data.patients.length > 0) {
      this.loadPatients(true);
    }
  },

  onPullDownRefresh() {
    this.loadPatients(true).then(() => {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    if (!this.data.noMore && !this.data.loading) {
      this.loadPatients(false);
    }
  },

  /**
   * 确保登录后加载数据
   */
  ensureLoginAndLoad() {
    app.ensureLogin()
      .then(() => this.loadPatients(true))
      .catch(() => {
        // 开发阶段：如果后端未就绪，仍可浏览 UI
        wx.showToast({ title: '后端服务未连接', icon: 'none' });
      });
  },

  /**
   * 加载患者列表
   */
  loadPatients(refresh = false) {
    if (this.data.loading) return Promise.resolve();
    const page = refresh ? 1 : this.data.page;

    this.setData({ loading: true });

    return api.get('/api/patients', {
      keyword: this.data.keyword || undefined,
      page,
      pageSize: this.data.pageSize
    }).then(res => {
      const rows = res.data.rows || [];
      const patients = refresh ? rows : [...this.data.patients, ...rows];
      this.setData({
        patients,
        page: page + 1,
        total: res.data.total,
        noMore: patients.length >= res.data.total,
        loading: false
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  /**
   * 搜索输入（防抖）
   */
  onSearchInput: util.debounce(function(e) {
    this.setData({ keyword: e.detail.value }, () => {
      this.loadPatients(true);
    });
  }, 500),

  /**
   * 搜索确认
   */
  onSearchConfirm(e) {
    this.setData({ keyword: e.detail.value }, () => {
      this.loadPatients(true);
    });
  },

  /**
   * 点击患者卡片 → 进入详情
   */
  onPatientTap(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/patient-detail/index?id=${id}` });
  },

  /**
   * 打开新增患者弹窗
   */
  openAddModal() {
    this.setData({
      showAddModal: true,
      formData: {
        name: '',
        gender: 1,
        birthDate: '',
        diagnosisDate: '',
        phone: '',
        notes: ''
      }
    });
  },

  /**
   * 关闭弹窗
   */
  closeAddModal() {
    this.setData({ showAddModal: false });
  },

  /**
   * 表单字段变更
   */
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`formData.${field}`]: e.detail.value
    });
  },

  /**
   * 性别切换
   */
  onGenderChange(e) {
    this.setData({
      'formData.gender': parseInt(e.currentTarget.dataset.gender)
    });
  },

  /**
   * 日期选择
   */
  onDateChange(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`formData.${field}`]: e.detail.value
    });
  },

  /**
   * 提交新增患者
   */
  submitAddPatient() {
    const { formData } = this.data;
    if (!formData.name.trim()) {
      wx.showToast({ title: '请输入患者姓名', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    api.post('/api/patients', {
      name: formData.name.trim(),
      gender: formData.gender,
      birthDate: formData.birthDate || undefined,
      diagnosisDate: formData.diagnosisDate || undefined,
      phone: formData.phone || undefined,
      notes: formData.notes || undefined
    }, { showLoading: true }).then(() => {
      this.setData({ showAddModal: false, submitting: false });
      wx.showToast({ title: '添加成功', icon: 'success' });
      this.loadPatients(true);
    }).catch(() => {
      this.setData({ submitting: false });
    });
  }
});
