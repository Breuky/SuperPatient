/**
 * 把后端返回的图片地址转成浏览器可直接加载的 URL。
 * 图片接口需要鉴权，而 <img> 无法自定义请求头，因此把 token 拼在 query 上。
 * 外链（http/https 开头）与旧的本地上传路径原样返回。
 */
export function fileUrl(url) {
  if (!url) return ''
  if (!url.startsWith('/api/files/')) return url
  const token = localStorage.getItem('token') || ''
  return `${url}?token=${encodeURIComponent(token)}`
}
