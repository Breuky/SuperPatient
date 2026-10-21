import dayjs from 'dayjs'

export function formatDate(date, format = 'YYYY-MM-DD') {
  if (!date) return ''
  return dayjs(date).format(format)
}

export function formatDateTime(date) {
  return formatDate(date, 'YYYY-MM-DDTHH:mm:ss')
}

export function formatRelative(date) {
  if (!date) return ''
  const d = dayjs(date)
  const now = dayjs()
  const diffDays = now.diff(d, 'day')
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  return d.format('MM-DD')
}

export function ageFromBirthday(birthDate) {
  if (!birthDate) return null
  return dayjs().diff(dayjs(birthDate), 'year')
}

export function genderText(gender) {
  if (gender === 0) return '女'
  if (gender === 1) return '男'
  return '未知'
}

export function now() {
  return dayjs().format('YYYY-MM-DDTHH:mm:ss')
}

export function today() {
  return dayjs().format('YYYY-MM-DD')
}
