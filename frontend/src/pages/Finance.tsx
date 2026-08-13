import React, { useEffect, useState, useCallback } from 'react'
import {
  Card, Select, Row, Col, Button, Upload, Table, Space,
  Popconfirm, message, Typography, Empty, Divider, Alert, Tag,
} from 'antd'
import {
  UploadOutlined, DownloadOutlined, DeleteOutlined, InboxOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload'
import dayjs from 'dayjs'
import PageContainer from '../components/PageContainer'
import PermissionGuard from '../components/PermissionGuard'
import {
  getFinancePeriods, getFinanceMonthDetail,
  uploadFinanceReport, uploadFinanceVouchers,
  deleteFinanceFile, getFinanceDownloadUrl, getFinanceViewUrl,
} from '../api/finance'
import type { FinancePeriod, FinanceFile, FinanceMonthDetail } from '../types/finance'
import { downloadFile, viewFile } from '../utils/download'
import { canViewFinance } from '../utils/permission'

const { Text, Title } = Typography
const REPORT_EXTS = ['.doc', '.docx']
const VOUCHER_EXTS = ['.jpg', '.jpeg', '.png', '.pdf', '.doc', '.docx']
const currentYear = dayjs().year()
const YEAR_OPTIONS = Array.from({ length: 5 }, (_, i) => currentYear - i)
const MONTH_OPTIONS = Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 }))

const Finance: React.FC = () => {
  const [year, setYear] = useState<number>(currentYear)
  const [month, setMonth] = useState<number>(dayjs().month() + 1)
  const [periods, setPeriods] = useState<FinancePeriod[]>([])
  const [detail, setDetail] = useState<FinanceMonthDetail | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  // 报表上传
  const [reportFiles, setReportFiles] = useState<UploadFile[]>([])
  const [uploadingReport, setUploadingReport] = useState(false)

  // 凭据上传
  const [voucherFiles, setVoucherFiles] = useState<UploadFile[]>([])
  const [uploadingVouchers, setUploadingVouchers] = useState(false)

  const [downloading, setDownloading] = useState<number | null>(null)

  const fetchPeriods = useCallback(async () => {
    const res = await getFinancePeriods(year)
    setPeriods(res ?? [])
  }, [year])

  const fetchDetail = useCallback(async () => {
    setDetailLoading(true)
    try {
      const res = await getFinanceMonthDetail(year, month)
      setDetail(res)
    } catch {
      setDetail({ period: null, report: null, vouchers: [] })
    } finally {
      setDetailLoading(false)
    }
  }, [year, month])

  useEffect(() => { fetchPeriods() }, [fetchPeriods])
  useEffect(() => { fetchDetail() }, [fetchDetail])

  const handleView = async (file: FinanceFile) => {
    await viewFile(getFinanceViewUrl(file.id))
  }

  const handleDownload = async (file: FinanceFile) => {
    setDownloading(file.id)
    try {
      await downloadFile(getFinanceDownloadUrl(file.id), file.originalName)
    } finally {
      setDownloading(null)
    }
  }

  const handleDeleteFile = async (id: number) => {
    await deleteFinanceFile(id)
    message.success('删除成功')
    fetchDetail()
    fetchPeriods()
  }

  // 上传报表
  const beforeUploadReport = (file: File) => {
    const ext = '.' + file.name.split('.').pop()?.toLowerCase()
    if (!REPORT_EXTS.includes(ext)) {
      message.error('报表只允许上传 .doc 或 .docx 文件')
      return Upload.LIST_IGNORE
    }
    return false
  }

  const handleUploadReport = async () => {
    if (reportFiles.length === 0) { message.warning('请先选择文件'); return }
    const file = reportFiles[0]?.originFileObj
    if (!file) return
    setUploadingReport(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      await uploadFinanceReport(year, month, fd)
      message.success('报表上传成功')
      setReportFiles([])
      fetchDetail()
      fetchPeriods()
    } finally {
      setUploadingReport(false)
    }
  }

  // 上传凭据
  const beforeUploadVoucher = (file: File) => {
    const ext = '.' + file.name.split('.').pop()?.toLowerCase()
    if (!VOUCHER_EXTS.includes(ext)) {
      message.error('凭据不支持该文件类型')
      return Upload.LIST_IGNORE
    }
    return false
  }

  const handleUploadVouchers = async () => {
    if (voucherFiles.length === 0) { message.warning('请先选择文件'); return }
    setUploadingVouchers(true)
    try {
      const fd = new FormData()
      voucherFiles.forEach((f) => {
        if (f.originFileObj) fd.append('files', f.originFileObj)
      })
      await uploadFinanceVouchers(year, month, fd)
      message.success(`上传 ${voucherFiles.length} 个凭据成功`)
      setVoucherFiles([])
      fetchDetail()
      fetchPeriods()
    } finally {
      setUploadingVouchers(false)
    }
  }

  const voucherColumns: ColumnsType<FinanceFile> = [
    {
      title: '文件名', dataIndex: 'originalName', ellipsis: true,
      render: (name: string, record: FinanceFile) => (
        <a onClick={() => handleView(record)} style={{ cursor: 'pointer' }}>
          {name || '未知文件'}
        </a>
      ),
    },
    {
      title: '上传时间', dataIndex: 'createdAt', width: 160,
      render: (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—',
    },
    {
      title: '操作', width: 180, fixed: 'right',
      render: (_: unknown, record: FinanceFile) => (
        <Space>
          <Button size="small" icon={<EyeOutlined />}
            onClick={() => handleView(record)}>查看</Button>
          <Button size="small" icon={<DownloadOutlined />}
            loading={downloading === record.id}
            onClick={() => handleDownload(record)}>下载</Button>
          <Popconfirm title="确认删除？" onConfirm={() => handleDeleteFile(record.id)} okText="删除" cancelText="取消">
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const report = detail?.report ?? null
  const vouchers = detail?.vouchers ?? []

  return (
    <PermissionGuard allowed={canViewFinance()} message="当前身份无权限访问财务台账">
      <PageContainer title="财务台账">
        {/* 年份 + 月份选择 */}
        <Card style={{ marginBottom: 16 }}>
          <Space wrap>
            <Text strong>年份：</Text>
            <Select
              value={year}
              onChange={(v) => setYear(v)}
              style={{ width: 100 }}
              options={YEAR_OPTIONS.map((y) => ({ label: `${y}年`, value: y }))}
            />
            <Text strong>月份：</Text>
            <Select
              value={month}
              onChange={(v) => setMonth(v)}
              style={{ width: 100 }}
              options={MONTH_OPTIONS}
            />
            {periods.length > 0 && (
              <>
                <Text type="secondary">已有记录：</Text>
                <Space>
                  {periods.map((p) => (
                    <Tag
                      key={p.id}
                      color={p.financeMonth === month && p.financeYear === year ? 'blue' : 'default'}
                      style={{ cursor: 'pointer' }}
                      onClick={() => setMonth(p.financeMonth)}
                    >
                      {p.financeMonth}月
                    </Tag>
                  ))}
                </Space>
              </>
            )}
          </Space>
        </Card>

        <Row gutter={[16, 16]}>
          {/* ─── 月度支出报表 ─── */}
          <Col xs={24} lg={12}>
            <Card
              title={<Title level={5} style={{ margin: 0 }}>{year}年{month}月 支出报表</Title>}
              loading={detailLoading}
            >
              {report ? (
                <div style={{ marginBottom: 16 }}>
                  <div style={{
                    background: '#f6f8fa', borderRadius: 8, padding: '12px 16px',
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  }}>
                    <div>
                      <a
                        onClick={() => handleView(report)}
                        style={{ fontSize: 15, fontWeight: 500, cursor: 'pointer' }}
                      >
                        {report.originalName || '报表文件'}
                      </a>
                      <div style={{ marginTop: 4 }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          上传时间：{report.createdAt ? dayjs(report.createdAt).format('YYYY-MM-DD HH:mm') : '—'}
                        </Text>
                      </div>
                    </div>
                    <Space>
                      <Button size="small" icon={<EyeOutlined />} onClick={() => handleView(report)}>查看</Button>
                      <Button size="small" icon={<DownloadOutlined />}
                        loading={downloading === report.id}
                        onClick={() => handleDownload(report)}>下载</Button>
                      <Popconfirm title="确认删除该报表？" onConfirm={() => handleDeleteFile(report.id)} okText="删除" cancelText="取消">
                        <Button size="small" danger icon={<DeleteOutlined />} />
                      </Popconfirm>
                    </Space>
                  </div>
                  <Divider style={{ margin: '12px 0' }} />
                  <Alert message="上传新报表将替换当前报表" type="warning" showIcon style={{ marginBottom: 12 }} />
                </div>
              ) : (
                <Empty description="本月暂无支出报表" style={{ marginBottom: 16 }} />
              )}
              <Upload
                fileList={reportFiles}
                beforeUpload={beforeUploadReport}
                onChange={({ fileList: fl }) => setReportFiles(fl.slice(-1))}
                maxCount={1} accept=".doc,.docx"
                onRemove={() => setReportFiles([])}
              >
                <Button icon={<UploadOutlined />}>选择报表文件（.doc/.docx）</Button>
              </Upload>
              {reportFiles.length > 0 && (
                <Button
                  type="primary" style={{ marginTop: 8 }}
                  loading={uploadingReport}
                  onClick={handleUploadReport}
                >
                  上传报表
                </Button>
              )}
            </Card>
          </Col>

          {/* ─── 凭据文件 ─── */}
          <Col xs={24} lg={12}>
            <Card
              title={<Title level={5} style={{ margin: 0 }}>{year}年{month}月 凭据文件</Title>}
              loading={detailLoading}
            >
              <Upload.Dragger
                multiple fileList={voucherFiles}
                beforeUpload={beforeUploadVoucher}
                onChange={({ fileList: fl }) => setVoucherFiles(fl)}
                accept=".jpg,.jpeg,.png,.pdf,.doc,.docx"
                style={{ marginBottom: 12 }}
              >
                <p className="ant-upload-drag-icon"><InboxOutlined /></p>
                <p className="ant-upload-text">点击或拖拽上传凭据</p>
                <p className="ant-upload-hint">支持 .jpg .jpeg .png .pdf .doc .docx，可多选</p>
              </Upload.Dragger>
              {voucherFiles.length > 0 && (
                <Button
                  type="primary" style={{ marginBottom: 12 }}
                  loading={uploadingVouchers}
                  onClick={handleUploadVouchers}
                >
                  上传 {voucherFiles.length} 个凭据
                </Button>
              )}

              {vouchers.length > 0 ? (
                <Table
                  rowKey="id"
                  columns={voucherColumns}
                  dataSource={vouchers}
                  size="small"
                  scroll={{ x: 500 }}
                  pagination={false}
                />
              ) : (
                <Empty description="本月暂无凭据文件" />
              )}
            </Card>
          </Col>
        </Row>
      </PageContainer>
    </PermissionGuard>
  )
}

export default Finance
