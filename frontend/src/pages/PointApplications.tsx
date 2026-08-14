import React, { useEffect, useState, useCallback } from 'react'
import {
  Table, Select, Input, Space, Button, Tag, Modal,
  Form, message, Segmented,
} from 'antd'
import { SearchOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import PageContainer from '../components/PageContainer'
import PermissionGuard from '../components/PermissionGuard'
import {
  getPointApplications,
  approvePointApplication,
  rejectPointApplication,
} from '../api/point'
import { getCohorts } from '../api/cohort'
import type { PointApplication } from '../types/point'
import type { Cohort } from '../types/cohort'
import { canManage } from '../utils/permission'

const STATUS_TAG: Record<string, { color: string; label: string }> = {
  PENDING: { color: 'gold', label: '待审核' },
  APPROVED: { color: 'green', label: '已通过' },
  REJECTED: { color: 'red', label: '已驳回' },
}
const UNASSIGNED = 'unassigned'

const PointApplications: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<PointApplication[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [cohorts, setCohorts] = useState<Cohort[]>([])
  const [activeCohort, setActiveCohort] = useState<string>('all')

  const [rejectModalOpen, setRejectModalOpen] = useState(false)
  const [rejectingId, setRejectingId] = useState<number | null>(null)
  const [rejectForm] = Form.useForm<{ reviewComment: string }>()
  const [submitting, setSubmitting] = useState(false)

  const cohortId = activeCohort === 'all' ? undefined : activeCohort === UNASSIGNED ? -1 : Number(activeCohort)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getPointApplications({
        status: statusFilter || undefined,
        cohortId,
        keyword: keyword || undefined,
        page,
        size: pageSize,
      })
      setData(res.list ?? [])
      setTotal(res.total ?? 0)
    } finally {
      setLoading(false)
    }
  }, [statusFilter, keyword, page, pageSize, cohortId])

  useEffect(() => { fetchData() }, [fetchData])
  useEffect(() => { getCohorts().then(setCohorts) }, [])

  const handleApprove = async (id: number) => {
    await approvePointApplication(id)
    message.success('审核通过')
    fetchData()
  }

  const openRejectModal = (id: number) => {
    setRejectingId(id)
    rejectForm.resetFields()
    setRejectModalOpen(true)
  }

  const handleReject = async () => {
    const { reviewComment } = await rejectForm.validateFields()
    if (!rejectingId) return
    setSubmitting(true)
    try {
      await rejectPointApplication(rejectingId, reviewComment)
      message.success('已驳回')
      setRejectModalOpen(false)
      fetchData()
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<PointApplication> = [
    { title: '成员姓名', dataIndex: 'memberName', width: 90 },
    { title: '学号', dataIndex: 'studentNo', width: 120 },
    { title: '积分项目', dataIndex: 'itemName', width: 140 },
    {
      title: '分值', dataIndex: 'pointValue', width: 80,
      render: (v?: number) => v !== undefined ? (
        <Tag color={v > 0 ? 'green' : 'red'}>{v > 0 ? `+${v}` : v}</Tag>
      ) : '—'
    },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (s: string) => {
        const t = STATUS_TAG[s]
        return t ? <Tag color={t.color}>{t.label}</Tag> : s
      }
    },
    {
      title: '提交时间', dataIndex: 'createdAt', width: 155,
      render: (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—'
    },
    {
      title: '审核时间', dataIndex: 'reviewedAt', width: 155,
      render: (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—'
    },
    { title: '审核人', dataIndex: 'reviewedBy', width: 90 },
    { title: '审核意见', dataIndex: 'reviewComment', ellipsis: true, width: 150 },
    {
      title: '操作', width: 140, fixed: 'right',
      render: (_: unknown, record: PointApplication) =>
        record.status === 'PENDING' ? (
          <Space>
            <Button
              size="small" type="primary" icon={<CheckOutlined />}
              onClick={() => handleApprove(record.id)}
            >
              通过
            </Button>
            <Button
              size="small" danger icon={<CloseOutlined />}
              onClick={() => openRejectModal(record.id)}
            >
              驳回
            </Button>
          </Space>
        ) : (
          <span style={{ color: '#999' }}>已处理</span>
        ),
    },
  ]

  return (
    <PermissionGuard allowed={canManage()}>
      <PageContainer title="积分审核">
        <Segmented
          value={activeCohort}
          options={[
            { label: '全部', value: 'all' },
            ...cohorts.map((c) => ({ label: `${c.year}届`, value: String(c.id) })),
            { label: '未分届', value: UNASSIGNED },
          ]}
          onChange={(v) => { setActiveCohort(v as string); setPage(1) }}
          style={{ marginBottom: 12 }}
        />
        <Space style={{ marginBottom: 16 }} wrap>
          <Select
            value={statusFilter}
            onChange={(v) => { setStatusFilter(v); setPage(1) }}
            style={{ width: 120 }}
            options={[
              { label: '全部状态', value: '' },
              { label: '待审核', value: 'PENDING' },
              { label: '已通过', value: 'APPROVED' },
              { label: '已驳回', value: 'REJECTED' },
            ]}
          />
          <Input
            placeholder="搜索成员姓名/学号/积分项目"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onPressEnter={() => { setKeyword(searchInput); setPage(1) }}
            style={{ width: 260 }}
            prefix={<SearchOutlined />}
            allowClear
            onClear={() => { setKeyword(''); setSearchInput(''); setPage(1) }}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={() => { setKeyword(searchInput); setPage(1) }}
          >
            搜索
          </Button>
        </Space>

        <Table
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 1100 }}
          pagination={{
            current: page,
            pageSize,
            total,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p) => setPage(p),
          }}
        />

        <Modal
          title="驳回原因"
          open={rejectModalOpen}
          onOk={handleReject}
          onCancel={() => setRejectModalOpen(false)}
          confirmLoading={submitting}
          okText="确认驳回" cancelText="取消"
          okButtonProps={{ danger: true }}
        >
          <Form form={rejectForm} layout="vertical" style={{ marginTop: 16 }}>
            <Form.Item
              label="驳回原因"
              name="reviewComment"
              rules={[{ required: true, message: '请填写驳回原因' }]}
            >
              <Input.TextArea rows={3} placeholder="请填写驳回原因，例如：未在活动名单中" />
            </Form.Item>
          </Form>
        </Modal>
      </PageContainer>
    </PermissionGuard>
  )
}

export default PointApplications
