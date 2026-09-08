import { message } from 'antd'
import { getToken } from './auth'

/**
 * 获取完整 API 地址
 */
function fullUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) {
    return path
  }
  const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  if (!base || path === base || path.startsWith(`${base}/`)) {
    return path
  }
  return base + path
}

/**
 * 通过后端接口下载/查看文件（使用原生 fetch，绕过 axios 拦截器对 blob 的误解析）
 */
async function fetchBlob(url: string): Promise<{ blob: Blob; filename: string } | null> {
  const token = getToken()
  const resp = await fetch(fullUrl(url), {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!resp.ok) {
    if (resp.status === 401) {
      message.error('登录已过期，请重新登录')
      setTimeout(() => { window.location.href = `${import.meta.env.BASE_URL}login` }, 1000)
      return null
    }
    message.error('文件请求失败')
    return null
  }
  const blob = await resp.blob()
  // 从 Content-Disposition 解析文件名
  const disposition = resp.headers.get('Content-Disposition') || ''
  let filename = '文件'
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  const plainMatch = disposition.match(/filename="?([^";]+)"?/i)
  if (utf8Match?.[1]) {
    filename = decodeURIComponent(utf8Match[1])
  } else if (plainMatch?.[1]) {
    try { filename = decodeURIComponent(plainMatch[1]) } catch { filename = plainMatch[1] }
  }
  return { blob, filename }
}

/** 下载文件到本地 */
export async function downloadFile(url: string, filename?: string): Promise<void> {
  const result = await fetchBlob(url)
  if (!result) return
  const blobUrl = window.URL.createObjectURL(result.blob)
  const a = document.createElement('a')
  a.href = blobUrl
  a.download = filename || result.filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(blobUrl)
}

/** 在新标签页中查看文件 */
export async function viewFile(url: string): Promise<void> {
  const result = await fetchBlob(url)
  if (!result) return
  const blobUrl = window.URL.createObjectURL(result.blob)
  window.open(blobUrl, '_blank', 'noopener,noreferrer')
}

/** 获取后端 view 接口的完整 URL */
export function getViewUrl(path: string): string {
  return fullUrl(path)
}

/** 获取后端 download 接口的完整 URL */
export function getDownloadUrl(path: string): string {
  return fullUrl(path)
}
