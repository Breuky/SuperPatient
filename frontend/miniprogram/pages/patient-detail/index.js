// pages/patient-detail/index.js - 患者详情页
const api = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    patientId: null,
    patient: null,
    // 检查记录
    records: [],
    categories: [],
    activeCategoryId: null,
    startDate: '',
    endDate: '',
    loading: false,
    // 编辑弹窗
    showEditModal: false,
    editForm: {},
    submitting: false
  },

  onLoad(options) {
    this.setData({ patientId: options.id });
    this.loadCategories();
    this.loadPatientDetail();
    this.loadRecords();
  },

  onShow() {
    // 从新增记录页返回时刷新
    if (this.data.patientId) {
      this.loadPatientDetail();
      this.loadRecords();
    }
  },

  /**
   * 加载患者详情
   */
  loadPatientDetail() {
    api.get(`/api/patients/${this.data.patientId}`)
      .then(res => {
        this.setData({ patient: res.data });
      })
      .catch(() => {});
  },

  /**
   * 加载检查分类
   */
  loadCategories() {
    api.get('/api/categories')
      .then(res => {
        const categories = res.data || [];
        this.setData({
          categories,
          activeCategoryId: categories.length > 0 ? categories[0].id : null
        });
      })
      .catch(() => {});
  },

  /**
   * 加载检查记录
   */
  loadRecords() {
    this.setData({ loading: true });
    const params = { pageSize: 100 };
    if (this.data.activeCategoryId) {
      params.categoryId = this.data.activeCategoryId;
    }
    if (this.data.startDate) params.startDate = this.data.startDate;
    if (this.data.endDate) params.endDate = this.data.endDate;

    api.get(`/api/patients/${this.data.patientId}/records`, params)
      .then(res => {
        this.setData({
          records: res.data.rows || [],
          loading: false
        });
      })
      .catch(() => {
        this.setData({ loading: false });
      });
  },

  /**
   * 切换分类筛选
   */
  onCategoryChange(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ activeCategoryId: id === this.data.activeCategoryId ? null : id }, () => {
      this.loadRecords();
    });
  },

  /**
   * 日期筛选
   */
  onStartDateChange(e) {
    this.setData({ startDate: e.detail.value }, () => this.loadRecords());
  },

  onEndDateChange(e) {
    this.setData({ endDate: e.detail.value }, () => this.loadRecords());
  },

  /**
   * 点击检查记录 → 进入详情
   */
  onRecordTap(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/record-detail/index?id=${id}` });
  },

  /**
   * 新增检查记录
   */
  onAddRecord() {
    wx.navigateTo({ url: `/pages/record-add/index?patientId=${this.data.patientId}` });
  },

  /**
   * 查看趋势
   */
  onViewTrend() {
    wx.navigateTo({ url: `/pages/trend/index?patientId=${this.data.patientId}` });
  },

  /**
   * 导出数据
   */
  onExport() {
    api.downloadFile(`/api/patients/${this.data.patientId}/export`)
      .then(tempFilePath => {
        wx.openDocument({
          filePath: tempFilePath,
          showMenu: true,
          success: () => {},
          fail: () => {
            wx.showToast({ title: '打开文件失败', icon: 'none' });
          }
        });
      })
      .catch(() => {});
  },

  /**
   * 编辑患者
   */
  openEditModal() {
    const p = this.data.patient;
    this.setData({
      showEditModal: true,
      editForm: {
        name: p.name || '',
        gender: p.gender,
        birthDate: p.birthDate || '',
        diagnosisDate: p.diagnosisDate || '',
        phone: p.phone || '',
        notes: p.notes || ''
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

  onEditGenderChange(e) {
    this.setData({ 'editForm.gender': parseInt(e.currentTarget.dataset.gender) });
  },

  onEditDateChange(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`editForm.${field}`]: e.detail.value });
  },

  /**
   * 提交编辑
   */
  submitEdit() {
    const { editForm } = this.data;
    if (!editForm.name.trim()) {
      wx.showToast({ title: '姓名不能为空', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    api.put(`/api/patients/${this.data.patientId}`, {
      name: editForm.name.trim(),
      gender: editForm.gender,
      birthDate: editForm.birthDate || undefined,
      diagnosisDate: editForm.diagnosisDate || undefined,
      phone: editForm.phone || undefined,
      notes: editForm.notes || undefined
    }).then(() => {
      this.setData({ showEditModal: false, submitting: false });
      wx.showToast({ title: '修改成功', icon: 'success' });
      this.loadPatientDetail();
    }).catch(() => {
      this.setData({ submitting: false });
    });
  },

  /**
   * 删除患者
   */
  onDeletePatient() {
    util.showConfirm('确认删除', '删除患者将同时删除所有检查记录，此操作不可恢复。').then(confirmed => {
      if (confirmed) {
        api.del(`/api/patients/${this.data.patientId}`, { showLoading: true })
          .then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => wx.navigateBack(), 1500);
          })
          .catch(() => {});
      }
    });
  }
});
