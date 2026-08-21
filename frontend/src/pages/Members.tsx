import React, { useEffect, useState, useCallback } from 'react'
import {
  Table, Button, Input, Space, Modal, Form, Select,
  Popconfirm, message, Tag, Segmented, Empty,
} from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import PageContainer from '../components/PageContainer'
import CohortSelect from '../components/CohortSelect'
import { getMembers, updateMember, deleteMember, batchSetCohort, batchDeleteMembers } from '../api/member'
import { getCohorts } from '../api/cohort'
import type { Member, MemberForm } from '../types/member'
import type { Cohort } from '../types/cohort'
import { cohortLabel } from '../utils/cohort'
import { canManage, isAdmin } from '../utils/permission'
import { getCurrentUser } from '../utils/auth'

const POSITION_OPTIONS = ['社员', '部长', '会长', '副会长']
const DEPARTMENT_OPTIONS = ['秘书处', '技术部', '外联部', '宣策部', '组织部', '其他']
const UNASSIGNED = 'unassigned'

const Members: React.FC = () => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<Member[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [cohorts, setCohorts] = useState<Cohort[]>([])
  const [activeCohort, setActiveCohort] = useState<string>('all')
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [batchCohortOpen, setBatchCohortOpen] = useState(false)
  const [batchCohortId, setBatchCohortId] = useState<number | null>(null)
  const [batchSubmitting, setBatchSubmitting] = useState(false)
  const [batchDeleteOpen, setBatchDeleteOpen] = useState(false)
  const [batchDeleting, setBatchDeleting] = useState(false)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingMember, setEditingMember] = useState<Member | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<MemberForm>()

  useEffect(() => {
    getCohorts().then((list) => {
      setCohorts(list)
      const latest = list.find((c) => c.enabled)
      setActiveCohort(latest ? String(latest.id) : 'all')
    })
  }, [])

  const cohortId = activeCohort === 'all' ? undefined : activeCohort === UNASSIGNED ? -1 : Number(activeCohort)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getMembers({ keyword, cohortId, page, size: pageSize })
      setData(res.list ?? [])
      setTotal(res.total ?? 0)
    } finally {
      setLoading(false)
    }
  }, [keyword, page, pageSize, cohortId])

  useEffect(() => { fetchData() }, [fetchData])

  // 删除后刷新列表，并在当前页超出最大页时自动回退，避免出现空页
  const refreshAfterDelete = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getMembers({ keyword, cohortId, page, size: pageSize })
      const maxPage = Math.max(1, Math.ceil((res.total ?? 0) / pageSize))
      if (page > maxPage) {
        setPage(maxPage)
      } else {
        setData(res.list ?? [])
        setTotal(res.total ?? 0)
      }
    } finally {
      setLoading(false)
    }
  }, [keyword, cohortId, page, pageSize])

  const handleEdit = (record: Member) => {
    setEditingMember(record)
    form.setFieldsValue(record)
    setModalOpen(true)
  }

  const handleDelete = async (id: number) => {
    const currentMemberId = getCurrentUser()?.memberId
    if (currentMemberId != null && id === currentMemberId) {
      message.error('不能删除当前登录账号对应的成员')
      return
    }
    await deleteMember(id)
    message.success('删除成功')
    setSelectedRowKeys((keys) => keys.filter((k) => k !== id))
    refreshAfterDelete()
  }

  const handleBatchDelete = async () => {
    const currentMemberId = getCurrentUser()?.memberId
    if (currentMemberId != null && selectedRowKeys.map(Number).includes(currentMemberId)) {
      message.error('不能删除当前登录账号对应的成员，请取消选择本人后重试')
      return
    }
    setBatchDeleting(true)
    try {
      const result = await batchDeleteMembers(selectedRowKeys.map(Number))
      message.success(`成功删除 ${result.deletedMemberCount} 名成员及其对应积分数据`)
      setBatchDeleteOpen(false)
      setSelectedRowKeys([])
      refreshAfterDelete()
    } finally {
      setBatchDeleting(false)
    }
  }

  const handleBatchCohort = async () => {
    if (batchCohortId == null) return
    setBatchSubmitting(true)
    try {
      const n = await batchSetCohort(selectedRowKeys.map(Number), batchCohortId)
      message.success(`已为 ${n} 名成员设置届次`)
      setBatchCohortOpen(false)
      setSelectedRowKeys([])
      fetchData()
    } finally {
      setBatchSubmitting(false)
    }
  }

  const handleSubmit = async () => {
    if (!editingMember) return
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      await updateMember(editingMember.id, values)
      message.success('修改成功')
      setModalOpen(false)
      fetchData()
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<Member> = [
    { title: '姓名', dataIndex: 'name', width: 90 },
    { title: '学号', dataIndex: 'studentNo', width: 120 },
    { title: '届次', dataIndex: 'cohortYear', width: 90, render: (y) => cohortLabel(y) },
    { title: '联系电话', dataIndex: 'phone', width: 130 },
    { title: '专业', dataIndex: 'major', width: 120, ellipsis: true },
    { title: '部门', dataIndex: 'department', width: 100 },
    {
      title: '职务', dataIndex: 'position', width: 90,
      render: (pos: string) => {
        const colorMap: Record<string, string> = { 会长: 'red', 副会长: 'orange', 部长: 'blue', 社员: 'green' }
        return <Tag color={colorMap[pos] || 'default'}>{pos}</Tag>
      }
    },
    canManage() ? {
      title: '操作',
      width: 140,
      render: (_: unknown, record: Member) => (
        <Space>
          <Button size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm
            title={`确认删除成员“${record.name}”？`}
            description="删除后，该成员对应的积分申请和积分记录也会同步删除；若存在关联登录账号，该账号将被禁用。"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            cancelText="取消"
          >
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    } : { title: '操作', width: 60, render: () => '—' },
  ]

  const cohortSegments = [
    { label: '全部', value: 'all' },
    ...cohorts.map((c) => ({ label: `${c.year}届`, value: String(c.id) })),
    { label: '未分届', value: UNASSIGNED },
  ]

  const emptyState = (
    <Empty
      image={Empty.PRESENTED_IMAGE_SIMPLE}
      description={
        <div>
          <div>暂无成员</div>
          <div style={{ color: '#98a2b3', fontSize: 12, marginTop: 4 }}>成员档案将在创建账号时自动生成</div>
        </div>
      }
    >
      {canManage() && (
        <Button type="primary" onClick={() => navigate('/users')}>
          前往账号管理
        </Button>
      )}
    </Empty>
  )

  return (
    <PageContainer
      title="成员管理"
      extra={
        <Space>
          {canManage() && selectedRowKeys.length > 0 && (
            <Button onClick={() => setBatchCohortOpen(true)}>
              批量设置届次（{selectedRowKeys.length}）
            </Button>
          )}
          {canManage() && selectedRowKeys.length > 0 && (
            <Button danger onClick={() => setBatchDeleteOpen(true)}>
              批量删除（{selectedRowKeys.length}）
            </Button>
          )}
        </Space>
      }
    >
      <Segmented
        value={activeCohort}
        options={cohortSegments}
        onChange={(v) => { setActiveCohort(v as string); setPage(1) }}
        style={{ marginBottom: 16 }}
      />

      <Space style={{ marginBottom: 16 }}>
        <Input
          placeholder="搜索姓名/学号/专业/部门/职务"
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
        rowSelection={canManage() ? { selectedRowKeys, onChange: (keys) => setSelectedRowKeys(keys) } : undefined}
        scroll={{ x: 900 }}
        locale={{ emptyText: emptyState }}
        pagination={{
          current: page,
          pageSize,
          total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => setPage(p),
        }}
      />

      <Modal
        title="编辑成员"
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        okText="保存"
        cancelText="取消"
        width={480}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="姓名" name="name" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item label="学号" name="studentNo" rules={[{ required: true, message: '请输入学号' }]}>
            <Input placeholder="请输入学号" />
          </Form.Item>
          <Form.Item label="联系电话" name="phone">
            <Input placeholder="请输入联系电话" />
          </Form.Item>
          <Form.Item label="专业" name="major">
            <Input placeholder="请输入专业" />
          </Form.Item>
          <Form.Item label="届次" name="cohortId">
            <CohortSelect enabledOnly={false} placeholder="请选择届次" />
          </Form.Item>
          <Form.Item label="部门" name="department">
            <Select
              disabled={!isAdmin()}
              placeholder={isAdmin() ? '请选择部门' : '仅会长/副会长可修改'}
              options={DEPARTMENT_OPTIONS.map((d) => ({ label: d, value: d }))}
              allowClear
            />
          </Form.Item>
          <Form.Item label="职务" name="position">
            <Select
              disabled={!isAdmin()}
              placeholder={isAdmin() ? '请选择职务' : '仅会长/副会长可修改'}
              options={POSITION_OPTIONS.map((p) => ({ label: p, value: p }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="批量设置届次"
        open={batchCohortOpen}
        onOk={handleBatchCohort}
        confirmLoading={batchSubmitting}
        onCancel={() => setBatchCohortOpen(false)}
        okText="确定"
        cancelText="取消"
        okButtonProps={{ disabled: batchCohortId == null }}
      >
        <p style={{ marginBottom: 12 }}>已选择 {selectedRowKeys.length} 名成员，设置为：</p>
        <CohortSelect
          value={batchCohortId}
          onChange={(v) => setBatchCohortId(v)}
          placeholder="请选择届次"
          style={{ width: '100%' }}
        />
      </Modal>

      <Modal
        title="确认批量删除成员？"
        open={batchDeleteOpen}
        onOk={handleBatchDelete}
        onCancel={() => setBatchDeleteOpen(false)}
        confirmLoading={batchDeleting}
        okText="确认删除"
        cancelText="取消"
        okButtonProps={{ danger: true }}
      >
        <p>已选择 {selectedRowKeys.length} 名成员。</p>
        <p style={{ marginBottom: 8 }}>删除后：</p>
        <ul style={{ paddingLeft: 20, margin: 0 }}>
          <li>成员将从有效成员名单中移除；</li>
          <li>该成员对应的积分登记、积分申请和积分记录将同步删除；</li>
          <li>积分总表和排名将重新计算；</li>
          <li>若成员存在关联登录账号，对应账号将被禁用；</li>
          <li>其他成员数据不会受到影响。</li>
        </ul>
        <p style={{ marginTop: 12, color: '#ff4d4f' }}>此操作不可直接撤销，请谨慎确认。</p>
      </Modal>
    </PageContainer>
  )
}

export default Members
