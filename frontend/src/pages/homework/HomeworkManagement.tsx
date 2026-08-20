import React, { useEffect, useState, useCallback } from 'react'
import {
  Table, Button, Modal, Form, Input, InputNumber, Select, DatePicker, Tag, Space, message, Popconfirm, Segmented, Upload
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd'
import {
  PlusOutlined, EditOutlined, SendOutlined, StopOutlined, DeleteOutlined, InboxOutlined, DownloadOutlined
} from '@ant-design/icons'
import PageContainer from '../../components/PageContainer'
import {
  listAssignments, createAssignment, updateAssignment,
  publishAssignment, closeAssignment, deleteAssignment, batchDeleteAssignments,
  listAssignmentFiles, deleteAssignmentFile, uploadAssignmentFiles, getAssignmentFileDownloadUrl
} from '../../api/homework'
import { getCurrentUser } from '../../utils/auth'
import { downloadFile } from '../../utils/download'
import type { HomeworkAssignment, HomeworkAssignmentForm, AssignmentFileInfo } from '../../types/homework'
import { getCohorts } from '../../api/cohort'
import type { Cohort } from '../../types/cohort'
import CohortSelect from '../../components/CohortSelect'
import { cohortLabel } from '../../utils/cohort'
import dayjs from 'dayjs'

const { TextArea } = Input

const HomeworkManagement: React.FC = () => {
  const [assignments, setAssignments] = useState<HomeworkAssignment[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [cohorts, setCohorts] = useState<Cohort[]>([])
  const [activeCohort, setActiveCohort] = useState<string>('all')
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [existingFiles, setExistingFiles] = useState<AssignmentFileInfo[]>([])
  const [form] = Form.useForm()

  const currentUser = getCurrentUser()
  const isMinister = currentUser?.position === '部长' && !currentUser?.fullAccess
  const ministerDept = currentUser?.department || ''
  const cohortId = activeCohort === 'all' ? undefined : Number(activeCohort)

  const fetchAssignments = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listAssignments({ page: 1, size: 100, cohortId })
      setAssignments(data.list)
    } catch { /* handled */ } finally { setLoading(false) }
  }, [cohortId])

  useEffect(() => { fetchAssignments() }, [fetchAssignments])

  useEffect(() => { getCohorts().then(setCohorts) }, [])

  const handleCreate = () => {
    setEditingId(null)
    form.resetFields()
    setFileList([])
    setExistingFiles([])
    // 部长默认设置自己部门
    if (isMinister) {
      form.setFieldsValue({ targetType: 'DEPARTMENT', targetDepartment: ministerDept })
    } else {
      form.setFieldsValue({ targetType: 'ALL' })
    }
    if (activeCohort !== 'all') {
      form.setFieldsValue({ cohortId: Number(activeCohort) })
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
      cohortId: item.cohortId ?? undefined,
      deadline: dayjs(item.deadline),
      maxPoints: item.maxPoints,
    })
    setFileList([])
    setExistingFiles([])
    setModalOpen(true)
    listAssignmentFiles(item.id).then(setExistingFiles).catch(() => setExistingFiles([]))
  }

  const handleDeleteExistingFile = async (file: AssignmentFileInfo) => {
    if (!editingId) return
    try {
      await deleteAssignmentFile(editingId, file.fileId)
      message.success('附件已删除')
      setExistingFiles(prev => prev.filter(f => f.fileId !== file.fileId))
    } catch (e: unknown) {
      message.error((e as Error)?.message || '删除失败')
    }
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
        cohortId: values.cohortId,
        deadline: values.deadline.toISOString(),
        maxPoints: values.maxPoints,
      }
      const newFiles = fileList.filter(f => f.originFileObj).map(f => f.originFileObj!)
      if (editingId) {
        await updateAssignment(editingId, data)
        if (newFiles.length > 0) {
          try {
            await uploadAssignmentFiles(editingId, newFiles)
          } catch {
            message.warning('作业已更新，但部分附件上传失败，请重新上传')
          }
        }
        message.success('作业已更新')
      } else {
        const created = await createAssignment(data)
        if (newFiles.length > 0) {
          try {
            await uploadAssignmentFiles(created.id, newFiles)
          } catch {
            message.warning('作业已创建，但有附件上传失败，请进入编辑页面重新上传')
          }
        }
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

  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) return
    try {
      await batchDeleteAssignments(selectedRowKeys.map(Number))
      message.success(`已批量删除 ${selectedRowKeys.length} 个作业`)
      setSelectedRowKeys([])
      fetchAssignments()
    } catch (e: unknown) {
      message.error((e as Error)?.message || '批量删除失败')
    }
  }

  const statusMap: Record<string, { color: string; text: string }> = {
    DRAFT: { color: 'default', text: '草稿' },
    PUBLISHED: { color: 'blue', text: '已发布' },
    CLOSED: { color: 'red', text: '已关闭' },
  }

  const columns: ColumnsType<HomeworkAssignment> = [
    { title: '标题', dataIndex: 'title', key: 'title' },
    { title: '届次', dataIndex: 'cohortYear', key: 'cohort', width: 90,
      render: (v: number | null | undefined) => cohortLabel(v) },
    { title: '目标范围', key: 'target', width: 120,
      render: (_: unknown, r: HomeworkAssignment) =>
        r.targetType === 'ALL' ? '全体成员' : r.targetDepartment || '-' },
    { title: '截止时间', dataIndex: 'deadline', width: 160,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm') },
    { title: '最大积分', dataIndex: 'maxPoints', width: 100,
      render: (v: number | undefined) => v != null ? `${v} 分` : '不限' },
    { title: '提交/已批', key: 'counts', width: 100,
      render: (_: unknown, r: HomeworkAssignment) =>
        `${r.submissionCount ?? 0} / ${r.gradedCount ?? 0}` },
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
          <Popconfirm
            title={r.gradedCount > 0 ? '该作业已有批改记录，删除会同时收回成员积分，确定删除？' : '确定删除该作业？'}
            onConfirm={() => handleDelete(r.id)}
            okText="删除" cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      )},
  ]

  return (
    <PageContainer title="作业管理">
      <Segmented
        value={activeCohort}
        options={[
          { label: '全部', value: 'all' },
          ...cohorts.map((c) => ({ label: `${c.year}届`, value: String(c.id) })),
        ]}
        onChange={(v) => setActiveCohort(v as string)}
        style={{ marginBottom: 16 }}
      />
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>创建作业</Button>
          <Popconfirm
            title={`确定删除选中的 ${selectedRowKeys.length} 个作业？`}
            onConfirm={handleBatchDelete}
            okText="删除" cancelText="取消"
            disabled={selectedRowKeys.length === 0}
          >
            <Button danger icon={<DeleteOutlined />} disabled={selectedRowKeys.length === 0}>批量删除</Button>
          </Popconfirm>
        </Space>
      </div>
      <Table
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys),
        }}
        columns={columns} dataSource={assignments} rowKey="id"
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
          <Form.Item label="作业附件（选填）">
            {editingId && existingFiles.length > 0 && (
              <div style={{ marginBottom: 8 }}>
                {existingFiles.map(f => (
                  <div key={f.fileId} style={{ display: 'flex', alignItems: 'center' }}>
                    <Button type="link" size="small" icon={<DownloadOutlined />}
                      onClick={() => downloadFile(getAssignmentFileDownloadUrl(editingId, f.fileId), f.originalName)}>
                      {f.originalName}
                    </Button>
                    <Button type="link" size="small" danger icon={<DeleteOutlined />}
                      onClick={() => handleDeleteExistingFile(f)}>删除</Button>
                  </div>
                ))}
              </div>
            )}
            <Upload.Dragger
              multiple
              fileList={fileList}
              beforeUpload={() => false}
              onChange={({ fileList: fl }) => setFileList(fl)}
              maxCount={10}
            >
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">点击或拖拽文件到此处上传</p>
              <p className="ant-upload-hint">支持 PDF、Word、PPT、Excel、文本、ZIP 等格式，可不上传</p>
            </Upload.Dragger>
          </Form.Item>
          <Form.Item name="cohortId" label="届次" rules={[{ required: true, message: '请选择届次' }]}>
            <CohortSelect placeholder="请选择届次" />
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
                <Select.Option value="外联部">外联部</Select.Option>
                <Select.Option value="宣策部">宣策部</Select.Option>
                <Select.Option value="组织部">组织部</Select.Option>
                <Select.Option value="秘书处">秘书处</Select.Option>
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
          </Space>
        </Form>
      </Modal>
    </PageContainer>
  )
}

export default HomeworkManagement
