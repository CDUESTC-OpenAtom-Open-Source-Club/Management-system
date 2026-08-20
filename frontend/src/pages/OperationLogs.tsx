import React, { useEffect, useState, useCallback } from 'react'
import { Table, Select, Input, Space, Button, Tag } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import PageContainer from '../components/PageContainer'
import PermissionGuard from '../components/PermissionGuard'
import { getOperationLogs } from '../api/log'
import type { OperationLog } from '../types/log'
import { canViewLogs } from '../utils/permission'

const MODULE_OPTIONS = [
  { label: '全部', value: '' },
  { label: 'member（成员）', value: 'member' },
  { label: 'point（积分）', value: 'point' },
  { label: 'meeting（会议）', value: 'meeting' },
  { label: 'finance（财务）', value: 'finance' },
  { label: 'file（文件）', value: 'file' },
  { label: 'system（系统）', value: 'system' },
]

const ACTION_OPTIONS = [
  { label: '全部', value: '' },
  { label: 'CREATE', value: 'CREATE' },
  { label: 'UPDATE', value: 'UPDATE' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'APPROVE', value: 'APPROVE' },
  { label: 'REJECT', value: 'REJECT' },
  { label: 'UPLOAD', value: 'UPLOAD' },
  { label: 'DOWNLOAD', value: 'DOWNLOAD' },
  { label: 'REPLACE', value: 'REPLACE' },
]

const ACTION_COLOR: Record<string, string> = {
  CREATE: 'green', UPDATE: 'blue', DELETE: 'red',
  APPROVE: 'cyan', REJECT: 'orange', UPLOAD: 'purple',
  DOWNLOAD: 'default', REPLACE: 'gold',
}

const OperationLogs: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<OperationLog[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [moduleFilter, setModuleFilter] = useState('')
  const [actionFilter, setActionFilter] = useState('')
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getOperationLogs({
        moduleName: moduleFilter || undefined,
        actionType: actionFilter || undefined,
        keyword: keyword || undefined,
        page,
        size: pageSize,
      })
      setData(res.list ?? [])
      setTotal(res.total ?? 0)
    } finally {
      setLoading(false)
    }
  }, [moduleFilter, actionFilter, keyword, page, pageSize])

  useEffect(() => { fetchData() }, [fetchData])

  const columns: ColumnsType<OperationLog> = [
    { title: '操作人', dataIndex: 'operatorName', width: 90 },
    { title: '部门', dataIndex: 'operatorDepartment', width: 100 },
    { title: '职务', dataIndex: 'operatorPosition', width: 90 },
    { title: '模块', dataIndex: 'moduleName', width: 90,
      render: (v: string) => <Tag>{v}</Tag> },
    {
      title: '动作', dataIndex: 'actionType', width: 100,
      render: (v: string) => <Tag color={ACTION_COLOR[v] ?? 'default'}>{v}</Tag>,
    },
    { title: '目标ID', dataIndex: 'targetId', width: 80 },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    {
      title: '时间', dataIndex: 'createdAt', width: 155,
      render: (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '—',
    },
  ]

  return (
    <PermissionGuard allowed={canViewLogs()} message="当前身份无权限访问操作日志，仅限会长和副会长查看">
      <PageContainer title="操作日志">
        <Space style={{ marginBottom: 16 }} wrap>
          <Select
            value={moduleFilter || undefined}
            onChange={(v) => { setModuleFilter(v ?? ''); setPage(1) }}
            style={{ width: 160 }}
            placeholder="模块筛选"
            allowClear
            options={MODULE_OPTIONS.filter(o => o.value)}
          />
          <Select
            value={actionFilter || undefined}
            onChange={(v) => { setActionFilter(v ?? ''); setPage(1) }}
            style={{ width: 130 }}
            placeholder="动作筛选"
            allowClear
            options={ACTION_OPTIONS.filter(o => o.value)}
          />
          <Input
            placeholder="搜索描述/操作人"
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
      </PageContainer>
    </PermissionGuard>
  )
}

export default OperationLogs
