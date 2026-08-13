import React, { useEffect, useMemo, useState } from 'react'
import { Button, Card, Drawer, Form, Input, message, Modal, Popconfirm, Space, Switch, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { DeleteOutlined, FileAddOutlined, LockOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { batchCreateUsers, createUser, deleteUser, getUsers, resetUserPassword, updateUserEnabled } from '../api/user'
import type { BatchCreateUsersResponse, UserAccountResponse } from '../api/auth'
import { isFullAccess } from '../utils/auth'

const { Text } = Typography
const PAGE_SIZE_DEFAULT = 10

const Users: React.FC = () => {
  const [form] = Form.useForm()
  const [batchForm] = Form.useForm()
  const [resetForm] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [records, setRecords] = useState<UserAccountResponse[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(PAGE_SIZE_DEFAULT)
  const [keyword, setKeyword] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [batchOpen, setBatchOpen] = useState(false)
  const [resetTarget, setResetTarget] = useState<UserAccountResponse | null>(null)
  const [batchResult, setBatchResult] = useState<BatchCreateUsersResponse | null>(null)
  const [createSubmitting, setCreateSubmitting] = useState(false)
  const [batchSubmitting, setBatchSubmitting] = useState(false)
  const canView = isFullAccess()

  const loadData = async (p = page, s = size, kw = keyword) => {
    setLoading(true)
    try {
      const res = await getUsers({ page: p, size: s, keyword: kw || undefined })
      setRecords(res.list ?? [])
      setTotal(res.total ?? 0)
      setPage(res.page || p)
      setSize(res.size || s)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData(1, PAGE_SIZE_DEFAULT, '')
  }, [])

  const handleSearch = () => { void loadData(1, size, keyword.trim()) }
  const handleResetSearch = () => { setKeyword(''); void loadData(1, size, '') }

  const handleCreate = async (values: { username: string; initialPassword: string; enabled?: boolean }) => {
    setCreateSubmitting(true)
    try {
      await createUser(values)
      message.success('账号创建成功')
      setCreateOpen(false)
      form.resetFields()
      await loadData(1, size, keyword)
    } finally {
      setCreateSubmitting(false)
    }
  }

  const handleBatchCreate = async (values: { usernamesText: string; initialPassword: string }) => {
    const usernames = Array.from(new Set(
      values.usernamesText
        .split(/\r?\n/)
        .map((item) => item.trim())
        .filter(Boolean),
    ))
    if (!usernames.length) {
      message.error('请至少输入一个用户名')
      return
    }
    setBatchSubmitting(true)
    try {
      const res = await batchCreateUsers({
        accounts: usernames.map((username) => ({ username, initialPassword: values.initialPassword })),
      })
      setBatchResult(res)
      if (res.created.length > 0) {
        message.success(`批量创建完成：成功 ${res.created.length} 个，失败 ${res.failed.length} 个`)
        await loadData(1, size, keyword)
      } else {
        message.warning('批量创建完成，但没有成功创建账号')
      }
    } finally {
      setBatchSubmitting(false)
    }
  }

  const handleToggleEnabled = async (record: UserAccountResponse, enabled: boolean) => {
    if (enabled === false && record.username === 'lintao') {
      message.warning('当前用户不可被禁用')
      return
    }
    await updateUserEnabled(record.id, enabled)
    message.success('状态已更新')
    await loadData(page, size, keyword)
  }

  const handleResetPassword = async (values: { newPassword: string }) => {
    if (!resetTarget) return
    await resetUserPassword(resetTarget.id, values.newPassword)
    message.success('密码已重置')
    setResetTarget(null)
    resetForm.resetFields()
    await loadData(page, size, keyword)
  }

  const handleDelete = async (record: UserAccountResponse) => {
    await deleteUser(record.id)
    message.success('账号已删除')
    // 删除当前页最后一条且非第一页时，回退一页，避免停留在空页
    const nextPage = records.length === 1 && page > 1 ? page - 1 : page
    await loadData(nextPage, size, keyword)
  }

  const columns: ColumnsType<UserAccountResponse> = useMemo(() => [
    { title: '用户名', dataIndex: 'username', width: 140, fixed: 'left' },
    { title: '姓名', dataIndex: 'name', width: 120, render: (value) => value || '-' },
    { title: '学号', dataIndex: 'studentNo', width: 120, render: (value) => value || '-' },
    { title: '联系电话', dataIndex: 'phone', width: 140, render: (value) => value || '-' },
    { title: '专业', dataIndex: 'major', width: 140, render: (value) => value || '-' },
    { title: '部门', dataIndex: 'department', width: 120, render: (value) => value || '-' },
    { title: '职务', dataIndex: 'position', width: 120, render: (value) => value || '-' },
    { title: '资料状态', dataIndex: 'profileCompleted', width: 110, render: (value) => <Tag color={value ? 'green' : 'orange'}>{value ? '已完善' : '未完善'}</Tag> },
    { title: '初始密码', dataIndex: 'initialPasswordChanged', width: 110, render: (value) => <Tag color={value ? 'blue' : 'gold'}>{value ? '已修改' : '未修改'}</Tag> },
    { title: '启用状态', dataIndex: 'enabled', width: 100, render: (value) => <Tag color={value ? 'success' : 'default'}>{value ? '启用' : '禁用'}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', width: 180, render: (value) => value ? new Date(value).toLocaleString() : '-' },
    { title: '上次登录', dataIndex: 'lastLoginAt', width: 180, render: (value) => value ? new Date(value).toLocaleString() : '-' },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 280,
      render: (_, record) => (
        <Space size={8} wrap>
          <Switch checked={record.enabled} onChange={(checked) => void handleToggleEnabled(record, checked)} />
          <Button size="small" icon={<LockOutlined />} onClick={() => { setResetTarget(record); resetForm.setFieldsValue({ newPassword: '' }) }}>重置密码</Button>
          <Popconfirm title="确认删除该账号？" okText="删除" cancelText="取消" onConfirm={() => void handleDelete(record)}>
            <Button size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ], [page, size, keyword])

  if (!canView) {
    return <Card><Text>无权限访问该页面。</Text></Card>
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card>
        <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space wrap>
            <Input.Search
              allowClear
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={handleSearch}
              placeholder="搜索用户名 / 姓名 / 学号"
              style={{ width: 280 }}
            />
            <Button icon={<ReloadOutlined />} onClick={handleResetSearch}>重置</Button>
          </Space>
          <Space wrap>
            <Button icon={<FileAddOutlined />} onClick={() => setBatchOpen(true)}>批量创建</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>新建账号</Button>
          </Space>
        </Space>
      </Card>

      <Card>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={records}
          scroll={{ x: 1600 }}
          pagination={{
            current: page,
            pageSize: size,
            total,
            showSizeChanger: true,
            onChange: (nextPage, nextSize) => void loadData(nextPage, nextSize || PAGE_SIZE_DEFAULT, keyword),
          }}
        />
      </Card>

      <Modal title="新建账号" open={createOpen} onCancel={() => setCreateOpen(false)} footer={null} destroyOnClose>
        <Form form={form} layout="vertical" onFinish={handleCreate} initialValues={{ enabled: true }}>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input placeholder="用户名" />
          </Form.Item>
          <Form.Item name="initialPassword" label="初始密码" rules={[{ required: true, message: '请输入初始密码' }, { min: 6, message: '至少 6 位' }]}>
            <Input.Password placeholder="初始密码" />
          </Form.Item>
          <Form.Item name="enabled" label="是否启用" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={() => setCreateOpen(false)}>取消</Button>
            <Button type="primary" htmlType="submit" loading={createSubmitting}>创建</Button>
          </Space>
        </Form>
      </Modal>

      <Drawer title="批量创建账号" open={batchOpen} width={560} onClose={() => { setBatchOpen(false); setBatchResult(null); batchForm.resetFields() }} destroyOnClose>
        <Form form={batchForm} layout="vertical" onFinish={handleBatchCreate}>
          <Form.Item name="usernamesText" label="用户名列表" rules={[{ required: true, message: '请按行输入用户名' }]}>
            <Input.TextArea rows={8} placeholder="每行一个用户名" />
          </Form.Item>
          <Form.Item name="initialPassword" label="初始密码" rules={[{ required: true, message: '请输入初始密码' }, { min: 6, message: '至少 6 位' }]}>
            <Input.Password placeholder="管理员输入初始密码" />
          </Form.Item>
          <Space>
            <Button onClick={() => { setBatchOpen(false); batchForm.resetFields() }}>取消</Button>
            <Button type="primary" htmlType="submit" loading={batchSubmitting}>批量创建</Button>
          </Space>
        </Form>

        {batchResult && (
          <Card size="small" style={{ marginTop: 16 }} title="创建结果">
            <Text>成功 {batchResult.created.length} 个，失败 {batchResult.failed.length} 个。</Text>
            <div style={{ marginTop: 12 }}>
              {batchResult.created.map((item) => <Tag key={item.username} color="green">{item.username}</Tag>)}
              {batchResult.failed.map((item) => <Tag key={`${item.username}-${item.reason}`} color="red">{item.username}：{item.reason}</Tag>)}
            </div>
          </Card>
        )}
      </Drawer>

      <Modal
        title={`重置密码${resetTarget ? ` - ${resetTarget.username}` : ''}`}
        open={!!resetTarget}
        onCancel={() => setResetTarget(null)}
        footer={null}
        destroyOnClose
      >
        <Form form={resetForm} layout="vertical" onFinish={handleResetPassword}>
          <Form.Item name="newPassword" label="新密码" rules={[{ required: true, message: '请输入新密码' }, { min: 6, message: '至少 6 位' }]}>
            <Input.Password placeholder="请输入新密码" />
          </Form.Item>
          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={() => setResetTarget(null)}>取消</Button>
            <Button type="primary" htmlType="submit">确认重置</Button>
          </Space>
        </Form>
      </Modal>
    </Space>
  )
}

export default Users
