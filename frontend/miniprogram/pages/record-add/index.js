// pages/record-add/index.js - 新增检查记录
const api = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    patientId: null,
    // 分类与指标
    categories: [],
    selectedCategoryId: null,
    indicators: [],
    // 表单
    examDate: util.formatDateTime(new Date()),
    hospital: '',
    doctor: '',
    notes: '',
    imageUrl: '',        // 存库用的相对路径
    imagePreviewUrl: '', // 展示用（带 token）
    // 指标值：{ indicatorId: { valueDecimal, valueText } }
    valuesMap: {},
    // 加载
    loadingCategories: false,
    submitting: false
  },

  onLoad(options) {
    this.setData({ patientId: options.patientId });
    this.loadCategories();
  },

  /**
   * 加载分类列表（含指标）
   */
  loadCategories() {
    this.setData({ loadingCategories: true });
    api.get('/api/categories')
      .then(res => {
        const categories = res.data || [];
        this.setData({
          categories,
          loadingCategories: false,
          selectedCategoryId: categories.length > 0 ? categories[0].id : null
        }, () => {
          this.updateIndicators();
        });
      })
      .catch(() => {
        this.setData({ loadingCategories: false });
      });
  },

  /**
   * 切换分类 → 更新指标列表
   */
  onCategoryChange(e) {
    this.setData({
      selectedCategoryId: parseInt(e.currentTarget.dataset.id),
      valuesMap: {} // 清空已填值
    }, () => {
      this.updateIndicators();
    });
  },

  /**
   * 更新当前分类下的指标
   */
  updateIndicators() {
    const cat = this.data.categories.find(c => c.id === this.data.selectedCategoryId);
    this.setData({ indicators: cat ? (cat.indicators || []) : [] });
  },

  /**
   * 日期选择
   */
  onDateChange(e) {
    this.setData({ examDate: e.detail.value });
  },

  /**
   * 表单字段变更
   */
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value });
  },

  /**
   * 指标值变更
   */
  onValueChange(e) {
    const indicatorId = e.currentTarget.dataset.id;
    const valueType = e.currentTarget.dataset.type;
    const value = e.detail.value;
    const valuesMap = { ...this.data.valuesMap };

    if (!valuesMap[indicatorId]) {
      valuesMap[indicatorId] = {};
    }

    if (valueType === 'number') {
      valuesMap[indicatorId].valueDecimal = value;
    } else {
      valuesMap[indicatorId].valueText = value;
    }

    this.setData({ valuesMap });
  },

  /**
   * 拍照 / 选择照片
   */
  onChooseImage() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const filePath = res.tempFilePaths[0];
        // 上传到后端
        api.uploadFile(filePath, 'report')
          .then(data => {
            this.setData({ imageUrl: data.url, imagePreviewUrl: api.fileUrl(data.url) });
            wx.showToast({ title: '上传成功', icon: 'success' });
          })
          .catch(() => {});
      }
    });
  },

  /**
   * 删除照片
   */
  onRemoveImage() {
    this.setData({ imageUrl: '', imagePreviewUrl: '' });
  },

  /**
   * 预览照片
   */
  onPreviewImage() {
    wx.previewImage({
      urls: [this.data.imagePreviewUrl],
      current: this.data.imagePreviewUrl
    });
  },

  /**
   * 构建 values 数组
   */
  buildValues() {
    const { indicators, valuesMap } = this.data;
    const values = [];

    for (const indicator of indicators) {
      const val = valuesMap[indicator.id];
      // 跳过未填写的指标
      if (!val) continue;

      const item = { indicatorId: indicator.id };
      if (indicator.valueType === 'number') {
        if (val.valueDecimal === undefined || val.valueDecimal === '') continue;
        item.valueDecimal = parseFloat(val.valueDecimal);
      } else {
        if (!val.valueText || !val.valueText.trim()) continue;
        item.valueText = val.valueText.trim();
      }
      values.push(item);
    }

    return values;
  },

  /**
   * 提交检查记录
   */
  onSubmit() {
    const values = this.buildValues();

    if (values.length === 0) {
      wx.showToast({ title: '请至少填写一项指标', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    const data = {
      patientId: parseInt(this.data.patientId),
      categoryId: this.data.selectedCategoryId,
      examDate: util.toISODateTime(this.data.examDate),
      hospital: this.data.hospital || undefined,
      doctor: this.data.doctor || undefined,
      notes: this.data.notes || undefined,
      imageUrl: this.data.imageUrl || undefined,
      values
    };

    api.post('/api/records', data, { showLoading: true, loadingText: '提交中...' })
      .then(() => {
        this.setData({ submitting: false });
        wx.showToast({ title: '录入成功', icon: 'success' });
        setTimeout(() => wx.navigateBack(), 1500);
      })
      .catch(() => {
        this.setData({ submitting: false });
      });
  }
});
