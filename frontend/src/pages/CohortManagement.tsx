import React, { useEffect, useState } from 'react'
import { Button, Table, Modal, Form, InputNumber, Switch, Space, Popconfirm, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import PageContainer from '../components/PageContainer'
import { getCohorts, createCohort, updateCohort, deleteCohort } from '../api/cohort'
import type { Cohort } from '../types/cohort'
import { isFullAccess } from '../utils/auth'

const CohortManagement: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<Cohort[]>([])
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Cohort | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<{ year: number; enabled: boolean }>()

  const load = async () => {
    setLoading(true)
    try {
      setData(await getCohorts())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ enabled: true })
    setModalOpen(true)
  }

  const openEdit = (record: Cohort) => {
    setEditing(record)
    form.setFieldsValue({ year: record.year, enabled: record.enabled })
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      if (editing) {
        await updateCohort(editing.id, values)
        message.success('届次已更新')
      } else {
        await createCohort(values)
        message.success('届次已创建')
      }
      setModalOpen(false)
      await load()
    } finally {
      setSubmitting(false)
    }
  }

  const handleToggle = async (record: Cohort, enabled: boolean) => {
    await updateCohort(record.id, { enabled })
    message.success(enabled ? '已启用' : '已停用')
    await load()
  }

  const handleDelete = async (id: number) => {
    await deleteCohort(id)
    message.success('届次已删除')
    await load()
  }

  if (!isFullAccess()) {
    return (
      <PageContainer title="届次管理">
        <div style={{ padding: 24 }}>无权限访问该页面。</div>
      </PageContainer>
    )
  }

  return (
    <PageContainer
      title="届次管理"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增届次
        </Button>
      }
    >
      <Table<Cohort>
        rowKey="id"
        loading={loading}
        dataSource={data}
        pagination={false}
        columns={[
          { title: '届次', dataIndex: 'year', render: (y: number) => `${y}届` },
          {
            title: '启用',
            dataIndex: 'enabled',
            render: (v: boolean, record: Cohort) => (
              <Switch checked={v} onChange={(checked) => handleToggle(record, checked)} />
            ),
          },
          {
            title: '操作',
            render: (_, record: Cohort) => (
              <Space>
                <a onClick={() => openEdit(record)}>编辑</a>
                <Popconfirm
                  title="确定删除该届次？被成员或作业引用的届次无法删除"
                  onConfirm={() => handleDelete(record.id)}
                >
                  <a style={{ color: '#ff4d4f' }}>删除</a>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? '编辑届次' : '新增届次'}
        open={modalOpen}
        onOk={handleSubmit}
        confirmLoading={submitting}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 12 }}>
          <Form.Item name="year" label="届次年份" rules={[{ required: true, message: '请输入年份' }]}>
            <InputNumber style={{ width: '100%' }} min={2000} max={2099} precision={0} placeholder="例如 2027" />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  )
}

export default CohortManagement
