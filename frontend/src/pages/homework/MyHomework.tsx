import React, { useEffect, useState, useCallback } from 'react'
import {
  Card, Button, Modal, Form, Input, Upload, message, Tag, Space, Descriptions, Empty, Spin, Table
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  UploadOutlined, EyeOutlined, EditOutlined, ClockCircleOutlined,
  CheckCircleOutlined, CloseCircleOutlined, ExclamationCircleOutlined
} from '@ant-design/icons'
import type { UploadFile } from 'antd'
import PageContainer from '../../components/PageContainer'
import { listMyHomework, submitHomework, getMySubmission, getDownloadFileUrl, getViewFileUrl } from '../../api/homework'
import { downloadFile, viewFile } from '../../utils/download'
import type { HomeworkAssignment, HomeworkSubmission } from '../../types/homework'
import dayjs from 'dayjs'

const { TextArea } = Input

const MyHomework: React.FC = () => {
  const [assignments, setAssignments] = useState<HomeworkAssignment[]>([])
  const [loading, setLoading] = useState(false)
  const [submitModalOpen, setSubmitModalOpen] = useState(false)
  const [detailModalOpen, setDetailModalOpen] = useState(false)
  const [currentAssignment, setCurrentAssignment] = useState<HomeworkAssignment | null>(null)
  const [mySubmission, setMySubmission] = useState<HomeworkSubmission | null>(null)
  const [submissionLoading, setSubmissionLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [form] = Form.useForm()

  const fetchAssignments = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listMyHomework()
      setAssignments(data)
    } catch {
      // error handled by interceptor
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchAssignments() }, [fetchAssignments])

  const handleViewDetail = async (item: HomeworkAssignment) => {
    setCurrentAssignment(item)
    setDetailModalOpen(true)
    setSubmissionLoading(true)
    try {
      const sub = await getMySubmission(item.id)
      setMySubmission(sub)
    } catch {
      setMySubmission(null)
    } finally {
      setSubmissionLoading(false)
    }
  }

  const handleOpenSubmit = (item: HomeworkAssignment) => {
    setCurrentAssignment(item)
    form.resetFields()
    setFileList([])
    setSubmitModalOpen(true)
    getMySubmission(item.id).then(sub => {
      setMySubmission(sub)
      form.setFieldsValue({ content: sub.content || '' })
    }).catch(() => { setMySubmission(null) })
  }

  const handleSubmit = async () => {
    if (!currentAssignment) return
    try {
      const values = await form.validateFields()
      setSubmitting(true)
      const files = fileList.filter(f => f.originFileObj).map(f => f.originFileObj!)
      await submitHomework(currentAssignment.id, values.content || '', files)
      message.success(mySubmission ? '作业已更新' : '作业已提交')
      setSubmitModalOpen(false)
      fetchAssignments()
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'errorFields' in e) return
      message.error((e as Error)?.message || '提交失败')
    } finally {
      setSubmitting(false)
    }
  }

  const getStatusTag = (item: HomeworkAssignment) => {
    if (item.submittedCount > 0 && item.gradedCount > 0) {
      return <Tag color="green" icon={<CheckCircleOutlined />}>已批改</Tag>
    }
    if (item.submittedCount > 0) {
      return <Tag color="blue" icon={<CheckCircleOutlined />}>已提交</Tag>
    }
    if (dayjs(item.deadline).isBefore(dayjs())) {
      return <Tag color="red" icon={<CloseCircleOutlined />}>已截止</Tag>
    }
    return <Tag color="orange" icon={<ExclamationCircleOutlined />}>未提交</Tag>
  }

  const canSubmit = (item: HomeworkAssignment) => {
    if (item.status === 'CLOSED') return false
    return dayjs(item.deadline).isAfter(dayjs())
  }

  const columns: ColumnsType<HomeworkAssignment> = [
    { title: '作业标题', dataIndex: 'title', key: 'title' },
    { title: '目标范围', key: 'target', width: 120,
      render: (_: unknown, r: HomeworkAssignment) =>
        r.targetType === 'ALL' ? '全体成员' : r.targetDepartment || '-' },
    { title: '截止时间', dataIndex: 'deadline', key: 'deadline', width: 180,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm') },
    { title: '最大积分', dataIndex: 'maxPoints', key: 'maxPoints', width: 100,
      render: (v: number | undefined) => v != null ? `${v} 分` : '-' },
    { title: '状态', key: 'status', width: 100, render: (_: unknown, r: HomeworkAssignment) => getStatusTag(r) },
    { title: '操作', key: 'actions', width: 200,
      render: (_: unknown, r: HomeworkAssignment) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(r)}>详情</Button>
          {canSubmit(r) && (
            <Button type="link" size="small" icon={<EditOutlined />}
              onClick={() => handleOpenSubmit(r)}>
              {r.submittedCount > 0 ? '重新提交' : '提交'}
            </Button>
          )}
        </Space>
      ) },
  ]

  return (
    <PageContainer title="我的作业">
      <Spin spinning={loading}>
        {assignments.length === 0 ? (
          <Empty description="暂无需要完成的作业" style={{ padding: 60 }} />
        ) : (
          <Table columns={columns} dataSource={assignments} rowKey="id" pagination={false}
            scroll={{ x: 800 }} />
        )}
      </Spin>

      {/* 查看详情 Modal */}
      <Modal title={currentAssignment?.title} open={detailModalOpen}
        onCancel={() => setDetailModalOpen(false)} footer={null} width={680} destroyOnClose>
        <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
          <Descriptions.Item label="目标范围">
            {currentAssignment?.targetType === 'ALL' ? '全体成员' : currentAssignment?.targetDepartment || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="截止时间">
            {currentAssignment?.deadline ? dayjs(currentAssignment.deadline).format('YYYY-MM-DD HH:mm') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="最大积分">{currentAssignment?.maxPoints != null ? `${currentAssignment.maxPoints} 分` : '不限'}</Descriptions.Item>
          <Descriptions.Item label="积分归属">{currentAssignment?.pointItemName || '-'}</Descriptions.Item>
          <Descriptions.Item label="作业要求" span={2}>
            <div style={{ whiteSpace: 'pre-wrap' }}>{currentAssignment?.description || '无'}</div>
          </Descriptions.Item>
        </Descriptions>

        <Card title="我的提交" size="small">
          <Spin spinning={submissionLoading}>
            {mySubmission ? (
              <>
                <Descriptions column={2} size="small" bordered>
                  <Descriptions.Item label="提交状态">
                    <Tag color={mySubmission.status === 'GRADED' ? 'green' : 'blue'}>
                      {mySubmission.status === 'GRADED' ? '已批改' : '已提交'}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="提交时间">
                    {dayjs(mySubmission.submittedAt).format('YYYY-MM-DD HH:mm')}
                  </Descriptions.Item>
                  {mySubmission.awardedPoints != null && (
                    <Descriptions.Item label="获得积分">{mySubmission.awardedPoints} 分</Descriptions.Item>
                  )}
                  {mySubmission.reviewedByName && (
                    <Descriptions.Item label="批改人">{mySubmission.reviewedByName}</Descriptions.Item>
                  )}
                  {mySubmission.reviewComment && (
                    <Descriptions.Item label="批改意见" span={2}>
                      {mySubmission.reviewComment}
                    </Descriptions.Item>
                  )}
                </Descriptions>
                {mySubmission.content && (
                  <div style={{ marginTop: 12 }}>
                    <strong>提交内容：</strong>
                    <div style={{ whiteSpace: 'pre-wrap', marginTop: 4 }}>{mySubmission.content}</div>
                  </div>
                )}
                {mySubmission.files && mySubmission.files.length > 0 && (
                  <div style={{ marginTop: 12 }}>
                    <strong>附件：</strong>
                    {mySubmission.files.map(f => (
                      <Space key={f.id} style={{ marginRight: 12, marginBottom: 4 }}>
                        <Button type="link" size="small" onClick={() =>
                          viewFile(getViewFileUrl(mySubmission.id, f.fileId))}>
                          {f.originalName}
                        </Button>
                        <Button type="link" size="small" onClick={() =>
                          downloadFile(getDownloadFileUrl(mySubmission.id, f.fileId), f.originalName)}>
                          下载
                        </Button>
                      </Space>
                    ))}
                  </div>
                )}
              </>
            ) : (
              <Empty description="尚未提交" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            )}
          </Spin>
        </Card>
      </Modal>

      {/* 提交 Modal */}
      <Modal title={`提交作业：${currentAssignment?.title || ''}`} open={submitModalOpen}
        onCancel={() => setSubmitModalOpen(false)} onOk={handleSubmit}
        confirmLoading={submitting} okText="提交" width={600} destroyOnClose>
        {currentAssignment?.deadline && (
          <div style={{ marginBottom: 12, color: '#666' }}>
            <ClockCircleOutlined /> 截止时间：{dayjs(currentAssignment.deadline).format('YYYY-MM-DD HH:mm')}
          </div>
        )}
        <Form form={form} layout="vertical">
          <Form.Item name="content" label="提交说明">
            <TextArea rows={4} placeholder="输入作业说明或文字内容（可选）" maxLength={2000} showCount />
          </Form.Item>
          <Form.Item label="附件">
            <Upload fileList={fileList} onChange={({ fileList: fl }) => setFileList(fl)}
              beforeUpload={() => false} multiple maxCount={5}>
              <Button icon={<UploadOutlined />}>选择文件</Button>
            </Upload>
            <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>
              支持 PDF、DOC、DOCX、ZIP、图片等格式，最多 5 个文件
            </div>
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  )
}

export default MyHomework
