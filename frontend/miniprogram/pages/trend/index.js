// pages/trend/index.js - 趋势图表页
const api = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    patients: [],
    patientIndex: 0,
    selectedPatientId: null,
    // 指标列表
    trendableIndicators: [],
    selectedIndicatorId: null,
    selectedIndicator: null,
    // 趋势数据
    points: [],
    // 日期范围
    startDate: '',
    endDate: '',
    // Canvas
    canvasWidth: 0,
    canvasHeight: 0,
    chartReady: false
  },

  onLoad() {
    this.loadPatients();
  },

  /**
   * 加载患者列表（用于选择）
   */
  loadPatients() {
    api.get('/api/patients', { pageSize: 100 })
      .then(res => {
        const patients = res.data.rows || [];
        this.setData({ patients, patientIndex: 0 });
        if (patients.length > 0) {
          this.setData({ selectedPatientId: patients[0].id }, () => {
            this.loadTrendableIndicators();
          });
        }
      })
      .catch(() => {});
  },

  /**
   * 选择患者
   */
  onPatientChange(e) {
    const idx = parseInt(e.detail.value);
    const patient = this.data.patients[idx];
    this.setData({
      patientIndex: idx,
      selectedPatientId: patient.id,
      selectedIndicatorId: null,
      points: [],
      chartReady: false
    }, () => {
      this.loadTrendableIndicators();
    });
  },

  /**
   * 加载可查趋势的指标
   */
  loadTrendableIndicators() {
    if (!this.data.selectedPatientId) return;
    api.get(`/api/patients/${this.data.selectedPatientId}/trendable-indicators`)
      .then(res => {
        const indicators = res.data || [];
        this.setData({ trendableIndicators: indicators });
      })
      .catch(() => {});
  },

  /**
   * 选择指标 → 加载趋势
   */
  onIndicatorTap(e) {
    const id = e.currentTarget.dataset.id;
    const indicator = this.data.trendableIndicators.find(i => i.indicatorId === id);
    this.setData({ selectedIndicatorId: id, selectedIndicator: indicator }, () => {
      this.loadTrend();
    });
  },

  /**
   * 加载趋势数据
   */
  loadTrend() {
    const { selectedPatientId, selectedIndicatorId, startDate, endDate } = this.data;
    if (!selectedPatientId || !selectedIndicatorId) return;

    api.get(`/api/patients/${selectedPatientId}/trends`, {
      indicatorId: selectedIndicatorId,
      startDate: startDate || undefined,
      endDate: endDate || undefined
    }).then(res => {
      const data = res.data;
      this.setData({
        selectedIndicator: data.indicator,
        points: data.points || []
      }, () => {
        this.drawChart();
      });
    }).catch(() => {});
  },

  /**
   * Canvas 绘制折线图
   */
  drawChart() {
    const { points, selectedIndicator } = this.data;
    if (points.length === 0) return;

    const query = wx.createSelectorQuery();
    query.select('#trendCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0] || !res[0].node) return;

        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');
        const dpr = wx.getSystemInfoSync().pixelRatio;
        const width = res[0].width;
        const height = res[0].height;

        canvas.width = width * dpr;
        canvas.height = height * dpr;
        ctx.scale(dpr, dpr);

        this.setData({ canvasWidth: width, canvasHeight: height, chartReady: true });
        this.renderChart(ctx, width, height, points, selectedIndicator);
      });
  },

  /**
   * 渲染折线图
   */
  renderChart(ctx, w, h, points, indicator) {
    const padding = { top: 30, right: 40, bottom: 50, left: 60 };
    const chartW = w - padding.left - padding.right;
    const chartH = h - padding.top - padding.bottom;

    // 清空
    ctx.clearRect(0, 0, w, h);

    // 计算数值范围
    let values = points.map(p => p.value);
    let dataMin = Math.min(...values);
    let dataMax = Math.max(...values);

    // 扩展范围包含正常范围
    if (indicator.normalMin != null) dataMin = Math.min(dataMin, indicator.normalMin);
    if (indicator.normalMax != null) dataMax = Math.max(dataMax, indicator.normalMax);
    const range = dataMax - dataMin || 1;
    dataMin -= range * 0.1;
    dataMax += range * 0.1;

    const scaleX = (i) => padding.left + (chartW * i) / Math.max(points.length - 1, 1);
    const scaleY = (v) => padding.top + chartH - ((v - dataMin) / (dataMax - dataMin)) * chartH;

    // 绘制网格和 Y 轴标签
    ctx.strokeStyle = '#EBEBEB';
    ctx.lineWidth = 0.5;
    ctx.fillStyle = '#999';
    ctx.font = '18rpx sans-serif';
    ctx.textAlign = 'right';
    for (let i = 0; i <= 4; i++) {
      const y = padding.top + (chartH * i) / 4;
      const val = dataMax - ((dataMax - dataMin) * i) / 4;
      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(w - padding.right, y);
      ctx.stroke();
      ctx.fillText(val.toFixed(1), padding.left - 8, y + 4);
    }

    // 正常范围区域
    if (indicator.normalMin != null && indicator.normalMax != null) {
      ctx.fillStyle = 'rgba(7, 193, 96, 0.08)';
      const yMax = scaleY(indicator.normalMax);
      const yMin = scaleY(indicator.normalMin);
      ctx.fillRect(padding.left, yMax, chartW, yMin - yMax);
    }

    // 绘制折线
    ctx.strokeStyle = '#07C160';
    ctx.lineWidth = 2;
    ctx.beginPath();
    points.forEach((p, i) => {
      const x = scaleX(i);
      const y = scaleY(p.value);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // 绘制数据点和日期标签
    points.forEach((p, i) => {
      const x = scaleX(i);
      const y = scaleY(p.value);

      // 点
      ctx.beginPath();
      ctx.arc(x, y, p.isAbnormal !== 0 ? 6 : 4, 0, Math.PI * 2);
      ctx.fillStyle = p.isAbnormal !== 0 ? '#FA5151' : '#07C160';
      ctx.fill();
      if (p.isAbnormal !== 0) {
        ctx.strokeStyle = '#fff';
        ctx.lineWidth = 1.5;
        ctx.stroke();
      }

      // X 轴日期
      const dateStr = util.formatDate(p.examDate);
      ctx.fillStyle = '#999';
      ctx.font = '20rpx sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(dateStr, x, h - padding.bottom + 24);
    });
  },

  /**
   * 日期筛选
   */
  onStartDateChange(e) {
    this.setData({ startDate: e.detail.value }, () => this.loadTrend());
  },

  onEndDateChange(e) {
    this.setData({ endDate: e.detail.value }, () => this.loadTrend());
  }
});
