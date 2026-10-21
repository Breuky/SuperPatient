// util.js - 工具函数

/**
 * 格式化日期 (yyyy-MM-dd)
 */
function formatDate(date) {
  if (!date) return '';
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * 格式化日期时间 (yyyy-MM-dd HH:mm)
 */
function formatDateTime(date) {
  if (!date) return '';
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hour = String(d.getHours()).padStart(2, '0');
  const min = String(d.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${min}`;
}

/**
 * 格式化日期时间为 ISO 格式 (yyyy-MM-ddTHH:mm:ss)
 */
function toISODateTime(date) {
  if (!date) return '';
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hour = String(d.getHours()).padStart(2, '0');
  const min = String(d.getMinutes()).padStart(2, '0');
  const sec = String(d.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day}T${hour}:${min}:${sec}`;
}

/**
 * 计算年龄 (从出生日期)
 */
function calcAge(birthDate) {
  if (!birthDate) return null;
  const birth = new Date(birthDate);
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) {
    age--;
  }
  return age;
}

/**
 * 性别文本
 */
function genderText(gender) {
  if (gender === 1) return '男';
  if (gender === 0) return '女';
  return '未知';
}

/**
 * 异常状态文本
 */
function abnormalText(isAbnormal) {
  if (isAbnormal === 1) return '偏高';
  if (isAbnormal === 2) return '偏低';
  return '正常';
}

/**
 * 异常状态样式类名
 */
function abnormalClass(isAbnormal) {
  if (isAbnormal === 1) return 'tag-abnormal-high';
  if (isAbnormal === 2) return 'tag-abnormal-low';
  return 'tag-normal';
}

/**
 * 防抖
 */
function debounce(fn, delay = 300) {
  let timer = null;
  return function (...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}

/**
 * 确认弹窗
 */
function showConfirm(title, content, confirmText = '确定') {
  return new Promise((resolve) => {
    wx.showModal({
      title,
      content,
      confirmText,
      confirmColor: '#FA5151',
      success(res) {
        resolve(res.confirm);
      }
    });
  });
}

module.exports = {
  formatDate,
  formatDateTime,
  toISODateTime,
  calcAge,
  genderText,
  abnormalText,
  abnormalClass,
  debounce,
  showConfirm
};
