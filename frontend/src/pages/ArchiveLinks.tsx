import React, { useEffect, useState, useCallback } from 'react'
import {
  Table, Button, Input, Select, Space, Modal, Form,
  InputNumber, Popconfirm, message, Tag, Typography, Tooltip,
} from 'antd'
import { PlusOutlined, SearchOutlined, LinkOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import PageContainer from '../components/PageContainer'
import {
  getArchiveLinks, createArchiveLink, updateArchiveLink, deleteArchiveLink,
} from '../api/archive'
import { getCohorts } from '../api/cohort'
import type { ArchiveLink, ArchiveLinkForm } from '../types/archive'
import type { Cohort } from '../types/cohort'
import { cohortLabel } from '../utils/cohort'
import { canManage } from '../utils/permission'

const { Text } = Typography

const ARCHIVE_TYPES = ['证书材料', '活动照片', '日常展示', '推文截图', '总结材料', '其他']
const TYPE_COLOR: Record<string, string> = {
  证书材料: 'gold', 活动照片: 'blue', 日常展示: 'cyan',
  推文截图: 'purple', 总结材料: 'green', 其他: 'default',
}
const currentYear = dayjs().year()
const YEAR_OPTIONS = Array.from({ length: 6 }, (_, i) => currentYear - i)

const ArchiveLinks: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<ArchiveLink[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [yearFilter, setYearFilter] = useState<number | undefined>()
  const [typeFilter, setTypeFilter] = useState<string>('')
  const [cohorts, setCohorts] = useState<Cohort[]>([])
  const [cohortFilter, setCohortFilter] = useState<number | undefined>()
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<ArchiveLink | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<ArchiveLinkForm>()

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getArchiveLinks({
        year: yearFilter,
        type: typeFilter || undefined,
        cohortId: cohortFilter,
        keyword: keyword || undefined,
        page,
        size: pageSize,
      })
      setData(res.list ?? [])
      setTotal(res.total ?? 0)
    } finally {
      setLoading(false)
    }
  }, [yearFilter, typeFilter, keyword, page, pageSize, cohortFilter])

  useEffect(() => { fetchData() }, [fetchData])
  useEffect(() => { getCohorts().then(setCohorts) }, [])

  const handleAdd = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ archiveYear: currentYear, cohortIds: [] })
    setModalOpen(true)
  }

  const handleEdit = (record: ArchiveLink) => {
    setEditing(record)
    form.setFieldsValue({
      ...record,
      cohortIds: record.cohorts?.map((c) => c.id) ?? [],
    })
    setModalOpen(true)
  }

  const handleDelete = async (id: number) => {
    await deleteArchiveLink(id)
    message.success('删除成功')
    fetchData()
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      if (editing) {
        await updateArchiveLink(editing.id, values)
        message.success('修改成功')
      } else {
        await createArchiveLink(values)
        message.success('新增成功')
      }
      setModalOpen(false)
      fetchData()
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<ArchiveLink> = [
    { title: '年份', dataIndex: 'archiveYear', width: 80 },
    {
      title: '所属届次', dataIndex: 'cohorts', width: 150,
      render: (cohorts: Cohort[] | undefined) =>
        !cohorts || cohorts.length === 0
          ? <Tag>未分届</Tag>
          : cohorts.map((c) => <Tag key={c.id}>{cohortLabel(c.year)}</Tag>),
    },
    {
      title: '类型', dataIndex: 'archiveType', width: 100,
      render: (v: string) => <Tag color={TYPE_COLOR[v] ?? 'default'}>{v}</Tag>,
    },
    { title: '名称', dataIndex: 'title', width: 180, ellipsis: true },
    {
      title: '链接', dataIndex: 'url', ellipsis: true,
      render: (url: string) => (
        <Tooltip title={url}>
          <Text type="secondary" style={{ fontSize: 12 }}>{url.slice(0, 40)}{url.length > 40 ? '…' : ''}</Text>
        </Tooltip>
      ),
    },
    { title: '说明', dataIndex: 'description', ellipsis: true, width: 150 },
    {
      title: '操作', width: canManage() ? 200 : 100, fixed: 'right',
      render: (_: unknown, record: ArchiveLink) => (
        <Space>
          <Button
            size="small" icon={<LinkOutlined />} type="link"
            onClick={() => window.open(record.url, '_blank', 'noopener,noreferrer')}
          >
            打开链接
          </Button>
          {canManage() && (
            <>
              <Button size="small" onClick={() => handleEdit(record)}>编辑</Button>
              <Popconfirm
                title="确认删除该归档链接？"
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
      title="活动资料归档"
      extra={canManage() && (
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          新增归档
        </Button>
      )}
    >
      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          allowClear placeholder="年份筛选"
          style={{ width: 110 }}
          value={yearFilter}
          onChange={(v) => { setYearFilter(v); setPage(1) }}
          options={YEAR_OPTIONS.map((y) => ({ label: `${y}年`, value: y }))}
        />
        <Select
          allowClear placeholder="届次筛选"
          style={{ width: 120 }}
          value={cohortFilter ?? undefined}
          onChange={(v) => { setCohortFilter(v ?? undefined); setPage(1) }}
          options={[
            ...cohorts.map((c) => ({ label: `${c.year}届`, value: c.id })),
            { label: '未分届', value: -1 },
          ]}
        />
        <Select
          allowClear placeholder="类型筛选"
          style={{ width: 120 }}
          value={typeFilter || undefined}
          onChange={(v) => { setTypeFilter(v ?? ''); setPage(1) }}
          options={ARCHIVE_TYPES.map((t) => ({ label: t, value: t }))}
        />
        <Input
          placeholder="搜索名称/说明"
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
        scroll={{ x: 900 }}
        pagination={{
          current: page, pageSize, total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => setPage(p),
        }}
      />

      <Modal
        title={editing ? '编辑归档链接' : '新增归档链接'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        okText="保存" cancelText="取消"
        width={520}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="资料名称" name="title" rules={[{ required: true, message: '请输入资料名称' }]}>
            <Input placeholder="例：2025年开源分享会照片" />
          </Form.Item>
          <Form.Item label="年份" name="archiveYear" rules={[{ required: true, message: '请输入年份' }]}>
            <InputNumber min={2020} max={2099} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="所属届次" name="cohortIds" rules={[{ required: true, message: '请选择所属届次' }]}>
            <Select
              mode="multiple"
              allowClear
              placeholder="请选择所属届次（可多选）"
              options={cohorts.map((c) => ({ label: `${c.year}届`, value: c.id }))}
              onChange={(vals: number[]) => {
                if (vals && vals.length > 0) {
                  const c = cohorts.find((x) => x.id === vals[0])
                  if (c) form.setFieldValue('archiveYear', c.year)
                }
              }}
            />
          </Form.Item>
          <Form.Item label="资料类型" name="archiveType" rules={[{ required: true, message: '请选择类型' }]}>
            <Select options={ARCHIVE_TYPES.map((t) => ({ label: t, value: t }))} placeholder="请选择资料类型" />
          </Form.Item>
          <Form.Item
            label="网盘链接" name="url"
            rules={[
              { required: true, message: '请输入链接' },
              { type: 'url', message: '请输入合法的 URL（以 http:// 或 https:// 开头）' },
            ]}
          >
            <Input placeholder="https://pan.example.com/..." />
          </Form.Item>
          <Form.Item label="说明" name="description">
            <Input.TextArea rows={2} placeholder="可选备注说明" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  )
}

export default ArchiveLinks
