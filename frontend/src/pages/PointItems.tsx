import React, { useEffect, useState, useCallback } from 'react'
import {
  Table, Button, Space, Modal, Form, Input, InputNumber,
  Select, Switch, Popconfirm, message, Tag, Alert,
} from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import PageContainer from '../components/PageContainer'
import {
  getPointItems, createPointItem, updatePointItem, deletePointItem,
} from '../api/point'
import type { PointItem } from '../types/point'
import { canManage } from '../utils/permission'

const ITEM_TYPE_OPTIONS = ['活动', '会议', '任务', '其他']

const PointItems: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<PointItem[]>([])
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<PointItem | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<Partial<PointItem>>()

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getPointItems()
      setData(res ?? [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchData() }, [fetchData])

  const handleAdd = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ enabled: true, allowMemberApply: true, sortOrder: 0 })
    setModalOpen(true)
  }

  const handleEdit = (record: PointItem) => {
    setEditing(record)
    form.setFieldsValue(record)
    setModalOpen(true)
  }

  const handleDelete = async (id: number) => {
    await deletePointItem(id)
    message.success('删除成功')
    fetchData()
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      if (editing) {
        await updatePointItem(editing.id, values)
        message.success('修改成功')
      } else {
        await createPointItem(values)
        message.success('新增成功')
      }
      setModalOpen(false)
      fetchData()
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<PointItem> = [
    { title: '项目名称', dataIndex: 'itemName', width: 140 },
    {
      title: '分值', dataIndex: 'pointValue', width: 80,
      render: (v: number) => (
        <Tag color={v > 0 ? 'green' : v < 0 ? 'red' : 'default'}>{v > 0 ? `+${v}` : v}</Tag>
      )
    },
    { title: '类型', dataIndex: 'itemType', width: 80 },
    { title: '说明', dataIndex: 'description', ellipsis: true },
    { title: '顺序', dataIndex: 'sortOrder', width: 70 },
    {
      title: '是否启用', dataIndex: 'enabled', width: 90,
      render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? '启用' : '停用'}</Tag>
    },
    {
      title: '允许登记', dataIndex: 'allowMemberApply', width: 90,
      render: (v: boolean) => <Tag color={v ? 'blue' : 'default'}>{v ? '允许' : '不允许'}</Tag>
    },
    canManage() ? {
      title: '操作', width: 140,
      render: (_: unknown, record: PointItem) => (
        <Space>
          <Button size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm
            title="确认删除该积分项目？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除" cancelText="取消"
          >
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    } : { title: '操作', width: 60, render: () => '—' },
  ]

  return (
    <PageContainer
      title="积分项目管理"
      extra={
        canManage() && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增项目
          </Button>
        )
      }
    >
      {!canManage() && (
        <Alert
          message="当前身份无管理权限，只能查看积分项目"
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        scroll={{ x: 800 }}
        pagination={false}
      />

      <Modal
        title={editing ? '编辑积分项目' : '新增积分项目'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        okText="保存" cancelText="取消"
        width={480}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            label="项目名称" name="itemName"
            rules={[{ required: true, message: '请输入项目名称' }]}
          >
            <Input placeholder="例：开源分享会" />
          </Form.Item>
          <Form.Item
            label="积分值" name="pointValue"
            rules={[{ required: true, message: '请输入积分值' }]}
          >
            <InputNumber placeholder="正数加分，负数扣分" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="类型" name="itemType">
            <Select
              placeholder="请选择类型"
              options={ITEM_TYPE_OPTIONS.map((t) => ({ label: t, value: t }))}
              allowClear
            />
          </Form.Item>
          <Form.Item label="说明" name="description">
            <Input.TextArea rows={2} placeholder="积分项目说明" />
          </Form.Item>
          <Form.Item label="显示顺序" name="sortOrder">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="是否启用" name="enabled" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
          <Form.Item label="允许成员登记" name="allowMemberApply" valuePropName="checked">
            <Switch checkedChildren="允许" unCheckedChildren="不允许" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  )
}

export default PointItems
