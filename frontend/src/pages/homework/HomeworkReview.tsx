import React, { useEffect, useState, useCallback } from 'react'
import {
  Table, Button, Modal, Form, Input, InputNumber, Tag, Space, Descriptions, Empty, message,
  Card, Row, Col, Statistic, Segmented
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  EyeOutlined, CheckOutlined, TeamOutlined, FileTextOutlined, UserOutlined
} from '@ant-design/icons'
import PageContainer from '../../components/PageContainer'
import { listAssignments, listSubmissions, getSubmission, gradeSubmission, getDownloadFileUrl, getViewFileUrl } from '../../api/homework'
import { downloadFile, viewFile } from '../../utils/download'
import type { HomeworkAssignment, HomeworkSubmission, GradeRequest } from '../../types/homework'
import { getCohorts } from '../../api/cohort'
import type { Cohort } from '../../types/cohort'
import dayjs from 'dayjs'

const { TextArea } = Input

const HomeworkReview: React.FC = () => {
  // 作业列表
  const [assignments, setAssignments] = useState<HomeworkAssignment[]>([])
  const [loading, setLoading] = useState(false)
  // 选中的作业 - 显示提交列表
  const [selectedAssignment, setSelectedAssignment] = useState<HomeworkAssignment | null>(null)
  const [submissions, setSubmissions] = useState<HomeworkSubmission[]>([])
  const [subLoading, setSubLoading] = useState(false)
  // 批改弹窗
  const [gradeModalOpen, setGradeModalOpen] = useState(false)
  const [currentSubmission, setCurrentSubmission] = useState<HomeworkSubmission | null>(null)
  const [grading, setGrading] = useState(false)
  const [gradeForm] = Form.useForm()
  const [cohorts, setCohorts] = useState<Cohort[]>([])
  const [activeCohort, setActiveCohort] = useState<string>('all')

  const cohortId = activeCohort === 'all' ? undefined : Number(activeCohort)

  const fetchAssignments = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listAssignments({ page: 1, size: 100, cohortId })
      setAssignments(data.list)
      // 批改后实时刷新选中作业的计数（工作台统计）
      setSelectedAssignment((prev) => {
        if (!prev) return prev
        return data.list.find((a) => a.id === prev.id) ?? prev
      })
    } catch { /* handled */ } finally { setLoading(false) }
  }, [cohortId])

  useEffect(() => { fetchAssignments() }, [fetchAssignments])
  useEffect(() => { getCohorts().then(setCohorts) }, [])

  const handleSelectAssignment = async (item: HomeworkAssignment) => {
    setSelectedAssignment(item)
    setSubLoading(true)
    try {
      const data = await listSubmissions(item.id, undefined, 1, 200)
      setSubmissions(data.list)
    } catch { /* handled */ } finally { setSubLoading(false) }
  }

  const handleOpenGrade = async (sub: HomeworkSubmission) => {
    setCurrentSubmission(sub)
    gradeForm.resetFields()
    gradeForm.setFieldsValue({
      points: sub.awardedPoints ?? undefined,
      comment: sub.reviewComment || ''
    })
    setGradeModalOpen(true)
  }

  const handleViewSubmission = async (sub: HomeworkSubmission) => {
    try {
      const full = await getSubmission(sub.id)
      setCurrentSubmission(full)
      gradeForm.resetFields()
      gradeForm.setFieldsValue({
        points: full.awardedPoints ?? undefined,
        comment: full.reviewComment || ''
      })
      setGradeModalOpen(true)
    } catch { message.error('获取提交详情失败') }
  }

  const handleGrade = async () => {
    if (!currentSubmission) return
    try {
      const values = await gradeForm.validateFields()
      setGrading(true)
      const req: GradeRequest = { points: values.points, comment: values.comment || '' }
      await gradeSubmission(currentSubmission.id, req)
      message.success(currentSubmission.status === 'GRADED' ? '批改已更新' : '批改完成')
      setGradeModalOpen(false)
      // 实时刷新：提交列表（积分列）+ 作业列表计数（工作台统计）
      if (selectedAssignment) {
        const data = await listSubmissions(selectedAssignment.id, undefined, 1, 200)
        setSubmissions(data.list)
      }
      await fetchAssignments()
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'errorFields' in e) return
      message.error((e as Error)?.message || '批改失败')
    } finally { setGrading(false) }
  }

  // 作业列表列
  const assignmentColumns: ColumnsType<HomeworkAssignment> = [
    { title: '作业标题', dataIndex: 'title', key: 'title' },
    { title: '目标范围', key: 'target', width: 120,
      render: (_: unknown, r: HomeworkAssignment) =>
        r.targetType === 'ALL' ? '全体成员' : r.targetDepartment || '-' },
    { title: '截止时间', dataIndex: 'deadline', width: 160,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm') },
    { title: '提交/已批', key: 'counts', width: 100,
      render: (_: unknown, r: HomeworkAssignment) =>
        `${r.submissionCount ?? 0} / ${r.gradedCount ?? 0}` },
    { title: '状态', dataIndex: 'status', width: 90,
      render: (v: string) => {
        const map: Record<string, { color: string; text: string }> = {
          DRAFT: { color: 'default', text: '草稿' },
          PUBLISHED: { color: 'blue', text: '已发布' },
          CLOSED: { color: 'red', text: '已关闭' },
        }
        const cfg = map[v] || { color: 'default', text: v }
        return <Tag color={cfg.color}>{cfg.text}</Tag>
      }},
    { title: '操作', key: 'actions', width: 100,
      render: (_: unknown, r: HomeworkAssignment) => (
        <Button type="primary" size="small" icon={<EyeOutlined />}
          onClick={() => handleSelectAssignment(r)}>查看提交</Button>
      )},
  ]

  // 提交列表列
  const submissionColumns: ColumnsType<HomeworkSubmission> = [
    { title: '姓名', dataIndex: 'memberName', width: 100 },
    { title: '学号', dataIndex: 'memberStudentNo', width: 120 },
    { title: '部门', dataIndex: 'memberDepartment', width: 100, render: (v: string) => v || '-' },
    { title: '提交时间', dataIndex: 'submittedAt', width: 160,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm') },
    { title: '状态', dataIndex: 'status', width: 90,
      render: (v: string) => (
        <Tag color={v === 'GRADED' ? 'green' : 'blue'}>{v === 'GRADED' ? '已批改' : '已提交'}</Tag>
      )},
    { title: '积分', dataIndex: 'awardedPoints', width: 80,
      render: (v: number | undefined) => v != null ? `${v} 分` : '-' },
    { title: '批改人', dataIndex: 'reviewedByName', width: 100, render: (v: string) => v || '-' },
    { title: '操作', key: 'actions', width: 100,
      render: (_: unknown, r: HomeworkSubmission) => (
        <Button type="link" size="small" icon={<CheckOutlined />}
          onClick={() => handleOpenGrade(r)}>批改</Button>
      )},
  ]

  return (
    <PageContainer title="作业批改">
      <Segmented
        value={activeCohort}
        options={[
          { label: '全部', value: 'all' },
          ...cohorts.map((c) => ({ label: `${c.year}届`, value: String(c.id) })),
        ]}
        onChange={(v) => setActiveCohort(v as string)}
        style={{ marginBottom: 16 }}
      />

      {/* 返回按钮 */}
      {selectedAssignment ? (
        <>
          <Button type="link" onClick={() => setSelectedAssignment(null)}
            style={{ padding: 0, marginBottom: 12 }}>← 返回作业列表</Button>
          <Card size="small" style={{ marginBottom: 16 }}>
            <Row gutter={24}>
              <Col span={8}><Statistic title="作业标题" value={selectedAssignment.title} prefix={<FileTextOutlined />} /></Col>
              <Col span={4}><Statistic title="提交人数" value={selectedAssignment.submissionCount ?? 0} prefix={<TeamOutlined />} /></Col>
              <Col span={4}><Statistic title="已批改" value={selectedAssignment.gradedCount ?? 0} prefix={<CheckOutlined />} /></Col>
              <Col span={4}><Statistic title="待批改" value={(selectedAssignment.submissionCount ?? 0) - (selectedAssignment.gradedCount ?? 0)} /></Col>
            </Row>
          </Card>
          <Table columns={submissionColumns} dataSource={submissions} rowKey="id"
            loading={subLoading} pagination={false} scroll={{ x: 800 }}
            onRow={(record) => ({ onDoubleClick: () => handleViewSubmission(record) })} />
        </>
      ) : (
        <Table columns={assignmentColumns} dataSource={assignments} rowKey="id"
          loading={loading} pagination={false} scroll={{ x: 700 }} />
      )}

      {/* 批改/查看 Modal */}
      <Modal title="批改作业" open={gradeModalOpen}
        onCancel={() => setGradeModalOpen(false)} onOk={handleGrade}
        confirmLoading={grading} okText="确认批改" width={680} destroyOnClose>
        {currentSubmission && (
          <>
            <Descriptions column={3} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="姓名">{currentSubmission.memberName}</Descriptions.Item>
              <Descriptions.Item label="学号">{currentSubmission.memberStudentNo}</Descriptions.Item>
              <Descriptions.Item label="部门">{currentSubmission.memberDepartment || '-'}</Descriptions.Item>
              <Descriptions.Item label="作业">{currentSubmission.homeworkTitle}</Descriptions.Item>
              <Descriptions.Item label="提交时间">
                {dayjs(currentSubmission.submittedAt).format('YYYY-MM-DD HH:mm')}
              </Descriptions.Item>
              <Descriptions.Item label="当前状态">
                <Tag color={currentSubmission.status === 'GRADED' ? 'green' : 'blue'}>
                  {currentSubmission.status === 'GRADED' ? '已批改' : '已提交'}
                </Tag>
              </Descriptions.Item>
            </Descriptions>

            {currentSubmission.content && (
              <Card title="提交内容" size="small" style={{ marginBottom: 16 }}>
                <div style={{ whiteSpace: 'pre-wrap' }}>{currentSubmission.content}</div>
              </Card>
            )}

            {currentSubmission.files && currentSubmission.files.length > 0 && (
              <Card title="附件" size="small" style={{ marginBottom: 16 }}>
                {currentSubmission.files.map(f => (
                  <Space key={f.id} style={{ marginRight: 12, marginBottom: 4 }}>
                    <Button type="link" size="small" icon={<EyeOutlined />}
                      onClick={() => viewFile(getViewFileUrl(currentSubmission.id, f.fileId))}>
                      {f.originalName}
                    </Button>
                    <Button type="link" size="small"
                      onClick={() => downloadFile(getDownloadFileUrl(currentSubmission.id, f.fileId), f.originalName)}>
                      下载
                    </Button>
                  </Space>
                ))}
              </Card>
            )}

            <Form form={gradeForm} layout="vertical">
              <Form.Item name="comment" label="批改意见">
                <TextArea rows={3} placeholder="填写批改意见（可选）" maxLength={1000} showCount />
              </Form.Item>
              <Form.Item name="points" label="积分" rules={[{ required: true, message: '请输入积分' }]}>
                <InputNumber min={0} max={selectedAssignment?.maxPoints ?? 999} style={{ width: 200 }}
                  precision={selectedAssignment?.maxPoints != null && selectedAssignment.maxPoints % 1 !== 0 ? 1 : 0} />
                {selectedAssignment?.maxPoints != null && (
                  <span style={{ marginLeft: 8, color: '#999' }}>最大积分：{selectedAssignment.maxPoints} 分</span>
                )}
              </Form.Item>
            </Form>
          </>
        )}
      </Modal>
    </PageContainer>
  )
}

export default HomeworkReview
