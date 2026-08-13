import React, { useEffect, useState, useCallback, useRef } from 'react'
import {
  Table, Input, Button, Space, Tag, Modal, Form,
  InputNumber, Select, DatePicker, Popconfirm, message,
  Typography, Card, Alert,
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
  getMemberPointRecords, createMemberPointRecord,
  updatePointRecord, deletePointRecord,
  getPointItems,
} from '../api/point'
import type {
  PointsTableColumn, PointsTableRow, SearchPositionResponse,
  PointRecord, PointRecordForm,
} from '../types/point'
import type { PointItem } from '../types/point'
import { canManage } from '../utils/permission'

const { Text } = Typography

const PointsTable: React.FC = () => {
  // ─── 积分总表 ────────────────────────────────────────────
  const [loading, setLoading] = useState(false)
  const [columns, setColumns] = useState<PointsTableColumn[]>([])
  const [rows, setRows] = useState<PointsTableRow[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(50)
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')

  // ─── 搜索定位 ────────────────────────────────────────────
  const [locateKeyword, setLocateKeyword] = useState('')
  const [locateInput, setLocateInput] = useState('')
  const [locateResult, setLocateResult] = useState<SearchPositionResponse | null>(null)
  const [matchIndex, setMatchIndex] = useState(0)
  const [highlightedMemberId, setHighlightedMemberId] = useState<number | null>(null)
  const [locating, setLocating] = useState(false)

  // ─── 积分明细 Modal ──────────────────────────────────────
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailMember, setDetailMember] = useState<PointsTableRow | null>(null)
  const [detailRecords, setDetailRecords] = useState<PointRecord[]>([])
  const [detailLoading, setDetailLoading] = useState(false)
  const [pointItems, setPointItems] = useState<PointItem[]>([])

  // ─── 新增积分 Modal ──────────────────────────────────────
  const [addPointOpen, setAddPointOpen] = useState(false)
  const [addPointMember, setAddPointMember] = useState<PointsTableRow | null>(null)
  const [addPointForm] = Form.useForm<PointRecordForm>()
  const [addSubmitting, setAddSubmitting] = useState(false)

  // ─── 编辑积分记录 Modal ───────────────────────────────────
  const [editRecordOpen, setEditRecordOpen] = useState(false)
  const [editingRecord, setEditingRecord] = useState<PointRecord | null>(null)
  const [editRecordForm] = Form.useForm<PointRecordForm>()
  const [editSubmitting, setEditSubmitting] = useState(false)

  const fetchTable = useCallback(async (targetPage = page) => {
    setLoading(true)
    try {
      const res = await getPointsTable({ page: targetPage, size: pageSize, keyword })
      setColumns(res.columns ?? [])
      setRows(res.rows ?? [])
      setTotal(res.total ?? 0)
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, keyword])

  useEffect(() => { fetchTable() }, [fetchTable])

  useEffect(() => {
    getPointItems().then((res) => setPointItems(res ?? []))
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
  }, [locateKeyword, pageSize, page, fetchTable])

  const handleLocateSearch = () => {
    if (!locateInput.trim()) { message.warning('请输入搜索关键词'); return }
    setLocateKeyword(locateInput.trim())
    setMatchIndex(0)
  }

  useEffect(() => {
    if (locateKeyword) handleLocate(0)
  }, [locateKeyword]) // eslint-disable-line

  // ─── 查看明细 ────────────────────────────────────────────
  const openDetail = async (record: PointsTableRow) => {
    setDetailMember(record)
    setDetailOpen(true)
    setDetailLoading(true)
    try {
      const records = await getMemberPointRecords(record.memberId)
      setDetailRecords(records ?? [])
    } finally {
      setDetailLoading(false)
    }
  }

  const refreshDetail = async () => {
    if (!detailMember) return
    setDetailLoading(true)
    try {
      const records = await getMemberPointRecords(detailMember.memberId)
      setDetailRecords(records ?? [])
    } finally {
      setDetailLoading(false)
    }
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
  const openEditRecord = (record: PointRecord) => {
    setEditingRecord(record)
    editRecordForm.setFieldsValue({
      ...record,
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

  // ─── 构建动态列 ───────────────────────────────────────────
  const tableColumns: ColumnsType<PointsTableRow> = [
    {
      title: '排名', dataIndex: 'rankNo', width: 65, fixed: 'left',
      render: (v: number) => (
        <Text strong style={{ color: v <= 3 ? '#f50' : undefined }}>{v}</Text>
      )
    },
    { title: '姓名', dataIndex: 'name', width: 85, fixed: 'left' },
    { title: '学号', dataIndex: 'studentNo', width: 115 },
    { title: '联系电话', dataIndex: 'phone', width: 130 },
    { title: '专业', dataIndex: 'major', width: 120, ellipsis: true },
    { title: '部门', dataIndex: 'department', width: 90 },
    { title: '职务', dataIndex: 'position', width: 80 },
    ...columns.map((col) => ({
      title: (
        <div style={{ textAlign: 'center' as const }}>
          <div style={{ fontSize: 12, fontWeight: 600 }}>{col.itemName}</div>
          <div style={{ fontSize: 11, color: '#888' }}>+{col.pointValue}</div>
        </div>
      ),
      key: `col_${col.pointItemId}`,
      width: 90,
      align: 'center' as const,
      render: (_: unknown, row: PointsTableRow) => {
        const score = row.scores?.[String(col.pointItemId)] ?? 0
        return score > 0 ? <Tag color="blue">{score}</Tag> : <span style={{ color: '#ccc' }}>0</span>
      },
    })),
    {
      title: '总分', dataIndex: 'totalScore', width: 80, fixed: 'right',
      render: (v: number) => (
        <Text strong style={{ color: '#1677ff', fontSize: 16 }}>{v}</Text>
      )
    },
    {
      title: '操作', width: canManage() ? 170 : 90, fixed: 'right',
      render: (_: unknown, record: PointsTableRow) => (
        <Space size={4}>
          <Button size="small" icon={<EyeOutlined />} onClick={() => openDetail(record)}>
            明细
          </Button>
          {canManage() && (
            <Button
              size="small" type="primary" icon={<PlusOutlined />}
              onClick={() => openAddPoint(record)}
            >
              加分
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const detailColumns: ColumnsType<PointRecord> = [
    {
      title: '时间', dataIndex: 'occurredAt', width: 155,
      render: (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—'
    },
    { title: '积分项目', dataIndex: 'itemName', width: 130, render: (v?: string) => v || '手动录入' },
    {
      title: '分值', dataIndex: 'score', width: 80,
      render: (v: number) => (
        <Tag color={v > 0 ? 'green' : 'red'}>{v > 0 ? `+${v}` : v}</Tag>
      )
    },
    { title: '原因', dataIndex: 'reason', ellipsis: true },
    {
      title: '来源', dataIndex: 'sourceType', width: 90,
      render: (v: string) => v === 'APPLICATION' ? <Tag>活动登记</Tag> : <Tag color="orange">手动录入</Tag>
    },
    { title: '操作人', dataIndex: 'operatorName', width: 90 },
    canManage() ? {
      title: '操作', width: 120,
      render: (_: unknown, record: PointRecord) => (
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
    } : { title: '操作', width: 60, render: () => '—' },
  ]

  const pointItemOptions = pointItems.map((p) => ({ label: `${p.itemName}（+${p.pointValue}）`, value: p.id }))

  return (
    <PageContainer title="积分总表">
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

      {/* 积分明细 Modal */}
      <Modal
        title={detailMember ? `${detailMember.name} 的积分明细` : '积分明细'}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={900}
      >
        {canManage() && detailMember && (
          <Button
            type="primary" icon={<PlusOutlined />} size="small"
            style={{ marginBottom: 12 }}
            onClick={() => openAddPoint(detailMember)}
          >
            手动新增积分
          </Button>
        )}
        <Table
          rowKey="id"
          columns={detailColumns}
          dataSource={detailRecords}
          loading={detailLoading}
          pagination={{ pageSize: 10 }}
          scroll={{ x: 700 }}
          size="small"
        />
      </Modal>

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
