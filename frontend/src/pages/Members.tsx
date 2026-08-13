import React, { useEffect, useState, useCallback } from 'react'
import {
  Table, Button, Input, Space, Modal, Form, Select,
  Popconfirm, message, Tag,
} from 'antd'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import PageContainer from '../components/PageContainer'
import { getMembers, createMember, updateMember, deleteMember } from '../api/member'
import type { Member, MemberForm } from '../types/member'
import { canManage, isAdmin } from '../utils/permission'

const POSITION_OPTIONS = ['社员', '部长', '会长', '副会长']
const DEPARTMENT_OPTIONS = ['秘书处', '技术部', '宣传部', '运营部', '其他']

const Members: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<Member[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editingMember, setEditingMember] = useState<Member | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<MemberForm>()

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getMembers({ keyword, page, size: pageSize })
      setData(res.list ?? [])
      setTotal(res.total ?? 0)
    } finally {
      setLoading(false)
    }
  }, [keyword, page, pageSize])

  useEffect(() => { fetchData() }, [fetchData])

  const handleAdd = () => {
    setEditingMember(null)
    form.resetFields()
    setModalOpen(true)
  }

  const handleEdit = (record: Member) => {
    setEditingMember(record)
    form.setFieldsValue(record)
    setModalOpen(true)
  }

  const handleDelete = async (id: number) => {
    await deleteMember(id)
    message.success('删除成功')
    fetchData()
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      if (editingMember) {
        await updateMember(editingMember.id, values)
        message.success('修改成功')
      } else {
        await createMember(values)
        message.success('新增成功')
      }
      setModalOpen(false)
      fetchData()
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<Member> = [
    { title: '姓名', dataIndex: 'name', width: 90 },
    { title: '学号', dataIndex: 'studentNo', width: 120 },
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
            title="确认删除该成员？"
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

  return (
    <PageContainer
      title="成员管理"
      extra={
        canManage() && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增成员
          </Button>
        )
      }
    >
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
        scroll={{ x: 800 }}
        pagination={{
          current: page,
          pageSize,
          total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => setPage(p),
        }}
      />

      <Modal
        title={editingMember ? '编辑成员' : '新增成员'}
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
    </PageContainer>
  )
}

export default Members
