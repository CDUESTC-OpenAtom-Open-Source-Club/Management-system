import React, { useEffect, useState, useCallback } from 'react'
import {
  Table, Button, Modal, Form, Input, InputNumber, Select, DatePicker, Tag, Space, message, Popconfirm
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  PlusOutlined, EditOutlined, SendOutlined, StopOutlined, DeleteOutlined
} from '@ant-design/icons'
import PageContainer from '../../components/PageContainer'
import {
  listAssignments, createAssignment, updateAssignment,
  publishAssignment, closeAssignment, deleteAssignment
} from '../../api/homework'
import { getCurrentUser } from '../../utils/auth'
import type { HomeworkAssignment, HomeworkAssignmentForm } from '../../types/homework'
import type { PointItem } from '../../types/point'
import { getPointItems } from '../../api/point'
import dayjs from 'dayjs'

const { TextArea } = Input

const HomeworkManagement: React.FC = () => {
  const [assignments, setAssignments] = useState<HomeworkAssignment[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [pointItems, setPointItems] = useState<PointItem[]>([])
  const [form] = Form.useForm()

  const currentUser = getCurrentUser()
  const isMinister = currentUser?.position === '部长' && !currentUser?.fullAccess
  const ministerDept = currentUser?.department || ''

  const fetchAssignments = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listAssignments(undefined, 1, 100)
      setAssignments(data.list)
    } catch { /* handled */ } finally { setLoading(false) }
  }, [])

  const fetchPointItems = useCallback(async () => {
    try {
      const items = await getPointItems()
      setPointItems(items)
    } catch { /* handled */ }
  }, [])

  useEffect(() => { fetchAssignments(); fetchPointItems() }, [fetchAssignments, fetchPointItems])

  const handleCreate = () => {
    setEditingId(null)
    form.resetFields()
    // 部长默认设置自己部门
    if (isMinister) {
      form.setFieldsValue({ targetType: 'DEPARTMENT', targetDepartment: ministerDept })
    } else {
      form.setFieldsValue({ targetType: 'ALL' })
    }
    setModalOpen(true)
  }

  const handleEdit = (item: HomeworkAssignment) => {
    setEditingId(item.id)
    form.setFieldsValue({
      title: item.title,
      description: item.description || '',
      targetType: item.targetType,
      targetDepartment: item.targetDepartment || undefined,
      deadline: dayjs(item.deadline),
      maxPoints: item.maxPoints,
      pointItemId: item.pointItemId || undefined,
    })
    setModalOpen(true)
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      const data: HomeworkAssignmentForm = {
        title: values.title,
        description: values.description || '',
        targetType: values.targetType,
        targetDepartment: values.targetDepartment || undefined,
        deadline: values.deadline.toISOString(),
        maxPoints: values.maxPoints,
        pointItemId: values.pointItemId || undefined,
      }
      if (editingId) {
        await updateAssignment(editingId, data)
        message.success('作业已更新')
      } else {
        await createAssignment(data)
        message.success('作业已创建')
      }
      setModalOpen(false)
      fetchAssignments()
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'errorFields' in e) return
      message.error((e as Error)?.message || '操作失败')
    } finally { setSaving(false) }
  }

  const handlePublish = async (id: number) => {
    try {
      await publishAssignment(id)
      message.success('作业已发布')
      fetchAssignments()
    } catch (e: unknown) {
      message.error((e as Error)?.message || '发布失败')
    }
  }

  const handleClose = async (id: number) => {
    try {
      await closeAssignment(id)
      message.success('作业已关闭')
      fetchAssignments()
    } catch (e: unknown) {
      message.error((e as Error)?.message || '关闭失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteAssignment(id)
      message.success('作业已删除')
      fetchAssignments()
    } catch (e: unknown) {
      message.error((e as Error)?.message || '删除失败')
    }
  }

  const statusMap: Record<string, { color: string; text: string }> = {
    DRAFT: { color: 'default', text: '草稿' },
    PUBLISHED: { color: 'blue', text: '已发布' },
    CLOSED: { color: 'red', text: '已关闭' },
  }

  const columns: ColumnsType<HomeworkAssignment> = [
    { title: '标题', dataIndex: 'title', key: 'title' },
    { title: '目标范围', key: 'target', width: 120,
      render: (_: unknown, r: HomeworkAssignment) =>
        r.targetType === 'ALL' ? '全体成员' : r.targetDepartment || '-' },
    { title: '截止时间', dataIndex: 'deadline', width: 160,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm') },
    { title: '最大积分', dataIndex: 'maxPoints', width: 100,
      render: (v: number | undefined) => v != null ? `${v} 分` : '不限' },
    { title: '积分归属', dataIndex: 'pointItemName', width: 120, render: (v: string) => v || '-' },
    { title: '提交/已批', key: 'counts', width: 100,
      render: (_: unknown, r: HomeworkAssignment) =>
        `${r.submittedCount ?? 0} / ${r.gradedCount ?? 0}` },
    { title: '状态', dataIndex: 'status', width: 90,
      render: (v: string) => {
        const cfg = statusMap[v] || { color: 'default', text: v }
        return <Tag color={cfg.color}>{cfg.text}</Tag>
      }},
    { title: '操作', key: 'actions', width: 220, fixed: 'right',
      render: (_: unknown, r: HomeworkAssignment) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />}
            onClick={() => handleEdit(r)}>编辑</Button>
          {r.status === 'DRAFT' && (
            <Button type="link" size="small" icon={<SendOutlined />}
              onClick={() => handlePublish(r.id)}>发布</Button>
          )}
          {r.status === 'PUBLISHED' && (
            <Button type="link" size="small" icon={<StopOutlined />}
              onClick={() => handleClose(r.id)}>关闭</Button>
          )}
          {r.gradedCount === 0 && (
            <Popconfirm title="确定删除该作业？" onConfirm={() => handleDelete(r.id)}>
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          )}
        </Space>
      )},
  ]

  return (
    <PageContainer title="作业管理">
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>创建作业</Button>
      </div>
      <Table columns={columns} dataSource={assignments} rowKey="id"
        loading={loading} pagination={false} scroll={{ x: 1000 }} />

      {/* 创建/编辑 Modal */}
      <Modal title={editingId ? '编辑作业' : '创建作业'} open={modalOpen}
        onCancel={() => setModalOpen(false)} onOk={handleSave}
        confirmLoading={saving} okText="保存" width={640} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
          <Form.Item name="title" label="作业标题"
            rules={[{ required: true, message: '请输入作业标题' }]}>
            <Input placeholder="输入作业标题" maxLength={200} />
          </Form.Item>
          <Form.Item name="description" label="作业要求">
            <TextArea rows={4} placeholder="输入作业详细说明" maxLength={5000} showCount />
          </Form.Item>
          <Space size="large">
            <Form.Item name="targetType" label="目标范围"
              rules={[{ required: true, message: '请选择目标范围' }]}>
              <Select style={{ width: 140 }} disabled={isMinister}
                onChange={(val) => {
                  if (val === 'ALL') form.setFieldsValue({ targetDepartment: undefined })
                }}>
                <Select.Option value="ALL">全体成员</Select.Option>
                <Select.Option value="DEPARTMENT">指定部门</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item name="targetDepartment" label="目标部门"
              rules={[({ getFieldValue }) => ({
                validator: (_, value) => {
                  if (getFieldValue('targetType') === 'DEPARTMENT' && !value) {
                    return Promise.reject(new Error('请选择目标部门'))
                  }
                  return Promise.resolve()
                }
              })]}>
              <Select style={{ width: 140 }} placeholder="选择部门"
                disabled={isMinister}
                allowClear={form.getFieldValue('targetType') !== 'DEPARTMENT'}>
                <Select.Option value="技术部">技术部</Select.Option>
                <Select.Option value="宣传部">宣传部</Select.Option>
                <Select.Option value="秘书处">秘书处</Select.Option>
                <Select.Option value="组织部">组织部</Select.Option>
              </Select>
            </Form.Item>
          </Space>
          <Space size="large">
            <Form.Item name="deadline" label="截止时间"
              rules={[{ required: true, message: '请选择截止时间' }]}>
              <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item name="maxPoints" label="最大积分">
              <InputNumber min={0} precision={1} style={{ width: 120 }} placeholder="不限" />
            </Form.Item>
            <Form.Item name="pointItemId" label="积分归属">
              <Select style={{ width: 180 }} placeholder="选择积分项目" allowClear>
                {pointItems.map(pi => (
                  <Select.Option key={pi.id} value={pi.id}>{pi.itemName}</Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </PageContainer>
  )
}

export default HomeworkManagement
