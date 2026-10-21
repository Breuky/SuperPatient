// pages/record-detail/index.js - 检查记录详情
const api = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    recordId: null,
    record: null,
    loading: true,
    // 编辑模式
    editMode: false,
    editForm: {},
    submitting: false,
    // 编辑指标值
    editingValues: {}
  },

  onLoad(options) {
    this.setData({ recordId: options.id });
    this.loadDetail();
  },

  /**
   * 加载记录详情
   */
  loadDetail() {
    this.setData({ loading: true });
    api.get(`/api/records/${this.data.recordId}`)
      .then(res => {
        const record = res.data;
        record.imageUrl = api.fileUrl(res.data.imageUrl);
        this.setData({ record, loading: false });
      })
      .catch(() => {
        this.setData({ loading: false });
      });
  },

  /**
   * 预览报告照片
   */
  onPreviewImage() {
    wx.previewImage({
      urls: [this.data.record.imageUrl],
      current: this.data.record.imageUrl
    });
  },

  /**
   * 进入编辑模式
   */
  onEdit() {
    const record = this.data.record;
    const editingValues = {};
    (record.values || []).forEach(v => {
      editingValues[v.indicatorId] = {
        valueDecimal: v.valueDecimal !== null ? String(v.valueDecimal) : '',
        valueText: v.valueText || '',
        valueType: v.valueType
      };
    });
    this.setData({
      editMode: true,
      editForm: {
        examDate: record.examDate || '',
        hospital: record.hospital || '',
        doctor: record.doctor || '',
        notes: record.notes || ''
      },
      editingValues
    });
  },

  /**
   * 取消编辑
   */
  onCancelEdit() {
    this.setData({ editMode: false });
  },

  /**
   * 编辑表单字段变更
   */
  onEditFieldChange(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`editForm.${field}`]: e.detail.value });
  },

  /**
   * 编辑指标值变更
   */
  onEditValueChange(e) {
    const id = e.currentTarget.dataset.id;
    const valType = e.currentTarget.dataset.valType;
    const editingValues = { ...this.data.editingValues };
    if (!editingValues[id]) editingValues[id] = {};
    if (valType === 'number') {
      editingValues[id].valueDecimal = e.detail.value;
    } else {
      editingValues[id].valueText = e.detail.value;
    }
    this.setData({ editingValues });
  },

  /**
   * 提交编辑
   */
  onSubmitEdit() {
    this.setData({ submitting: true });
    const { editForm, editingValues, record } = this.data;

    const values = [];
    for (const indicatorId of Object.keys(editingValues)) {
      const v = editingValues[indicatorId];
      const item = { indicatorId: parseInt(indicatorId) };
      if (v.valueType === 'number') {
        if (v.valueDecimal === '' || v.valueDecimal === undefined) continue;
        item.valueDecimal = parseFloat(v.valueDecimal);
      } else {
        if (!v.valueText || !v.valueText.trim()) continue;
        item.valueText = v.valueText.trim();
      }
      values.push(item);
    }

    api.put(`/api/records/${this.data.recordId}`, {
      examDate: util.toISODateTime(editForm.examDate) || undefined,
      hospital: editForm.hospital || undefined,
      doctor: editForm.doctor || undefined,
      notes: editForm.notes || undefined,
      values
    }).then(() => {
      this.setData({ editMode: false, submitting: false });
      wx.showToast({ title: '修改成功', icon: 'success' });
      this.loadDetail();
    }).catch(() => {
      this.setData({ submitting: false });
    });
  },

  /**
   * 删除记录
   */
  onDelete() {
    util.showConfirm('确认删除', '删除后数据不可恢复').then(confirmed => {
      if (confirmed) {
        api.del(`/api/records/${this.data.recordId}`)
          .then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => wx.navigateBack(), 1500);
          })
          .catch(() => {});
      }
    });
  }
});
