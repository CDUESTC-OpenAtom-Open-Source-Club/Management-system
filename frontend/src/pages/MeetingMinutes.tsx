import React, { useEffect, useState, useCallback } from 'react'
import {
  Table, Button, Input, Select, Space, Modal, Form,
  DatePicker, Upload, Popconfirm, message, Typography,
} from 'antd'
import {
  PlusOutlined, DownloadOutlined, UploadOutlined, SearchOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload'
import dayjs from 'dayjs'
import PageContainer from '../components/PageContainer'
import {
  getMeetingMinutes, createMeetingMinute,
  updateMeetingMinute, deleteMeetingMinute, getMeetingDownloadUrl,
} from '../api/meeting'
import type { MeetingMinute } from '../types/meeting'
import { downloadFile } from '../utils/download'
import { canManage } from '../utils/permission'

const { Text } = Typography
const ALLOWED_EXTS = ['.doc', '.docx']
const currentYear = dayjs().year()
const YEAR_OPTIONS = Array.from({ length: 6 }, (_, i) => currentYear - i)
const MONTH_OPTIONS = Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 }))

const MeetingMinutes: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<MeetingMinute[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [yearFilter, setYearFilter] = useState<number | undefined>()
  const [monthFilter, setMonthFilter] = useState<number | undefined>()
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [isEdit, setIsEdit] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [downloading, setDownloading] = useState<number | null>(null)
  const [form] = Form.useForm()

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getMeetingMinutes({
        year: yearFilter,
        month: monthFilter,
        keyword: keyword || undefined,
        page,
        size: pageSize,
      })
      setData(res.list ?? [])
      setTotal(res.total ?? 0)
    } finally {
      setLoading(false)
    }
  }, [yearFilter, monthFilter, keyword, page, pageSize])

  useEffect(() => { fetchData() }, [fetchData])

  const openAdd = () => {
    setIsEdit(false)
    setEditingId(null)
    form.resetFields()
    setFileList([])
    setModalOpen(true)
  }

  const openEdit = (record: MeetingMinute) => {
    setIsEdit(true)
    setEditingId(record.id)
    form.setFieldsValue({
      title: record.title,
      meetingDate: dayjs(record.meetingDate),
      remark: record.remark,
    })
    setFileList([])
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    setFileList([])
    form.resetFields()
  }

  const beforeUpload = (file: File) => {
    const ext = '.' + file.name.split('.').pop()?.toLowerCase()
    if (!ALLOWED_EXTS.includes(ext)) {
      message.error('只允许上传 .doc 或 .docx 文件')
      return Upload.LIST_IGNORE
    }
    return false
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    if (!isEdit && fileList.length === 0) {
      message.error('请选择要上传的文件')
      return
    }
    setSubmitting(true)
    try {
      const fd = new FormData()
      fd.append('title', values.title)
      fd.append('meetingDate', dayjs(values.meetingDate).format('YYYY-MM-DD'))
      if (values.remark) fd.append('remark', values.remark)
      if (fileList.length > 0 && fileList[0].originFileObj) {
        fd.append('file', fileList[0].originFileObj)
      }
      if (isEdit && editingId) {
        await updateMeetingMinute(editingId, fd)
        message.success('修改成功')
      } else {
        await createMeetingMinute(fd)
        message.success('上传成功')
      }
      closeModal()
      fetchData()
    } finally {
      setSubmitting(false)
    }
  }

  const handleDownload = async (record: MeetingMinute) => {
    setDownloading(record.id)
    try {
      await downloadFile(getMeetingDownloadUrl(record.id), `${record.title}.docx`)
    } finally {
      setDownloading(null)
    }
  }

  const handleDelete = async (id: number) => {
    await deleteMeetingMinute(id)
    message.success('删除成功')
    fetchData()
  }

  const columns: ColumnsType<MeetingMinute> = [
    { title: '会议名称', dataIndex: 'title', ellipsis: true, width: 200 },
    { title: '会议日期', dataIndex: 'meetingDate', width: 110 },
    { title: '年份', dataIndex: 'meetingYear', width: 70 },
    { title: '月份', dataIndex: 'meetingMonth', width: 65, render: (v: number) => `${v}月` },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    {
      title: '操作', width: canManage() ? 230 : 100, fixed: 'right',
      render: (_: unknown, record: MeetingMinute) => (
        <Space>
          <Button
            size="small" icon={<DownloadOutlined />}
            loading={downloading === record.id}
            onClick={() => handleDownload(record)}
          >
            下载
          </Button>
          {canManage() && (
            <>
              <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
              <Popconfirm
                title="确认删除该会议纪要？"
                onConfirm={() => handleDelete(record.id)}
                okText="删除" cancelText="取消"
              >
                <Button size="small" danger>删除</Button>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ]

  return (
    <PageContainer
      title="会议纪要"
      extra={canManage() && (
        <Button type="primary" icon={<PlusOutlined />} onClick={openAdd}>
          上传会议纪要
        </Button>
      )}
    >
      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          allowClear placeholder="年份"
          style={{ width: 100 }}
          value={yearFilter}
          onChange={(v) => { setYearFilter(v); setPage(1) }}
          options={YEAR_OPTIONS.map((y) => ({ label: `${y}年`, value: y }))}
        />
        <Select
          allowClear placeholder="月份"
          style={{ width: 100 }}
          value={monthFilter}
          onChange={(v) => { setMonthFilter(v); setPage(1) }}
          options={MONTH_OPTIONS}
        />
        <Input
          placeholder="搜索会议名称"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onPressEnter={() => { setKeyword(searchInput); setPage(1) }}
          style={{ width: 220 }}
          prefix={<SearchOutlined />}
          allowClear
          onClear={() => { setKeyword(''); setSearchInput(''); setPage(1) }}
        />
        <Button type="primary" icon={<SearchOutlined />}
          onClick={() => { setKeyword(searchInput); setPage(1) }}>
          搜索
        </Button>
      </Space>

      <Table
        rowKey="id" columns={columns} dataSource={data} loading={loading}
        scroll={{ x: 800 }}
        pagination={{
          current: page, pageSize, total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => setPage(p),
        }}
      />

      <Modal
        title={isEdit ? '编辑会议纪要' : '上传会议纪要'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={closeModal}
        confirmLoading={submitting}
        okText="保存" cancelText="取消"
        width={520}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="会议名称" name="title" rules={[{ required: true, message: '请输入会议名称' }]}>
            <Input placeholder="例：2025年第三次例会" />
          </Form.Item>
          <Form.Item label="会议日期" name="meetingDate" rules={[{ required: true, message: '请选择会议日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={2} placeholder="可选备注" />
          </Form.Item>
          <Form.Item
            label={isEdit ? '替换文件（可选）' : '上传文件'}
            required={!isEdit}
          >
            <Upload
              fileList={fileList}
              beforeUpload={beforeUpload}
              onChange={({ fileList: fl }) => setFileList(fl.slice(-1))}
              maxCount={1}
              accept=".doc,.docx"
              onRemove={() => setFileList([])}
            >
              <Button icon={<UploadOutlined />}>
                {isEdit ? '选择新文件（.doc/.docx）' : '选择文件（.doc/.docx）'}
              </Button>
            </Upload>
            {isEdit && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                不选择文件则保留原文件
              </Text>
            )}
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  )
}

export default MeetingMinutes
