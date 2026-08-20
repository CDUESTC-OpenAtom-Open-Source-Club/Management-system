import React, { useEffect, useState, useCallback } from 'react'
import {
  Table, Input, Button, Space, Tag, Modal, Form,
  InputNumber, Select, DatePicker, Popconfirm, message,
  Typography, Card, Alert, Segmented, Drawer, Descriptions,
} from 'antd'
import {
  SearchOutlined, AimOutlined, LeftOutlined, RightOutlined,
  PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import PageContainer from '../components/PageContainer'
import {
  getPointsTable, searchPointTablePosition,
  getMemberPointDetails, createMemberPointRecord,
  updatePointRecord, deletePointRecord,
  getPointItems,
} from '../api/point'
import type {
  PointsTableRow, SearchPositionResponse, PointDetail, PointRecordForm,
} from '../types/point'
import type { PointItem } from '../types/point'
import { getCohorts } from '../api/cohort'
import type { Cohort } from '../types/cohort'
import { isFullAccess } from '../utils/permission'

const { Text } = Typography
const UNASSIGNED = 'unassigned'

const PointsTable: React.FC = () => {
  // ─── 积分总表 ────────────────────────────────────────────
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<PointsTableRow[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(50)
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [cohorts, setCohorts] = useState<Cohort[]>([])
  const [activeCohort, setActiveCohort] = useState<string | null>(null)

  // ─── 搜索定位 ────────────────────────────────────────────
  const [locateKeyword, setLocateKeyword] = useState('')
  const [locateInput, setLocateInput] = useState('')
  const [locateResult, setLocateResult] = useState<SearchPositionResponse | null>(null)
  const [matchIndex, setMatchIndex] = useState(0)
  const [highlightedMemberId, setHighlightedMemberId] = useState<number | null>(null)
  const [locating, setLocating] = useState(false)

  // ─── 积分明细 Drawer（仅 fullAccess） ─────────────────────
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailMember, setDetailMember] = useState<PointsTableRow | null>(null)
  const [detailRecords, setDetailRecords] = useState<PointDetail[]>([])
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailPage, setDetailPage] = useState(1)
  const [detailTotal, setDetailTotal] = useState(0)
  const [detailPageSize] = useState(10)

  // ─── 新增积分 Modal ──────────────────────────────────────
  const [addPointOpen, setAddPointOpen] = useState(false)
  const [addPointMember, setAddPointMember] = useState<PointsTableRow | null>(null)
  const [addPointForm] = Form.useForm<PointRecordForm>()
  const [addSubmitting, setAddSubmitting] = useState(false)

  // ─── 编辑积分记录 Modal ───────────────────────────────────
  const [editRecordOpen, setEditRecordOpen] = useState(false)
  const [editingRecord, setEditingRecord] = useState<PointDetail | null>(null)
  const [editRecordForm] = Form.useForm<PointRecordForm>()
  const [editSubmitting, setEditSubmitting] = useState(false)

  const [pointItems, setPointItems] = useState<PointItem[]>([])

  const cohortId = activeCohort === null ? undefined : activeCohort === UNASSIGNED ? -1 : Number(activeCohort)

  const activeCohortLabel = activeCohort === UNASSIGNED
    ? '未分届'
    : `${cohorts.find((c) => String(c.id) === activeCohort)?.year ?? ''}届`

  const fetchTable = useCallback(async (targetPage = page) => {
    if (activeCohort === null) return
    setLoading(true)
    try {
      const res = await getPointsTable({ page: targetPage, size: pageSize, keyword, cohortId })
      setRows(res.rows ?? [])
      setTotal(res.total ?? 0)
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, keyword, cohortId, activeCohort])

  useEffect(() => { fetchTable() }, [fetchTable])

  useEffect(() => {
    getPointItems().then((res) => setPointItems(res ?? []))
    getCohorts().then((list) => {
      setCohorts(list)
      const latest = list.find((c) => c.enabled)
      setActiveCohort(latest ? String(latest.id) : UNASSIGNED)
    })
  }, [])

  // ─── 搜索定位 ────────────────────────────────────────────
  const handleLocate = useCallback(async (idx: number) => {
    if (!locateKeyword) return
    setLocating(true)
    try {
      const res = await searchPointTablePosition({
        keyword: locateKeyword,
        pageSize,
        matchIndex: idx,
        cohortId,
      })
      setLocateResult(res)
      setMatchIndex(idx)
      setHighlightedMemberId(res.memberId)
      if (res.pageNo !== page) {
        setPage(res.pageNo)
      } else {
        fetchTable(res.pageNo)
      }
    } finally {
      setLocating(false)
    }
  }, [locateKeyword, pageSize, page, fetchTable, cohortId])

  const handleLocateSearch = () => {
    if (!locateInput.trim()) { message.warning('请输入搜索关键词'); return }
    setLocateKeyword(locateInput.trim())
    setMatchIndex(0)
  }

  useEffect(() => {
    if (locateKeyword) handleLocate(0)
  }, [locateKeyword]) // eslint-disable-line

  // ─── 查看明细（仅 fullAccess） ────────────────────────────
  const loadDetail = useCallback(async (memberId: number, targetPage: number) => {
    setDetailLoading(true)
    try {
      const res = await getMemberPointDetails({ memberId, page: targetPage, size: detailPageSize })
      setDetailRecords(res.list ?? [])
      setDetailTotal(res.total ?? 0)
    } finally {
      setDetailLoading(false)
    }
  }, [detailPageSize])

  const openDetail = (record: PointsTableRow) => {
    setDetailMember(record)
    setDetailOpen(true)
    setDetailPage(1)
    loadDetail(record.memberId, 1)
  }

  const refreshDetail = () => {
    if (detailMember) loadDetail(detailMember.memberId, detailPage)
  }

  // ─── 新增积分 ────────────────────────────────────────────
  const openAddPoint = (record: PointsTableRow) => {
    setAddPointMember(record)
    addPointForm.resetFields()
    setAddPointOpen(true)
  }

  const handleAddPoint = async () => {
    const values = await addPointForm.validateFields()
    if (!addPointMember) return
    setAddSubmitting(true)
    try {
      const payload: PointRecordForm = {
        ...values,
        occurredAt: values.occurredAt
          ? dayjs(values.occurredAt as unknown as dayjs.Dayjs).format('YYYY-MM-DDTHH:mm:ss')
          : undefined,
      }
      await createMemberPointRecord(addPointMember.memberId, payload)
      message.success('积分新增成功')
      setAddPointOpen(false)
      fetchTable()
      if (detailOpen && detailMember?.memberId === addPointMember.memberId) {
        refreshDetail()
      }
    } finally {
      setAddSubmitting(false)
    }
  }

  // ─── 编辑积分记录 ─────────────────────────────────────────
  const openEditRecord = (record: PointDetail) => {
    setEditingRecord(record)
    editRecordForm.setFieldsValue({
      pointItemId: record.pointItemId,
      score: record.score,
      reason: record.reason,
      occurredAt: record.occurredAt ? (dayjs(record.occurredAt) as unknown as string) : undefined,
    })
    setEditRecordOpen(true)
  }

  const handleEditRecord = async () => {
    const values = await editRecordForm.validateFields()
    if (!editingRecord) return
    setEditSubmitting(true)
    try {
      const payload: PointRecordForm = {
        ...values,
        occurredAt: values.occurredAt
          ? dayjs(values.occurredAt as unknown as dayjs.Dayjs).format('YYYY-MM-DDTHH:mm:ss')
          : undefined,
      }
      await updatePointRecord(editingRecord.id, payload)
      message.success('积分记录已更新')
      setEditRecordOpen(false)
      refreshDetail()
      fetchTable()
    } finally {
      setEditSubmitting(false)
    }
  }

  const handleDeleteRecord = async (id: number) => {
    await deletePointRecord(id)
    message.success('积分记录已删除')
    refreshDetail()
    fetchTable()
  }

  // ─── 分值单元格：正数绿、负数红、零灰 ───────────────────────
  const scoreCell = (v: number) => {
    if (v === 0) return <Text type="secondary">0</Text>
    return <Text style={{ color: v > 0 ? '#389e0d' : '#cf1322' }}>{v}</Text>
  }

  // ─── 固定列 ───────────────────────────────────────────────
  const tableColumns: ColumnsType<PointsTableRow> = [
    {
      title: '排名', dataIndex: 'rankNo', width: 64, fixed: 'left',
      render: (v: number) => (
        <Text strong style={{ color: v <= 3 ? '#f50' : undefined }}>{v}</Text>
      ),
    },
    { title: '姓名', dataIndex: 'name', width: 90, fixed: 'left' },
    {
      title: '总分', dataIndex: 'totalScore', width: 90, align: 'center',
      render: (v: number) => (
        <Text strong style={{ color: '#1677ff', fontSize: 16 }}>{v}</Text>
      ),
    },
    { title: '活动', dataIndex: 'activityScore', width: 80, align: 'center', render: scoreCell },
    { title: '比赛', dataIndex: 'competitionScore', width: 80, align: 'center', render: scoreCell },
    { title: '开源学习', dataIndex: 'openSourceLearningScore', width: 92, align: 'center', render: scoreCell },
    { title: '社区贡献', dataIndex: 'communityContributionScore', width: 92, align: 'center', render: scoreCell },
    { title: '演讲或主持', dataIndex: 'speechHostingScore', width: 96, align: 'center', render: scoreCell },
    { title: '其他', dataIndex: 'otherScore', width: 72, align: 'center', render: scoreCell },
    ...(isFullAccess() ? [{
      title: '操作', width: 150, fixed: 'right' as const,
      render: (_: unknown, record: PointsTableRow) => (
        <Space size={4}>
          <Button size="small" icon={<EyeOutlined />} onClick={() => openDetail(record)}>
            明细
          </Button>
          <Button
            size="small" type="primary" icon={<PlusOutlined />}
            onClick={() => openAddPoint(record)}
          >
            加分
          </Button>
        </Space>
      ),
    }] : []),
  ]

  const detailColumns: ColumnsType<PointDetail> = [
    {
      title: '日期', dataIndex: 'occurredAt', width: 150,
      render: (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—',
    },
    { title: '积分项目', dataIndex: 'pointItemName', width: 140, render: (v?: string) => v || '手动录入' },
    { title: '类型', dataIndex: 'pointItemType', width: 90, render: (v?: string) => v || '其他' },
    {
      title: '积分', dataIndex: 'score', width: 80,
      render: (v: number) => (
        <Tag color={v > 0 ? 'green' : v < 0 ? 'red' : 'default'}>{v > 0 ? `+${v}` : v}</Tag>
      ),
    },
    { title: '来源', dataIndex: 'sourceLabel', width: 90, render: (v?: string) => v || '—' },
    { title: '备注', dataIndex: 'reason', ellipsis: true },
    {
      title: '操作', width: 120,
      render: (_: unknown, record: PointDetail) => (
        <Space size={4}>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEditRecord(record)}>编辑</Button>
          <Popconfirm
            title="确认删除该积分记录？"
            onConfirm={() => handleDeleteRecord(record.id)}
            okText="删除" cancelText="取消"
          >
            <Button size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const pointItemOptions = pointItems.map((p) => ({ label: `${p.itemName}（+${p.pointValue}）`, value: p.id }))

  return (
    <PageContainer title="积分总表">
      <Segmented
        value={activeCohort ?? undefined}
        options={[
          ...cohorts.map((c) => ({ label: `${c.year}届`, value: String(c.id) })),
          { label: '未分届', value: UNASSIGNED },
        ]}
        onChange={(v) => { setActiveCohort(v as string); setPage(1) }}
        style={{ marginBottom: 16 }}
      />

      {/* 搜索定位 */}
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="关键词搜索（按姓名/学号）"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onPressEnter={() => { setKeyword(searchInput); setPage(1) }}
            style={{ width: 220 }}
            prefix={<SearchOutlined />}
            allowClear
            onClear={() => { setKeyword(''); setSearchInput(''); setPage(1) }}
          />
          <Button onClick={() => { setKeyword(searchInput); setPage(1) }}>搜索</Button>

          <Input
            placeholder="定位成员（输入姓名关键词）"
            value={locateInput}
            onChange={(e) => setLocateInput(e.target.value)}
            onPressEnter={handleLocateSearch}
            style={{ width: 200 }}
            prefix={<AimOutlined />}
            allowClear
            onClear={() => { setLocateInput(''); setLocateKeyword(''); setLocateResult(null); setHighlightedMemberId(null) }}
          />
          <Button
            type="primary" icon={<AimOutlined />}
            onClick={handleLocateSearch}
            loading={locating}
          >
            定位
          </Button>
          {locateResult && (
            <>
              <Button
                size="small" icon={<LeftOutlined />}
                disabled={matchIndex <= 0}
                onClick={() => handleLocate(matchIndex - 1)}
              >
                上一个
              </Button>
              <Button
                size="small" icon={<RightOutlined />}
                disabled={matchIndex >= locateResult.matchCount - 1}
                onClick={() => handleLocate(matchIndex + 1)}
              >
                下一个
              </Button>
            </>
          )}
        </Space>

        {locateResult && (
          <Alert
            style={{ marginTop: 12 }}
            type="success"
            showIcon
            message={
              `当前定位：${locateResult.name}（${locateResult.studentNo}），` +
              `排名第 ${locateResult.rankNo}，总分 ${locateResult.totalScore}，` +
              `匹配 ${matchIndex + 1}/${locateResult.matchCount}`
            }
          />
        )}
      </Card>

      {/* 积分总表 */}
      <Table
        rowKey="memberId"
        columns={tableColumns}
        dataSource={rows}
        loading={loading}
        scroll={{ x: 'max-content' }}
        rowClassName={(record) =>
          highlightedMemberId === record.memberId ? 'highlight-row' : ''
        }
        pagination={{
          current: page,
          pageSize,
          total,
          showTotal: (t) => `共 ${t} 人`,
          onChange: (p) => setPage(p),
        }}
      />

      {/* 积分明细 Drawer（仅 fullAccess 可见，只读 + 分页） */}
      <Drawer
        title={detailMember ? `${detailMember.name} - 积分明细` : '积分明细'}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={760}
      >
        {detailMember && (
          <Descriptions size="small" column={2} style={{ marginBottom: 12 }}>
            <Descriptions.Item label="届次">{activeCohortLabel}</Descriptions.Item>
            <Descriptions.Item label="总积分">
              <Text strong style={{ color: '#1677ff' }}>{detailMember.totalScore}</Text>
            </Descriptions.Item>
          </Descriptions>
        )}
        <Table
          rowKey="id"
          columns={detailColumns}
          dataSource={detailRecords}
          loading={detailLoading}
          pagination={{
            current: detailPage,
            pageSize: detailPageSize,
            total: detailTotal,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p) => {
              setDetailPage(p)
              if (detailMember) loadDetail(detailMember.memberId, p)
            },
          }}
          scroll={{ x: 820 }}
          size="small"
        />
      </Drawer>

      {/* 新增积分 Modal */}
      <Modal
        title={`手动新增积分 - ${addPointMember?.name ?? ''}`}
        open={addPointOpen}
        onOk={handleAddPoint}
        onCancel={() => setAddPointOpen(false)}
        confirmLoading={addSubmitting}
        okText="保存" cancelText="取消"
        width={480}
      >
        <Form form={addPointForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="关联积分项目（可选）" name="pointItemId">
            <Select
              allowClear placeholder="选择积分项目（可不选）"
              options={pointItemOptions}
            />
          </Form.Item>
          <Form.Item label="分值" name="score" rules={[{ required: true, message: '请输入分值（可为负数）' }]}>
            <InputNumber style={{ width: '100%' }} placeholder="正数加分，负数扣分" />
          </Form.Item>
          <Form.Item label="原因" name="reason" rules={[{ required: true, message: '请填写原因' }]}>
            <Input.TextArea rows={2} placeholder="例：手动补录：参加开源分享会" />
          </Form.Item>
          <Form.Item label="发生时间" name="occurredAt">
            <DatePicker showTime style={{ width: '100%' }} placeholder="选择时间（可不填）" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑积分记录 Modal */}
      <Modal
        title="编辑积分记录"
        open={editRecordOpen}
        onOk={handleEditRecord}
        onCancel={() => setEditRecordOpen(false)}
        confirmLoading={editSubmitting}
        okText="保存" cancelText="取消"
        width={480}
      >
        <Form form={editRecordForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="关联积分项目（可选）" name="pointItemId">
            <Select allowClear placeholder="选择积分项目（可不选）" options={pointItemOptions} />
          </Form.Item>
          <Form.Item label="分值" name="score" rules={[{ required: true, message: '请输入分值' }]}>
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="原因" name="reason" rules={[{ required: true, message: '请填写原因' }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="发生时间" name="occurredAt">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  )
}

export default PointsTable
